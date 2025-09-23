package com.example.proofmark.core.crypto

import java.io.File

interface HashComputer {
    suspend fun sha256Hex(file: File): String
}
