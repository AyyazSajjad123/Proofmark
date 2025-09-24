package com.example.proofmark.feature.library

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun WatermarkGalleryScreen() {
    val ctx = LocalContext.current

    fun listCaptured(): List<File> =
        (ctx.getExternalFilesDir(null)?.listFiles()?.toList() ?: emptyList())
            .filter { it.isFile && it.name.endsWith(".jpg", ignoreCase = true) }
            .sortedByDescending { it.lastModified() }

    var files by remember { mutableStateOf(listCaptured()) }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved photos: ${files.size}", modifier = Modifier.weight(1f))
                Button(onClick = { files = listCaptured() }) { Text("Refresh") }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files, key = { it.absolutePath }) { f ->
                    val thumb by remember(f.absolutePath) { mutableStateOf(loadThumb(f, 400)) }
                    if (thumb != null) {
                        Image(
                            bitmap = thumb!!.asImageBitmap(),
                            contentDescription = f.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) { Text("…") }
                    }
                }
            }
        }
    }
}

private fun loadThumb(file: File, reqSize: Int): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        val (w, h) = opts.outWidth to opts.outHeight
        if (w <= 0 || h <= 0) return null
        var inSample = 1
        var halfW = w / 2
        var halfH = h / 2
        while ((halfW / inSample) >= reqSize && (halfH / inSample) >= reqSize) {
            inSample *= 2
        }
        val opts2 = BitmapFactory.Options().apply { inSampleSize = inSample }
        BitmapFactory.decodeFile(file.absolutePath, opts2)
    } catch (_: Throwable) {
        null
    }
}
