package com.example.proofmark.work

import android.content.Context
import androidx.work.*

object QueueOrchestrator {

    fun enqueue(
        wm: WorkManager,
        proofId: String,
        inputPath: String,
        maxMP: Int = 12,
        quality: String = "MED"
    ) {
        val base = workDataOf(
            Keys.PROOF_ID to proofId,
            Keys.INPUT to inputPath,
            Keys.MAX_MP to maxMP,
            Keys.QUALITY to quality
        )

        val normalize = OneTimeWorkRequestBuilder<NormalizeWorker>()
            .setInputData(base)
            .setConstraints(constraintsDefault())
            .addTag("proof")
            .addTag(workNameFor(proofId))
            .build()

        val overlay   = OneTimeWorkRequestBuilder<OverlayWorker>().build()
        val downscale = OneTimeWorkRequestBuilder<DownscaleWorker>().build()
        val compress  = OneTimeWorkRequestBuilder<CompressWorker>().build()
        val save      = OneTimeWorkRequestBuilder<SaveWorker>().build()
        val hash      = OneTimeWorkRequestBuilder<HashWorker>().build()
        val manifest  = OneTimeWorkRequestBuilder<ManifestWorker>().build()
        val mark      = OneTimeWorkRequestBuilder<MarkStatusWorker>().build()

        wm.enqueueUniqueWork(workNameFor(proofId), ExistingWorkPolicy.REPLACE, normalize)
        wm.beginUniqueWork(workNameFor(proofId), ExistingWorkPolicy.APPEND, normalize)
            .then(overlay)
            .then(downscale)
            .then(compress)
            .then(save)
            .then(hash)
            .then(manifest)
            .then(mark)
            .enqueue()
    }

    fun retryWithInput(wm: WorkManager, proofId: String, inputPath: String) =
        enqueue(wm, proofId, inputPath)

    fun drainOnAppStart(context: Context) {
        // keeps WorkManager DB tidy; safe to call on startup
        WorkManager.getInstance(context).pruneWork()
    }
}
