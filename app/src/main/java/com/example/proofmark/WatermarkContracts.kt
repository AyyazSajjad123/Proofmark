package com.example.proofmark.core.common

enum class WatermarkPreset { Light, Dark }

// (Optional) Spec class — workers ko iski zaroorat nahi, par future use ke liye rehne do:
data class WatermarkSpec(
    val utc: String,
    val local: String,
    val lat: Double?,
    val lon: Double?,
    val accuracyM: Float?,
    val deviceIdShort: String,
    val proofIdShort: String,
    val preset: WatermarkPreset
)
