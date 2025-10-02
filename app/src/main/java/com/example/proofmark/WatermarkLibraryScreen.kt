package com.example.proofmark.feature.library

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun WatermarkLibraryScreen() {
    val ctx = LocalContext.current
    val wm = remember(ctx) { WorkManager.getInstance(ctx) }

    val workInfosFlow = remember {
        callbackFlow<List<WorkInfo>> {
            val live = wm.getWorkInfosByTagLiveData("proof")
            val obs = androidx.lifecycle.Observer<List<WorkInfo>> { trySend(it ?: emptyList()) }
            live.observeForever(obs)
            awaitClose { live.removeObserver(obs) }
        }.onStart { emit(emptyList()) }
    }
    val infos by workInfosFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Watermark Library", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(infos, key = { it.id }) { wi ->
                WatermarkLibraryRow(
                    wi = wi,
                    onRetry = { proofId ->
                        val input = guessInputPath(ctx.getExternalFilesDir(null), proofId)
                        if (input != null && input.exists()) {
                            // 👇 Fully-qualified reference — no import required
                            com.example.proofmark.work.QueueOrchestrator.enqueue(
                                wm,
                                proofId = proofId,
                                inputPath = input.absolutePath,
                                maxMP = 12,
                                quality = "MED"
                            )
                            Toast.makeText(ctx, "Re-queued: $proofId", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, "Input not found for $proofId", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Divider()
            }
        }
    }
}

private fun guessInputPath(dir: File?, proofId: String): File? {
    if (dir == null) return null
    return File(dir, "$proofId.jpg")
}

@Composable
private fun WatermarkLibraryRow(
    wi: WorkInfo,
    onRetry: (proofId: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val proofId = wi.tags.firstOrNull { it.startsWith("proof:") }?.removePrefix("proof:") ?: "?"

    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("Proof: $proofId")
            Text("Status: ${wi.state}")
        }
        if (wi.state == WorkInfo.State.FAILED || wi.state == WorkInfo.State.CANCELLED) {
            Button(onClick = { scope.launch { onRetry(proofId) } }) {
                Text("Retry")
            }
        }
    }
}
