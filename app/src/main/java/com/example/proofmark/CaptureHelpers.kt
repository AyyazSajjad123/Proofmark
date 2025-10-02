package com.example.proofmark.feature.capture

import android.content.Context
import androidx.work.WorkManager
import com.example.proofmark.work.QueueOrchestrator
import java.io.File

/**
 * Simple wrapper to enqueue the proof processing chain.
 * Keeps this module free from WorkManager builder details.
 */
fun enqueueProofChain(
    context: Context,
    proofId: String,
    tempJpg: File,
    maxMP: Int = 12,
    quality: String = "MED"
) {
    val wm = WorkManager.getInstance(context)
    QueueOrchestrator.enqueue(
        wm = wm,
        proofId = proofId,
        inputPath = tempJpg.absolutePath,
        maxMP = maxMP,
        quality = quality
    )
}
