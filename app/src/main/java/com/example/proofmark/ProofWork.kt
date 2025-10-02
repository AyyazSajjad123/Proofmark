package com.example.proofmark.work

import android.graphics.*
import android.provider.Settings
import androidx.exifinterface.media.ExifInterface
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ProofWork(
    ctx: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.Default) {
        val proofId = inputData.getString(Keys.PROOF_ID) ?: return@withContext Result.retry()
        val inPath  = inputData.getString(Keys.INPUT_PATH) ?: return@withContext Result.retry()
        val maxMp   = inputData.getInt(Keys.MAX_MP, 12)
        val quality = inputData.getString(Keys.QUALITY) ?: "MED"
        val mirrorIfFront = inputData.getBoolean(Keys.MIRROR_IF_FRONT, false)

        // ✅ Safe optional reads (no hasKeyWithValueOfType<T>())
        val lat: Double? = if (inputData.keyValueMap.containsKey(Keys.LAT)) inputData.getDouble(Keys.LAT, 0.0) else null
        val lon: Double? = if (inputData.keyValueMap.containsKey(Keys.LON)) inputData.getDouble(Keys.LON, 0.0) else null
        val accM: Float? = if (inputData.keyValueMap.containsKey(Keys.ACC_M)) inputData.getFloat(Keys.ACC_M, 0f) else null

        val startMs = System.currentTimeMillis()

        try {
            // 1) Load + EXIF orientation
            val inputFile = File(inPath)
            val src0 = BitmapFactory.decodeFile(inputFile.absolutePath) ?: return@withContext Result.retry()
            val exif = ExifInterface(inPath)
            val rotated = rotateFromExif(src0, exifRotation(exif))

            // Optional front-camera mirror fix
            val maybeUnmirrored = if (mirrorIfFront) {
                val m = Matrix().apply { preScale(-1f, 1f) }
                Bitmap.createBitmap(rotated, 0, 0, rotated.width, rotated.height, m, true).also {
                    if (it != rotated && !rotated.isRecycled) rotated.recycle()
                }
            } else rotated

            // 2) Build metadata text
            val utc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
            val local = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date())

            val gpsState = when {
                lat != null && lon != null && accM != null -> "FRESH"
                lat != null && lon != null -> "STALE"
                else -> "NONE"
            }
            val deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
            val deviceShort = (deviceId ?: "device").takeLast(6)
            val proofShort = proofId.takeLast(6)

            val locStr = if (lat != null && lon != null) {
                val accStr = if (accM != null) " ±${accM.roundToInt()}m" else ""
                String.format(Locale.US, "lat %.6f, lon %.6f%s", lat, lon, accStr)
            } else "lat/lon: —"

            val metaLines = listOf(
                "UTC: $utc",
                "Local: $local",
                "GPS: $locStr ($gpsState)",
                "Device: $deviceShort  Proof: $proofShort"
            )

            // 3) Overlay metadata
            val overlayStart = System.currentTimeMillis()
            val watermarked = drawMultiLineWatermark(maybeUnmirrored, metaLines)
            val overlayMs = System.currentTimeMillis() - overlayStart

            // 4) Downscale to MP cap
            val capped = downscaleToMegapixels(watermarked, maxMp)

            // 5) Compress
            val jpegQ = when (quality.uppercase()) {
                "LOW" -> 70
                "HIGH" -> 95
                else   -> 85
            }
            val outDir = File(applicationContext.getExternalFilesDir(null), "proof_out").apply { mkdirs() }
            val outFile = File(outDir, "proof_${proofId}.jpg")

            val compressStart = System.currentTimeMillis()
            FileOutputStream(outFile).use { fos ->
                capped.compress(Bitmap.CompressFormat.JPEG, jpegQ, fos)
            }
            val compressMs = System.currentTimeMillis() - compressStart

            // 6) SHA-256
            val hashStart = System.currentTimeMillis()
            val sha256 = sha256File(outFile)
            val hashMs = System.currentTimeMillis() - hashStart

            // 7) Manifest
            val manifest = File(outDir, "proof_${proofId}.json")
            manifest.writeText(
                """
                {
                  "proofId": "$proofId",
                  "inputPath": "${inputFile.absolutePath.replace("\\", "\\\\")}",
                  "outputPath": "${outFile.absolutePath.replace("\\", "\\\\")}",
                  "sha256": "$sha256",
                  "quality": "$quality",
                  "maxMp": $maxMp,
                  "gpsState": "$gpsState",
                  "lat": ${lat ?: "null"},
                  "lon": ${lon ?: "null"},
                  "accM": ${accM ?: "null"}
                }
                """.trimIndent()
            )

            val chainMs = System.currentTimeMillis() - startMs

            val out = Data.Builder()
                .putString(Keys.OUTPUT_PATH, outFile.absolutePath)
                .putString(Keys.OUTPUT_HASH, sha256)
                .putLong(Keys.OVERLAY_MS, overlayMs)
                .putLong(Keys.COMPRESS_MS, compressMs)
                .putLong(Keys.HASH_MS, hashMs)
                .putLong(Keys.CHAIN_MS, chainMs)
                .build()

            Result.success(out)
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    // helpers
    private fun exifRotation(exif: ExifInterface): Int = when (
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90  -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    private fun rotateFromExif(src: Bitmap, deg: Int): Bitmap {
        if (deg == 0) return src
        val m = Matrix().apply { postRotate(deg.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true).also {
            if (it != src && !src.isRecycled) src.recycle()
        }
    }

    private fun drawMultiLineWatermark(base: Bitmap, lines: List<String>): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        val pad = out.width * 0.035f
        val textSize = out.width * 0.035f

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            style = Paint.Style.STROKE
            this.textSize = textSize
            strokeWidth = textSize * 0.12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
            this.textSize = textSize
            typeface = stroke.typeface
        }

        var y = out.height - pad
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            c.drawText(line, pad, y, stroke)
            c.drawText(line, pad, y, fill)
            y -= textSize * 1.25f
        }
        return out
    }

    private fun downscaleToMegapixels(src: Bitmap, maxMp: Int): Bitmap {
        val mp = (src.width.toLong() * src.height.toLong()) / 1_000_000.0
        if (mp <= maxMp + 0.01) return src
        val scale = kotlin.math.sqrt(maxMp / mp)
        val nw = (src.width * scale).roundToInt().coerceAtLeast(1)
        val nh = (src.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true).also {
            if (it != src && !src.isRecycled) src.recycle()
        }
    }

    private fun sha256File(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { ins ->
            val buf = ByteArray(8192)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

