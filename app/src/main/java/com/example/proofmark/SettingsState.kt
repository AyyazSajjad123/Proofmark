package com.example.proofmark.feature.settings

data class SettingsState(
    val language: String = "en",   // "en" | "ur"
    val quality: String = "MED",   // "LOW" | "MED" | "HIGH"
    val maxMp: Int = 12,           // 8 | 12 | 16
    val watermark: Boolean = true, // true => ON, false => OFF
    val flash: String = "AUTO"     // "AUTO" | "OFF" | "ON"
)
