package com.example.proofmark.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileShare {

    private fun uri(context: Context, file: File): Uri {
        val auth = "${context.packageName}.provider"
        return FileProvider.getUriForFile(context, auth, file)
    }

    fun openImage(context: Context, file: File) {
        val u = uri(context, file)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(u, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(i, "Open image")
        context.startActivity(chooser)
    }

    fun openPdf(context: Context, file: File) {
        val u = uri(context, file)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(u, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(i, "Open PDF"))
    }

    fun shareJpg(context: Context, file: File) {
        val u = uri(context, file)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, u)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(i, "Share JPG"))
    }

    fun shareZip(context: Context, file: File) {
        val u = uri(context, file)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, u)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(i, "Share ZIP"))
    }
}
