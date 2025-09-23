package com.example.proofmark.work.workers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.proofmark.core.common.SimpleExifNormalizer
import com.example.proofmark.core.common.WatermarkPreset
import com.example.proofmark.core.common.WatermarkUtil
import com.example.proofmark.core.crypto.Sha256Computer
import com.example.proofmark.work.Keys
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

/* -------- Data helper: avoids workDataOf inference issues -------- */
private fun dataOf(vararg put: Pair<String, Any?>): Data {
    val b = Data.Builder()
    for ((k, v) in put) when (v) {
        null -> b.putString(k, null)
        is String -> b.putString(k, v)
        is Int -> b.putInt(k, v)
        is Long -> b.putLong(k, v)
        is Boolean -> b.putBoolean(k, v)
        is Double -> b.putDouble(k, v)
        else -> b.putString(k, v.toString())
    }
    return b.build()
}

/* -------- Task await helper -------- */
private suspend fun <T> Task<T>.awaitOrNull(): T? = try {
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { r -> if (!cont.isCompleted) cont.resume(r) }
        addOnFailureListener { e -> if (!cont.isCompleted) cont.resumeWithException(e) }
        addOnCanceledListener { if (!cont.isCompleted) cont.resume(null as T?) }
    }
} catch (_: Throwable) { null }

/* ================= Normalize ================= */
@HiltWorker
class NormalizeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val input = inputData.getString(Keys.INPUT_PATH) ?: return Result.failure()
            val norm = SimpleExifNormalizer().normalizeAndUnmirror(input)
            val pid = inputData.getString(Keys.PROOF_ID) ?: UUID.randomUUID().toString()
            Result.success(dataOf(Keys.OUTPUT_PATH to norm, Keys.PROOF_ID to pid))
        } catch (t: Throwable) {
            Log.e("NormalizeWorker", "err", t); Result.retry()
        }
    }
}

/* ================= Overlay (Watermark) ================= */
@HiltWorker
class OverlayWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @SuppressLint("MissingPermission") // permission may be denied; caught below
    private suspend fun lastLocationOrNull(): Location? {
        return try {
            val fused = LocationServices.getFusedLocationProviderClient(appContext)

            // Try cached last location
            val last = fused.lastLocation.awaitOrNull()
            if (last != null && (System.currentTimeMillis() - last.time) < 120_000) return last

            // Try quick current location
            val cts = CancellationTokenSource()
            fused.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            ).awaitOrNull()
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val input = inputData.getString(Keys.OUTPUT_PATH) ?: return Result.failure()
            val proofId = inputData.getString(Keys.PROOF_ID) ?: UUID.randomUUID().toString()
            val loc = lastLocationOrNull()
            val out = WatermarkUtil.drawWatermark(appContext, input, proofId, loc, WatermarkPreset.Dark)
            Result.success(dataOf(Keys.OUTPUT_PATH to out.outputPath, Keys.PROOF_ID to proofId))
        } catch (t: Throwable) {
            Log.e("OverlayWorker", "err", t); Result.retry()
        }
    }
}

/* ================= Downscale (max MP) ================= */
@HiltWorker
class DownscaleWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val input = inputData.getString(Keys.OUTPUT_PATH) ?: return Result.failure()
            val maxMp = (inputData.getInt("maxMP", 12)).coerceIn(4, 48)
            val src = android.graphics.BitmapFactory.decodeFile(input) ?: return Result.failure()

            val mp = src.width.toLong() * src.height.toLong() / 1_000_000.0
            val scale = if (mp > maxMp) sqrt(maxMp / mp).toFloat() else 1f
            val outBmp = if (scale < 0.999f)
                android.graphics.Bitmap.createScaledBitmap(
                    src, (src.width * scale).toInt(), (src.height * scale).toInt(), true
                ) else src

            val outFile = File(File(input).parentFile, File(input).nameWithoutExtension + "_ds.jpg")
            outFile.outputStream().use { os ->
                outBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, os)
            }
            if (outBmp !== src) src.recycle()
            outBmp.recycle()

            Result.success(dataOf(Keys.OUTPUT_PATH to outFile.absolutePath, Keys.PROOF_ID to inputData.getString(Keys.PROOF_ID)))
        } catch (t: Throwable) {
            Log.e("DownscaleWorker", "err", t); Result.retry()
        }
    }
}

/* ================= Compress (MED/HIGH) ================= */
@HiltWorker
class CompressWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val input = inputData.getString(Keys.OUTPUT_PATH) ?: return Result.failure()
            val quality = when ((inputData.getString("quality") ?: "MED").uppercase(Locale.US)) {
                "HIGH" -> 92
                else -> 86
            }
            val bmp = android.graphics.BitmapFactory.decodeFile(input) ?: return Result.failure()
            val outFile = File(File(input).parentFile, File(input).nameWithoutExtension + "_cmp.jpg")
            outFile.outputStream().use { os ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, os)
            }
            bmp.recycle()
            Result.success(dataOf(Keys.OUTPUT_PATH to outFile.absolutePath, Keys.PROOF_ID to inputData.getString(Keys.PROOF_ID)))
        } catch (t: Throwable) {
            Log.e("CompressWorker", "err", t); Result.retry()
        }
    }
}

/* ================= Save (final folder) ================= */
@HiltWorker
class SaveWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val input = inputData.getString(Keys.OUTPUT_PATH) ?: return Result.failure()
            val proofId = inputData.getString(Keys.PROOF_ID) ?: UUID.randomUUID().toString()
            val dir = File(appContext.getExternalFilesDir(null), "proofs/$proofId").apply { mkdirs() }
            val finalPhoto = File(dir, "photo.jpg")
            File(input).copyTo(finalPhoto, overwrite = true)
            Result.success(dataOf(Keys.OUTPUT_PATH to finalPhoto.absolutePath, Keys.PROOF_ID to proofId))
        } catch (t: Throwable) {
            Log.e("SaveWorker", "err", t); Result.retry()
        }
    }
}

/* ================= Hash ================= */
@HiltWorker
class HashWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val input = inputData.getString(Keys.OUTPUT_PATH) ?: return Result.failure()
            val hex = Sha256Computer().sha256Hex(File(input))
            Result.success(dataOf(Keys.OUTPUT_PATH to input, Keys.SHA256 to hex, Keys.PROOF_ID to inputData.getString(Keys.PROOF_ID)))
        } catch (t: Throwable) {
            Log.e("HashWorker", "err", t); Result.retry()
        }
    }
}

/* ================= Manifest (manifest.json) ================= */
@HiltWorker
class ManifestWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val photoPath = inputData.getString(Keys.OUTPUT_PATH) ?: return Result.failure()
            val proofId = inputData.getString(Keys.PROOF_ID) ?: UUID.randomUUID().toString()
            val sha = inputData.getString(Keys.SHA256) ?: ""
            val file = File(photoPath)
            val dir = file.parentFile!!
            val manifest = JSONObject().apply {
                put("proofId", proofId)
                put("createdUtc", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date()))
                put("files", JSONArray().put(JSONObject().apply {
                    put("name", "photo.jpg")
                    put("path", "photo.jpg")
                    put("bytes", file.length())
                    put("sha256", sha)
                }))
            }
            val out = File(dir, "manifest.json")
            out.writeText(manifest.toString(2))
            Result.success(dataOf(Keys.OUTPUT_PATH to photoPath, Keys.PROOF_ID to proofId))
        } catch (t: Throwable) {
            Log.e("ManifestWorker", "err", t); Result.retry()
        }
    }
}

/* ================= Mark Status (placeholder) ================= */
@HiltWorker
class MarkStatusWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = Result.success()
}
