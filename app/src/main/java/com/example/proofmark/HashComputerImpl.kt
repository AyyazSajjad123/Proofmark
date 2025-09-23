package com.example.proofmark.core.crypto

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class Sha256Computer : HashComputer {
    override suspend fun sha256Hex(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(8192)
            while (true) {
                val r = fis.read(buf)
                if (r <= 0) break
                md.update(buf, 0, r)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
