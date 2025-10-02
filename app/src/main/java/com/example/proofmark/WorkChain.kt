package com.example.proofmark.work

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.system.measureTimeMillis

/** Canonical + legacy-compat keys (do NOT rename) */
object Keys {

    const val PROOF_ID = "proof_id"

    // canonical
    const val INPUT  = "input"
    const val OUTPUT = "output"
    const val OVERLAY_MS  = "overlay_ms"
    const val COMPRESS_MS = "compress_ms"
    const val HASH_MS     = "hash_ms"
    const val TOTAL_MS    = "chain_total_ms"
    const val SHA256      = "sha256"
    const val QUALITY     = "quality"
    const val MAX_MP      = "max_mp"

    // legacy aliases (stop unresolved refs)
    const val INPUT_PATH   = INPUT
    const val OUTPUT_PATH  = OUTPUT
    const val OUTPUT_HASH  = SHA256
    const val CHAIN_MS     = TOTAL_MS
    const val MIRROR_IF_FRONT = "mirror_if_front"
    const val LAT = "lat"
    const val LON = "lon"
    const val ACC_M = "acc_m"
}

/** Small base to keep all workers on Dispatchers.IO & safe-run */
abstract class BaseWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    final override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching { runSafely() }.getOrElse { Result.retry() }
    }
    abstract suspend fun runSafely(): Result
}

/** 1) Normalize: copy to internal work dir, ensure EXIF exists */
class NormalizeWorker(ctx: Context, p: WorkerParameters) : BaseWorker(ctx, p) {
    override suspend fun runSafely(): Result {
        val id = inputData.getString(Keys.PROOF_ID) ?: return Result.failure()
        val inPath = inputData.getString(Keys.INPUT) ?: return Result.failure()
        val src = File(inPath); if (!src.exists()) return Result.failure()

        val workDir = File(applicationContext.filesDir, "proof_work").apply { mkdirs() }
        val out = File(workDir, "$id.jpg")
        src.copyTo(out, overwrite = true)
        runCatching { ExifInterface(out.absolutePath).saveAttributes() }

        return Result.success(workDataOf(
            Keys.PROOF_ID to id,
            Keys.INPUT to out.absolutePath,
            Keys.OUTPUT to out.absolutePath
        ))
    }
}

/** 2) Overlay: (stub) – pass-through + timing */
class OverlayWorker(ctx: Context, p: WorkerParameters) : BaseWorker(ctx, p) {
    override suspend fun runSafely(): Result {
        val id = inputData.getString(Keys.PROOF_ID) ?: return Result.failure()
        val inPath = inputData.getString(Keys.INPUT) ?: return Result.failure()
        val overlayMs = measureTimeMillis { /* TODO real drawing later */ }
        return Result.success(workDataOf(
            Keys.PROOF_ID to id,
            Keys.INPUT to inPath,
            Keys.OUTPUT to inPath,
            Keys.OVERLAY_MS to overlayMs
        ))
    }
}

/** 3) Downscale: (stub) – keep original size for stability */
class DownscaleWorker(ctx: Context, p: WorkerParameters) : BaseWorker(ctx, p) {
    override suspend fun runSafely(): Result {
        val id = inputData.getString(Keys.PROOF_ID) ?: return Result.failure()
        val inPath = inputData.getString(Keys.INPUT) ?: return Result.failure()
        return Result.success(workDataOf(
            Keys.PROOF_ID to id,
            Keys.INPUT to inPath,
            Keys.OUTPUT to inPath
        ))
    }
}
// WorkChain.kt (inside class CompressWorker)
class CompressWorker(ctx: Context, p: WorkerParameters) : BaseWorker(ctx, p) {
    override suspend fun runSafely(): Result {
        val rawId = inputData.getString(Keys.PROOF_ID) ?: return Result.failure()
        val inPath = inputData.getString(Keys.INPUT) ?: return Result.failure()
        val src = File(inPath)
        if (!src.exists()) return Result.failure()

        // Normalize: accept "cap_1758..." or "1758..." -> keep only digits
        val digits = Regex("(\\d{6,})").find(rawId)?.groupValues?.get(1) ?: rawId.filter { it.isDigit() }

        // Final output: /Android/data/<pkg>/files/proofs/{digits}.jpg
        val outDir = File(applicationContext.getExternalFilesDir(null), "proofs").apply { mkdirs() }
        val out = File(outDir, "$digits.jpg")

        // Copy/overwrite
        src.copyTo(out, overwrite = true)

        // IMPORTANT: return OUTPUT so Library/Verify read the same path
        return Result.success(
            workDataOf(
                Keys.PROOF_ID to rawId,
                Keys.INPUT to out.absolutePath,
                Keys.OUTPUT to out.absolutePath
            )
        )
    }
}

/** 5) Save: just propagate OUTPUT forward (DB update handled elsewhere) */
class SaveWorker(ctx: Context, p: WorkerParameters) : BaseWorker(ctx, p) {
    override suspend fun runSafely(): Result {
        val id = inputData.getString(Keys.PROOF_ID) ?: return Result.failure()
        val out = inputData.getString(Keys.OUTPUT) ?: return Result.failure()
        return Result.success(workDataOf(
            Keys.PROOF_ID to id,
            Keys.INPUT to out,
            Keys.OUTPUT to out
        ))
    }
}

/** 6) Hash: compute sha256 of final JPG */
class HashWorker(ctx: Context, p: WorkerParameters) : BaseWorker(ctx, p) {
    override suspend fun runSafely(): Result {
        val id = inputData.getString(Keys.PROOF_ID) ?: return Result.failure()
        val out = inputData.getString(Keys.OUTPUT) ?: return Result.failure()
        val file = File(out); if (!file.exists()) return Result.failure()

        val md = MessageDigest.getInstance("SHA-256")
        val hashMs = measureTimeMillis {
            FileInputStream(file).use { fis ->
                val buf = ByteArray(8 * 1024)
                while (true) {
                    val n = fis.read(buf); if (n <= 0) break
                    md.update(buf, 0, n)
                }
            }
        }
        val sha = md.digest().joinToString("") { "%02x".format(it) }

        return Result.success(workDataOf(
            Keys.PROOF_ID to id,
            Keys.OUTPUT to out,
            Keys.SHA256 to sha,
            Keys.HASH_MS to hashMs
        ))
    }
}

// WorkChain.kt (inside class ManifestWorker)
class ManifestWorker(ctx: Context, p: WorkerParameters) : BaseWorker(ctx, p) {
    override suspend fun runSafely(): Result {
        val rawId = inputData.getString(Keys.PROOF_ID) ?: return Result.failure()
        val out = inputData.getString(Keys.OUTPUT) ?: return Result.failure()
        val jpg = File(out)
        if (!jpg.exists()) return Result.failure()

        // Normalize id to digits (same rule as above)
        val digits = Regex("(\\d{6,})").find(rawId)?.groupValues?.get(1) ?: rawId.filter { it.isDigit() }

        val sha = inputData.getString(Keys.SHA256) ?: ""
        val quality = inputData.getString(Keys.QUALITY)
        val maxMp = inputData.getInt(Keys.MAX_MP, -1).takeIf { it > 0 }

        val json = buildString {
            append("{")
            append("\"proofId\":\"").append(digits).append("\",")
            append("\"outputPath\":\"").append(jpg.absolutePath.replace("\\", "\\\\")).append("\",")
            append("\"bytes\":").append(jpg.length())
            if (sha.isNotEmpty()) append(",\"sha256\":\"").append(sha).append("\"")
            if (!quality.isNullOrEmpty()) append(",\"quality\":\"").append(quality).append("\"")
            if (maxMp != null) append(",\"maxMp\":").append(maxMp)
            append("}")
        }

        // JSON name must be proof_{digits}.json (NOT proof_cap_*.json)
        File(jpg.parentFile, "proof_${digits}.json").writeText(json)

        return Result.success(workDataOf(
            Keys.PROOF_ID to rawId,
            Keys.OUTPUT to out,
            Keys.SHA256 to sha
        ))
    }
}


/** 8) MarkStatus: terminal no-op (keeps OUTPUT) */
class MarkStatusWorker(ctx: Context, p: WorkerParameters) : BaseWorker(ctx, p) {
    override suspend fun runSafely(): Result {
        val id = inputData.getString(Keys.PROOF_ID) ?: return Result.failure()
        val out = inputData.getString(Keys.OUTPUT) ?: return Result.failure()
        return Result.success(workDataOf(Keys.PROOF_ID to id, Keys.OUTPUT to out))
    }
}

/** Common constraints + unique name helper */
fun constraintsDefault(): Constraints = Constraints.Builder()
    .setRequiresBatteryNotLow(true)
    .setRequiresStorageNotLow(true)
    .build()

fun workNameFor(id: String) = "proof:$id"
