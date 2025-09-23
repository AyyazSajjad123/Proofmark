package com.example.proofmark.core.common

interface ExifNormalizer {
    /** Returns path of normalized (possibly temp) JPEG */
    fun normalizeAndUnmirror(inputPath: String): String
}
