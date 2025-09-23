package com.example.proofmark.core.common

import android.content.Context
import android.graphics.*
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/* local GPS helper to avoid cross-file dependency */
private data class GpsState(val state: String, val accuracyM: Float?)
private fun deriveGpsState(loc: android.location.Location?, nowMs: Long = System.currentTimeMillis()): GpsState =
    when {
        loc == null -> GpsState("NONE", null)
        (nowMs - loc.time) <= 10_000 -> GpsState("FRESH", if (loc.hasAccuracy()) loc.accuracy else null)
        (nowMs - loc.time) > 120_000 -> GpsState("STALE", if (loc.hasAccuracy()) loc.accuracy else null)
        else -> GpsState("FRESH", if (loc.hasAccuracy()) loc.accuracy else null)
    }

object WatermarkUtil {

    data class Result(val outputPath: String)

    fun drawWatermark(
        context: Context,
        inputPath: String,
        proofId: String,
        loc: android.location.Location?,
        preset: WatermarkPreset = WatermarkPreset.Dark
    ): Result {
        val src = BitmapFactory.decodeFile(inputPath) ?: return Result(inputPath)
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(src, 0f, 0f, null)

        val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val localFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val local = localFmt.format(Date())

        val devId = (Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "na").take(2)
        val pf = proofId.take(2)

        val gps = deriveGpsState(loc)
        val lat = loc?.latitude
        val lon = loc?.longitude

        val lines = buildList {
            add("UTC $utc")
            add("Local $local")
            if (lat != null && lon != null) {
                val acc = gps.accuracyM?.toInt()?.let { " ± ${it}m" } ?: ""
                add("Lat ${"%.5f".format(lat)}, Lon ${"%.5f".format(lon)}$acc (${gps.state})")
            } else add("GPS ${gps.state}")
            add("dev: $devId | proof: $pf")
        }

        val paintBg = Paint().apply { color = if (preset == WatermarkPreset.Dark) 0xAA000000.toInt() else 0xAAFFFFFF.toInt() }
        val paintTx = Paint().apply {
            color = if (preset == WatermarkPreset.Dark) Color.WHITE else Color.BLACK
            isAntiAlias = true
            textSize = (src.width * 0.022f).coerceAtLeast(28f)
        }

        val pad = (src.width * 0.02f).coerceAtLeast(18f)
        val lineH = paintTx.textSize * 1.35f
        val blockW = lines.maxOf { paintTx.measureText(it) } + pad * 2
        val blockH = lineH * lines.size + pad

        val rect = RectF(pad, src.height - blockH - pad, pad + blockW, src.height - pad)
        canvas.drawRoundRect(rect, pad, pad, paintBg)

        var y = rect.top + pad + paintTx.textSize
        lines.forEach { line ->
            canvas.drawText(line, rect.left + pad, y, paintTx)
            y += lineH
        }

        val outFile = File(File(inputPath).parentFile, File(inputPath).nameWithoutExtension + "_wm.jpg")
        FileOutputStream(outFile).use { out.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        src.recycle(); out.recycle()

        return Result(outFile.absolutePath)
    }
}
