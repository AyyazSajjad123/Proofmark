package com.example.proofmark.feature.report

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.proofmark.core.pdf.PdfReport
import com.example.proofmark.core.pdf.PdfZip
import com.example.proofmark.feature.report.R  // <-- IMPORTANT: feature/report ka R
import java.io.File

@Composable
fun ReportScreen() {
    val ctx = LocalContext.current

    var latestImage by remember { mutableStateOf<File?>(null) }
    var latestManifest by remember { mutableStateOf<File?>(null) }
    var generatedPdf by remember { mutableStateOf<File?>(null) }
    var builtZip by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        val proofsDir = File(ctx.getExternalFilesDir(null), "proofs")
        val reportsDir = File(ctx.getExternalFilesDir(null), "reports")

        latestImage = proofsDir.listFiles { f ->
            f.isFile && f.name.matches(Regex("\\d+\\.jpg"))
        }?.maxByOrNull { it.lastModified() }

        latestManifest = proofsDir.listFiles { f ->
            f.isFile && f.name.startsWith("proof_") && f.name.endsWith(".json")
        }?.maxByOrNull { it.lastModified() }

        // Optional: pick already existing generated files
        generatedPdf = reportsDir.listFiles { f -> f.isFile && f.extension.equals("pdf", true) }
            ?.maxByOrNull { it.lastModified() }
        builtZip = reportsDir.listFiles { f -> f.isFile && f.extension.equals("zip", true) }
            ?.maxByOrNull { it.lastModified() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.report_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.report_desc), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                runCatching {
                    val img = latestImage ?: error(ctx.getString(R.string.report_no_proof))
                    val proofId = img.nameWithoutExtension
                    val reportId = "report_${proofId}"

                    val manifest = runCatching {
                        latestManifest?.let { PdfZip.readManifest(it) }
                    }.getOrNull()

                    val preset = PdfReport.SizePreset.MED
                    val pdf = PdfReport.buildA4Report(ctx, reportId, proofId, img, manifest, preset)
                    generatedPdf = pdf
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.report_pdf_generated, pdf.name),
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure {
                    Toast.makeText(ctx, it.message ?: "PDF failed", Toast.LENGTH_SHORT).show()
                }
            }) { Text(stringResource(R.string.report_generate_pdf)) }

            OutlinedButton(
                enabled = generatedPdf != null,
                onClick = {
                    generatedPdf?.let { shareFile(ctx, it, "application/pdf") }
                }
            ) { Text(stringResource(R.string.report_share_pdf)) }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                enabled = latestImage != null && latestManifest != null,
                onClick = {
                    runCatching {
                        val img = latestImage ?: error(ctx.getString(R.string.report_no_proof))
                        val manifest = latestManifest ?: error(ctx.getString(R.string.report_no_manifest))
                        val proofId = img.nameWithoutExtension
                        val zip = PdfZip.buildZip(ctx, proofId, img, manifest)
                        builtZip = zip
                    }.onFailure {
                        Toast.makeText(ctx, it.message ?: "ZIP failed", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text(stringResource(R.string.report_build_zip)) }

            OutlinedButton(
                enabled = builtZip != null,
                onClick = { builtZip?.let { shareFile(ctx, it, "application/zip") } }
            ) { Text(stringResource(R.string.report_share_zip)) }
        }

        Spacer(Modifier.height(20.dp))

        latestImage?.let {
            Text(stringResource(R.string.report_latest_image, it.name), style = MaterialTheme.typography.titleSmall)
        }
        latestManifest?.let {
            Text(stringResource(R.string.report_manifest, it.name), style = MaterialTheme.typography.titleSmall)
        }
        generatedPdf?.let {
            Text(stringResource(R.string.report_pdf, it.name), style = MaterialTheme.typography.bodySmall)
        }
        builtZip?.let {
            Text(stringResource(R.string.report_zip, it.name), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun shareFile(ctx: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val it = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(it, "Share"))
}
