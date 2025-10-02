package com.example.proofmark.core.pdf

import android.content.*
import java.io.File
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PdfZip {
    data class Manifest(
        val proofId: String?,
        val outputPath: String?,
        val bytes: Long?,
        val sha256: String?,
        val quality: String?,
        val maxMp: Int?
    )

    private fun String.nullIfBlank(): String? =
        if (this.isBlank()) null else this

    /** Read tiny JSON manifest written next to the JPG. */
    fun readManifest(file: File): Manifest? = runCatching {
        val txt = file.readText()
        // very small, naive parser good enough for our fields
        fun grab(k: String) = "\"$k\"\\s*:\\s*\"([^\"]*)\"".toRegex().find(txt)?.groupValues?.get(1)
        fun grabLong(k: String) = "\"$k\"\\s*:\\s*(\\d+)".toRegex().find(txt)?.groupValues?.get(1)?.toLong()
        Manifest(
            proofId    = grab("proofId"),
            outputPath = grab("outputPath"),
            bytes      = grabLong("bytes"),
            sha256     = grab("sha256"),
            quality    = grab("quality"),
            maxMp      = grabLong("maxMp")?.toInt()
        )
    }.getOrNull()

    /**
     * ZIP pack: { <proofId>.jpg, proof_<proofId>.json }
     * @return the created ZIP file
     */
    fun buildZip(
        context: Context,
        proofId: String,
        imageFile: File,
        manifestFile: File
    ): File {
        val outDir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val zipName = "proof_${proofId}.zip"           // non-null name
        val zipFile = File(outDir, zipName)

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            // JPG entry
            val imgEntryName = imageFile.name.ifBlank { "$proofId.jpg" }
            zos.putNextEntry(ZipEntry(imgEntryName))
            FileInputStream(imageFile).use { it.copyTo(zos) }
            zos.closeEntry()

            // Manifest entry
            val manEntryName = manifestFile.name.ifBlank { "proof_${proofId}.json" }
            zos.putNextEntry(ZipEntry(manEntryName))
            FileInputStream(manifestFile).use { it.copyTo(zos) }
            zos.closeEntry()
        }
        return zipFile
    }

    /**
     * Share any file through FileProvider.
     * @param mime e.g. "application/pdf", "image/jpeg", "application/zip"
     */
    fun shareFile(ctx: Context, file: File, mime: String) {
        val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Open with")
        // grant to all potential receivers
        val res = ctx.packageManager.queryIntentActivities(chooser, 0)
        for (ri in res) {
            ctx.grantUriPermission(ri.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(chooser)
    }
}
