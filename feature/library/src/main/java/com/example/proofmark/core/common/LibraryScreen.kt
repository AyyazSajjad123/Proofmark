package com.example.proofmark.feature.library

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.proofmark.feature.library.R   // <-- IMPORTANT: feature/library ka R
import java.io.File

@Composable
fun LibraryScreen() {
    val ctx = LocalContext.current
    var proofId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("—") }
    var file by remember { mutableStateOf<File?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    // Share JPG warning dialog
    var warnDialog by remember { mutableStateOf(false) }
    var pendingShare by remember { mutableStateOf<File?>(null) }

    // Simple flag (RC wiring optional). Keep it true for now.
    val warnShareEnabled = true

    LaunchedEffect(Unit) {
        val f = findLatestProofFile(ctx)
        if (f == null) {
            status = "No items"
        } else {
            proofId = f.nameWithoutExtension
            status = "SUCCEEDED"
            file = f
        }
    }

    if (warnDialog) {
        AlertDialog(
            onDismissRequest = { warnDialog = false; pendingShare = null },
            title = { Text(stringResource(R.string.share_warn_title)) },
            text  = { Text(stringResource(R.string.share_warn_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val f = pendingShare
                    warnDialog = false
                    pendingShare = null
                    if (f != null) shareGeneric(ctx, f, "image/jpeg")
                }) { Text(stringResource(R.string.share_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { warnDialog = false; pendingShare = null }) {
                    Text(stringResource(R.string.share_cancel))
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))

        if (proofId == null) {
            Text("No proofs found. Capture first.")
            return@Column
        }

        Text("Proof:")
        Text(proofId!!, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("Status: $status")
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = file != null,
                onClick = {
                    runCatching { openWithViewer(ctx, file!!, "image/jpeg") }
                        .onFailure { err = "Open failed: ${it.message}" }
                },
                modifier = Modifier.semantics { contentDescription = "Open proof image" }
            ) { Text("Open") }

            OutlinedButton(
                enabled = file != null,
                onClick = {
                    val f = file ?: return@OutlinedButton
                    if (warnShareEnabled) {
                        pendingShare = f
                        warnDialog = true
                    } else {
                        runCatching { shareGeneric(ctx, f, "image/jpeg") }
                            .onFailure { err = "Share failed: ${it.message}" }
                    }
                },
                modifier = Modifier.semantics { contentDescription = "Share proof image" }
            ) { Text("Share") }
        }

        Spacer(Modifier.height(12.dp))
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Divider(Modifier.padding(top = 16.dp))
    }
}

private fun findLatestProofFile(ctx: Context): File? {
    val ext = File(ctx.getExternalFilesDir(null), "proofs")
        .listFiles { f -> f.isFile && f.name.matches(Regex("\\d+\\.jpg")) }
        ?.maxByOrNull { it.lastModified() }

    val internal = File(ctx.filesDir, "proof_work")
        .listFiles { f -> f.isFile && f.name.matches(Regex("\\d+\\.jpg")) }
        ?.maxByOrNull { it.lastModified() }

    return listOfNotNull(ext, internal).maxByOrNull { it.lastModified() }
}

private fun openWithViewer(ctx: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val it = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(it, "Open with"))
}

private fun shareGeneric(ctx: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val it = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(it, "Share"))
}
