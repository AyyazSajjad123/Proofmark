package com.example.proofmark.feature.verify

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

@Composable
fun VerifyScreen() {
    val ctx: Context = LocalContext.current
    var lastProof by remember { mutableStateOf<File?>(null) }
    var lastSha by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { lastProof = findLatestProofFile(ctx) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.verify_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.verify_desc), style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { Toast.makeText(ctx, "File picker not wired yet", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.semantics { contentDescription = "Pick a proof file to verify" }
        ) { Text(stringResource(R.string.verify_pick)) }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                runCatching {
                    val f = lastProof ?: error(ctx.getString(R.string.verify_empty))
                    val sha: String = sha256Of(f)
                    lastSha = sha
                    Toast.makeText(ctx, "SHA-256 computed", Toast.LENGTH_SHORT).show()
                }.onFailure { err = it.message ?: "error" }
            },
            enabled = lastProof != null,
            modifier = Modifier.semantics { contentDescription = "Verify the most recent proof (recompute hash)" }
        ) { Text(stringResource(R.string.verify_latest)) }

        Spacer(Modifier.height(20.dp))
        if (lastProof == null) {
            Text(stringResource(R.string.verify_empty))
        } else {
            Text(
                stringResource(R.string.verify_latest_label, lastProof!!.name),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
        }

        lastSha?.let { sha ->
            Text(stringResource(R.string.verify_sha), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(sha, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText("sha256", sha))
                Toast.makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.verify_copy)) }
        }

        err?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

/* helpers */
private fun findLatestProofFile(ctx: Context): File? {
    val ext = File(ctx.getExternalFilesDir(null), "proofs")
        .listFiles { f -> f.isFile && f.name.matches(Regex("\\d+\\.jpg")) }
        ?.maxByOrNull { it.lastModified() }
    val internal = File(ctx.filesDir, "proof_work")
        .listFiles { f -> f.isFile && f.name.matches(Regex("\\d+\\.jpg")) }
        ?.maxByOrNull { it.lastModified() }
    return listOfNotNull(ext, internal).maxByOrNull { it.lastModified() }
}
private fun sha256Of(file: File): String {
    val md = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { fis ->
        val buf = ByteArray(8 * 1024)
        while (true) { val n = fis.read(buf); if (n <= 0) break; md.update(buf, 0, n) }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}
