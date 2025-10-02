package com.example.proofmark.core.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Urdu-safe A4 PDF builder with embedded font rendering + size tuning.
 *
 * Fonts (place at least one set):
 *  - res/font/noto_sans_regular.ttf
 *  - res/font/noto_naskh_arabic_regular.ttf
 * OR
 *  - assets/fonts/noto_sans_regular.ttf
 *  - assets/fonts/noto_naskh_arabic_regular.ttf
 */
object PdfReport {

    /** Back-compat model some older call-sites might still reference (not required for builder). */
    data class Manifest(
        val proofId: String,
        val outputPath: String,
        val bytes: Long,
        val sha256: String?,
        val quality: String?,
        val maxMp: Int?
    )

    /** Tunable presets for PDF size/quality. */
    enum class SizePreset(
        val maxImageMp: Int,            // cap image pixels (width*height)
        val bitmapConfig: Bitmap.Config // ARGB_8888 (best) vs RGB_565 (smaller)
    ) {
        LOW (maxImageMp = 2_000_000, bitmapConfig = Bitmap.Config.RGB_565),
        MED (maxImageMp = 4_000_000, bitmapConfig = Bitmap.Config.RGB_565),   // default
        HIGH(maxImageMp = 7_000_000, bitmapConfig = Bitmap.Config.ARGB_8888)
    }

    // A4 @ ~96dpi
    private const val PAGE_W = 1190
    private const val PAGE_H = 1684

    // margins/spacing
    private const val MARGIN = 56
    private const val SECTION_GAP = 22

    // ---------- PUBLIC API (keeps old call signature) ----------
    fun buildA4Report(
        context: Context,
        reportId: String,
        proofId: String,
        outImage: File,
        manifest: PdfZip.Manifest?
    ): File = buildA4Report(
        context = context,
        reportId = reportId,
        proofId = proofId,
        outImage = outImage,
        manifest = manifest,
        preset = SizePreset.MED // default 2–5 MB target
    )

    /** Overload with quality preset if you want to force LOW/HIGH from UI. */
    fun buildA4Report(
        context: Context,
        reportId: String,
        proofId: String,
        outImage: File,
        manifest: PdfZip.Manifest?,
        preset: SizePreset
    ): File {
        val outDir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val outFile = File(outDir, "$reportId.pdf")

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = pdf.startPage(pageInfo)
        val c = page.canvas

        // fonts (res/font or assets/fonts)
        val tfSans  = loadTypefaceByName(context, "noto_sans_regular")
        val tfNaskh = loadTypefaceByName(context, "noto_naskh_arabic_regular")

        // paints
        val title = makePaint(tfSans, 30f, bold = true)
        val sub   = makePaint(tfSans, 16f)
        val body  = makePaint(tfSans, 14f)
        val rtl   = makePaint(tfNaskh, 18f) // Urdu
        val small = makePaint(tfSans, 12f)

        // header
        var y = MARGIN.toFloat()
        drawTextLtr(c, "ProofMark – Report", MARGIN.toFloat(), y, title); y += title.textSize + 6
        drawTextLtr(c, "Report: $reportId", MARGIN.toFloat(), y, sub);      y += sub.textSize + 2
        drawTextLtr(c, "Generated: ${utcNow()}", MARGIN.toFloat(), y, sub); y += SECTION_GAP

        // Urdu explainer (RTL)
        drawTextRtl(
            c,
            "یہ PDF رپورٹ تصویر کی سالمیت (ہیش) اور اہم معلومات پیش کرتی ہے",
            (PAGE_W - MARGIN).toFloat(),
            y,
            rtl
        )
        y += SECTION_GAP

        // meta card
        val cardTop = y
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#DDDDDD")
            strokeWidth = 2f
        }
        val cardRect = RectF(MARGIN.toFloat(), cardTop, (PAGE_W - MARGIN).toFloat(), cardTop + 168f)
        c.drawRoundRect(cardRect, 14f, 14f, cardPaint)
        var metaY = cardTop + 26f

        drawTextLtr(c, "Proof ID: $proofId", MARGIN + 18f, metaY, body); metaY += body.textSize + 10f
        drawTextLtr(
            c,
            "Quality: ${manifest?.quality ?: "MED"}   •   MaxMP: ${manifest?.maxMp ?: "-"}",
            MARGIN + 18f, metaY, body
        ); metaY += body.textSize + 10f
        drawTextLtr(
            c,
            "Output: ${manifest?.outputPath ?: outImage.absolutePath}",
            MARGIN + 18f, metaY, body
        ); metaY += body.textSize + 10f
        drawTextLtr(
            c,
            "Size: ${formatBytes(outImage.length())}",
            MARGIN + 18f, metaY, body
        )

        y = cardRect.bottom + SECTION_GAP

        // image (size-tuned)
        val contentW = PAGE_W - (MARGIN * 2)
        val contentH = (PAGE_H * 0.42f).toInt()
        val bmp = scaledBitmapForPdf(
            srcPath = outImage.absolutePath,
            maxW = contentW,
            maxH = contentH,
            preset = preset
        )
        bmp?.let {
            val left = MARGIN + ((contentW - it.width) / 2f)
            c.drawBitmap(it, left, y, null)
            y += it.height + SECTION_GAP
            it.recycle()
        } ?: run {
            drawTextLtr(c, "(image decode failed)", MARGIN.toFloat(), y, body)
            y += body.textSize + SECTION_GAP
        }

        // integrity table
        val tableTitle = makePaint(tfSans, 18f, bold = true)
        drawTextLtr(c, "Integrity", MARGIN.toFloat(), y, tableTitle)
        y += tableTitle.textSize + 8f

        val sha = manifest?.sha256 ?: "(not provided)"
        val fname = outImage.name
        val rowH = body.textSize + 8f

        val col1X = MARGIN + 18f     // File
        val col2X = col1X + 170f     // SHA-256
        val col3X = col2X + 460f     // Bytes

        val head = makePaint(tfSans, 14f, bold = true)
        drawTextLtr(c, "File", col1X, y, head)
        drawTextLtr(c, "SHA-256", col2X, y, head)
        drawTextLtr(c, "Bytes", col3X, y, head)
        y += rowH

        val maxHashWidth = (col3X - 24f) - col2X
        val hashLines = splitToWidth(sha, body, maxHashWidth)

        drawTextLtr(c, fname, col1X, y, body)
        hashLines.forEachIndexed { idx, line -> drawTextLtr(c, line, col2X, y + (idx * rowH), body) }
        drawTextLtr(c, outImage.length().toString(), col3X, y, body)
        y += max(rowH, rowH * hashLines.size)

        // footer
        val footY = (PAGE_H - MARGIN).toFloat()
        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 1f }
        c.drawLine(MARGIN.toFloat(), footY, (PAGE_W - MARGIN).toFloat(), footY, footPaint)
        drawTextLtr(c, "ProofMark • A4 report • ${utcNow()}", MARGIN.toFloat(), footY - 6f, small)

        pdf.finishPage(page)
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()

        return outFile
    }

    // ----------------- Helpers (single definitions; no duplicates) -----------------

    /** Try res/font/<name>.ttf → assets/fonts/<name>.ttf → null */
    private fun loadTypefaceByName(ctx: Context, fontBaseName: String): Typeface? = try {
        val id = ctx.resources.getIdentifier(fontBaseName, "font", ctx.packageName)
        if (id != 0) {
            ResourcesCompat.getFont(ctx, id)
        } else {
            val assetPath = "fonts/${fontBaseName}.ttf"
            try { Typeface.createFromAsset(ctx.assets, assetPath) } catch (_: Throwable) { null }
        }
    } catch (_: Throwable) { null }

    private fun makePaint(tf: Typeface?, size: Float, bold: Boolean = false): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.BLACK
        p.textSize = size
        p.typeface = tf ?: Typeface.SANS_SERIF
        if (bold) p.typeface = Typeface.create(p.typeface, Typeface.BOLD)
        return p
    }

    private fun drawTextLtr(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        canvas.drawText(text, x, y, paint)
    }

    private fun drawTextRtl(canvas: Canvas, text: String, xRight: Float, y: Float, paint: Paint) {
        val w = paint.measureText(text)
        canvas.drawText(text, xRight - w, y, paint)
    }

    private fun utcNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private fun formatBytes(b: Long): String {
        if (b < 1024) return "$b B"
        val kb = b / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }

    /**
     * Decode + scale image for PDF with size preset.
     * Scales to fit (maxW,maxH) AND caps total pixels by preset.maxImageMp.
     */
    private fun scaledBitmapForPdf(
        srcPath: String,
        maxW: Int,
        maxH: Int,
        preset: SizePreset
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(srcPath, bounds)
        val w0 = bounds.outWidth
        val h0 = bounds.outHeight
        if (w0 <= 0 || h0 <= 0) return null

        val fitScale = min(maxW / w0.toFloat(), maxH / h0.toFloat()).coerceAtMost(1f)

        val px0 = w0.toLong() * h0.toLong()
        val pxCap = preset.maxImageMp.toLong()
        val capScale = if (px0 > pxCap) {
            kotlin.math.sqrt(pxCap.toDouble() / px0.toDouble()).toFloat()
        } else 1f

        val scale = min(fitScale, capScale).coerceAtMost(1f)
        val targetW = max(1, (w0 * scale).toInt())
        val targetH = max(1, (h0 * scale).toInt())

        var sample = 1
        while ((w0 / sample) > targetW * 2 || (h0 / sample) > targetH * 2) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = preset.bitmapConfig
        }
        val raw = BitmapFactory.decodeFile(srcPath, opts) ?: return null
        if (raw.width == targetW && raw.height == targetH) return raw
        val scaled = Bitmap.createScaledBitmap(raw, targetW, targetH, true)
        if (scaled !== raw) raw.recycle()
        return scaled
    }

    private fun splitToWidth(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.chunked(8)
        val out = mutableListOf<String>()
        var line = ""
        for (w in words) {
            val tryLine = if (line.isEmpty()) w else "$line $w"
            if (paint.measureText(tryLine) <= maxWidth) {
                line = tryLine
            } else {
                if (line.isNotEmpty()) out += line
                line = w
            }
        }
        if (line.isNotEmpty()) out += line
        return out
    }
}
