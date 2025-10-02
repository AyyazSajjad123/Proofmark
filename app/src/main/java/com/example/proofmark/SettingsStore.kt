package com.example.proofmark.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore("settings")
object Keys {
    val LANGUAGE = stringPreferencesKey("language")           // "en" | "ur"
    val QUALITY  = stringPreferencesKey("quality")            // "LOW"|"MED"|"HIGH"
    val MAX_MP   = intPreferencesKey("max_mp")                // 8|12|16
    val BIOMETRIC= booleanPreferencesKey("biometricEnabled")
    val FLASH    = stringPreferencesKey("flashMode")          // "AUTO"|"OFF"|"ON"
    val WATERMARK= stringPreferencesKey("watermarkPreset")    // "ON"|"OFF" stored as String

    // Day-21: RC bridge values
    val WARN_JPG_SHARE     = booleanPreferencesKey("warn_jpg_share_flag")
    val PDF_REPORT_QUALITY = stringPreferencesKey("pdf_report_quality") // "LOW"|"MED"|"HIGH"
}
