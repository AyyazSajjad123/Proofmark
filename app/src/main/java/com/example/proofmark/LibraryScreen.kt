package com.example.proofmark.feature.library

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun LibraryScreen() {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(emptyList<File>()) }

    LaunchedEffect(Unit) {
        val dir = ctx.getExternalFilesDir(null)
        val list = dir?.listFiles()
            ?.filter { it.isFile && it.extension.equals("jpg", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        items = list
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(items) { f ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // decode a tiny preview
                    val bmp = remember(f.absolutePath) {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                        BitmapFactory.decodeFile(f.absolutePath, opts)
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                        )
                    } else {
                        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            Text("N/A")
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        val isWm = f.name.startsWith("wm_")
                        Text(text = if (isWm) "Watermarked" else "Original")
                        Text(text = f.name)
                    }
                }
                Divider()
            }
        }
    }
}
