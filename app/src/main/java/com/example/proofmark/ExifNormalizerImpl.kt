package com.example.proofmark.core.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

class SimpleExifNormalizer : ExifNormalizer {
    override fun normalizeAndUnmirror(inputPath: String): String {
        val inFile = File(inputPath)
        val exif = ExifInterface(inFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val bm = BitmapFactory.decodeFile(inFile.absolutePath) ?: return inputPath
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE  -> { m.postRotate(90f);  m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL   -> m.postScale(1f, -1f)
        }

        val outBm = if (!m.isIdentity) Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true) else bm
        if (outBm === bm && orientation == ExifInterface.ORIENTATION_NORMAL) return inputPath

        val out = File(inFile.parentFile, inFile.nameWithoutExtension + "_norm.jpg")
        FileOutputStream(out).use { outBm.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        outBm.recycle()
        if (bm !== outBm) bm.recycle()
        return out.absolutePath
    }
}
