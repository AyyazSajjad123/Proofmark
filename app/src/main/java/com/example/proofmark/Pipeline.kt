package com.example.proofmark.work

import android.content.Context
import androidx.work.*
import com.example.proofmark.work.workers.*
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Central place for all WorkManager keys used across the pipeline.
 * Keeping here avoids "unresolved Keys" issues everywhere.
 */
object Keys {
    const val INPUT_PATH  = "input_path"
    const val OUTPUT_PATH = "output_path"
    const val PROOF_ID    = "proof_id"
    const val SHA256      = "sha256"
    const val MAX_MP      = "max_mp"
    const val QUALITY     = "quality"     // "MED" | "HIGH"
}

/**
 * A tiny orchestrator: builds & enqueues the Day-5 chain.
 * Normalize → Watermark → Downscale → Compress → Save → Hash → Manifest → MarkStatus
 */
object Pipeline {

    fun enqueue(
        appContext: Context,
        proofId: String = UUID.randomUUID().toString(),
        inputPath: String,
        maxMP: Int = 12,
        quality: String = "MED"
    ) {
        val wm = WorkManager.getInstance(appContext)

        val base = Data.Builder()
            .putString(Keys.PROOF_ID, proofId)
            .putString(Keys.INPUT_PATH, inputPath)
            .putInt(Keys.MAX_MP, maxMP)
            .putString(Keys.QUALITY, quality)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresStorageNotLow(false)
            .build()

        val normalize = OneTimeWorkRequestBuilder<NormalizeWorker>()
            .setInputData(base)
            .setConstraints(constraints)
            .build()

        val overlay = OneTimeWorkRequestBuilder<OverlayWorker>()
            .setConstraints(constraints)
            .build()

        val downscale = OneTimeWorkRequestBuilder<DownscaleWorker>()
            .setConstraints(constraints)
            .build()

        val compress = OneTimeWorkRequestBuilder<CompressWorker>()
            .setConstraints(constraints)
            .build()

        val save = OneTimeWorkRequestBuilder<SaveWorker>()
            .setConstraints(constraints)
            .build()

        val hash = OneTimeWorkRequestBuilder<HashWorker>()
            .setConstraints(constraints)
            .build()

        val manifest = OneTimeWorkRequestBuilder<ManifestWorker>()
            .setConstraints(constraints)
            .build()

        val markStatus = OneTimeWorkRequestBuilder<MarkStatusWorker>()
            .setConstraints(constraints)
            .build()

        wm.beginUniqueWork(
            /* uniqueName = */ "proof_$proofId",
            ExistingWorkPolicy.REPLACE,
            normalize
        ).then(overlay)
            .then(downscale)
            .then(compress)
            .then(save)
            .then(hash)
            .then(manifest)
            .then(markStatus)
            .enqueue()
    }
}
