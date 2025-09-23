package com.example.proofmark.capture

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

data class GpsSnapshot(
    val lat: Double?, val lon: Double?, val accM: Float?, val fixAgeSec: Long, val state: String // Fresh/Stale/None
)

data class SaveResult(val photo: File, val manifest: File, val sha256: String) {
    val sha256Short get() = sha256.take(8)
}

class CapturePipeline(private val ctx: Context) {

    // Tunables
    private val maxMegaPixels = 12        // later: read from Remote Config
    private val jpegQuality = 85          // MED (85)
    private val watermarkPreset = "auto"  // “Light/Dark” auto

    fun processAndSave(inputJpeg: File, frontCamera: Boolean, gps: GpsSnapshot): SaveResult? {
        try {
            // 1) Decode & normalize orientation
            val srcBytes = inputJpeg.readBytes()
            val normalized = normalizeJpeg(srcBytes, frontCamera)

            // 2) Downscale cap (<= 12MP)
            val capped = capMegaPixels(normalized, maxMegaPixels)

            // 3) Watermark (UTC/Local/GPS/deviceId/proofId short)
            val wm = drawWatermark(capped.bitmap, gps)

            // 4) Compress (MED)
            val finalJpeg = ByteArrayOutputStream().use { bos ->
                wm.compress(Bitmap.CompressFormat.JPEG, jpegQuality, bos)
                bos.toByteArray()
            }

            // 5) Save to files/
            val dir = File(ctx.filesDir, "proofs/${dayFolder()}").apply { mkdirs() }
            val id = System.currentTimeMillis().toString(36)
            val photoFile = File(dir, "P_$id.jpg").apply { writeBytes(finalJpeg) }

            // 6) Hash SHA-256 (streamed)
            val sha = sha256(photoFile)

            // 7) Manifest JSON v1
            val man = JSONObject(
                mapOf(
                    "schema" to "proofmark.manifest.v1",
                    "proofId" to id,
                    "utc" to isoUtc(),
                    "local" to isoLocal(),
                    "gps" to mapOf(
                        "lat" to gps.lat,
                        "lon" to gps.lon,
                        "accM" to gps.accM,
                        "state" to gps.state
                    ),
                    "bytes" to photoFile.length(),
                    "sha256" to sha
                )
            ).toString()

            val manFile = File(dir, "P_$id.json").apply { writeText(man) }

            // 8) Clean temp
            inputJpeg.delete()

            return SaveResult(photoFile, manFile, sha)
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }

    // --- helpers ---

    private data class Decoded(val bitmap: Bitmap, val rotation: Int)

    private fun normalizeJpeg(bytes: ByteArray, front: Boolean): Decoded {
        val bmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) // streaming later
        // Try read EXIF orientation
        val rotation = 0 // CameraX mostly writes oriented JPEG; we’ll rotate if needed later
        var bmp = bmap
        // Front camera unmirror
        if (front) bmp = bmp.flipHorizontal()
        return Decoded(bmp, rotation)
    }

    private data class Capped(val bitmap: Bitmap)
    private fun capMegaPixels(decoded: Decoded, maxMp: Int): Capped {
        val bmp = decoded.bitmap
        val mp = (bmp.width.toLong() * bmp.height.toLong()) / 1_000_000.0
        if (mp <= maxMp) return Capped(bmp)
        val scale = kotlin.math.sqrt(maxMp / mp)
        val w = (bmp.width * scale).toInt().coerceAtLeast(1)
        val h = (bmp.height * scale).toInt().coerceAtLeast(1)
        val out = Bitmap.createScaledBitmap(bmp, w, h, true)
        return Capped(out)
    }

    private fun drawWatermark(bmp: Bitmap, gps: GpsSnapshot): Bitmap {
        val out = bmp.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 6f
            textSize = (max(bmp.width, bmp.height) * 0.025f)
        }
        val pFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = pStroke.textSize
        }
        val margin = 24f
        val lines = buildList {
            add("UTC ${isoUtcShort()}  |  Local ${isoLocalShort()}")
            val gpsLine = when (gps.state) {
                "Fresh" -> "GPS ${fmt(gps.lat)}, ${fmt(gps.lon)} ±${gps.accM ?: 0}m"
                "Stale" -> "GPS Stale"
                else -> "GPS None"
            }
            add(gpsLine)
            add("Device ${deviceIdShort()}  |  Proof ${proofIdShort()}")
        }
        val x = margin
        var y = out.height - margin
        for (i in lines.indices.reversed()) {
            val text = lines[i]
            // dual-stroke
            c.drawText(text, x, y, pStroke)
            c.drawText(text, x, y, pFill)
            y -= (pFill.textSize + 10f)
        }
        return out
    }

    private fun Bitmap.flipHorizontal(): Bitmap {
        val m = Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buf = ByteArray(8 * 1024)
            var n: Int
            while (fis.read(buf).also { n = it } > 0) md.update(buf, 0, n)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isoUtc() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

    private fun isoUtcShort() = SimpleDateFormat("HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

    private fun isoLocal() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    private fun isoLocalShort() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    private fun dayFolder() = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun deviceIdShort(): String {
        val id = android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        return (id ?: "NA").takeLast(6)
    }
    private fun proofIdShort(): String = System.currentTimeMillis().toString(36).takeLast(5)

    private fun fmt(v: Double?): String = if (v == null) "--" else "%.6f".format(v)
}
