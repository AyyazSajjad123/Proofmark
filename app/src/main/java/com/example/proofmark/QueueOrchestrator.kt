package com.example.proofmark.work

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.proofmark.work.workers.*
import java.util.concurrent.TimeUnit

object QueueOrchestrator {

    fun enqueue(
        workManager: WorkManager,
        proofId: String,
        inputPath: String,
        maxMP: Int = 12,
        quality: String = "MED"
    ) {
        // use Data.Builder (avoids Pair<String, Any?> inference issues)
        val base: Data = Data.Builder()
            .putString(Keys.PROOF_ID, proofId)
            .putString(Keys.INPUT_PATH, inputPath)
            .build()

        val normalize = OneTimeWorkRequestBuilder<NormalizeWorker>()
            .setInputData(base)
            .build()

        val overlay = OneTimeWorkRequestBuilder<OverlayWorker>().build()

        val downscaleInput = Data.Builder()
            .putInt("maxMP", maxMP)
            .build()
        val downscale = OneTimeWorkRequestBuilder<DownscaleWorker>()
            .setInputData(downscaleInput)
            .build()

        val compressInput = Data.Builder()
            .putString("quality", quality)
            .build()
        val compress = OneTimeWorkRequestBuilder<CompressWorker>()
            .setInputData(compressInput)
            .build()

        val save = OneTimeWorkRequestBuilder<SaveWorker>().build()
        val hash = OneTimeWorkRequestBuilder<HashWorker>().build()
        val manifest = OneTimeWorkRequestBuilder<ManifestWorker>().build()

        val mark = OneTimeWorkRequestBuilder<MarkStatusWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        workManager
            .beginUniqueWork("proof-$proofId", ExistingWorkPolicy.REPLACE, normalize)
            .then(overlay)
            .then(downscale)
            .then(compress)
            .then(save)
            .then(hash)
            .then(manifest)
            .then(mark)
            .enqueue()
    }
}
