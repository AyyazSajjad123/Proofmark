package com.example.proofmark.feature.settings

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.proofmark.core.data.prefs.Keys
import com.example.proofmark.core.data.prefs.settingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        // Load & observe DataStore → State
        viewModelScope.launch {
            ctx.settingsDataStore.data
                .map { p ->
                    SettingsState(
                        language  = p[Keys.LANGUAGE] ?: "en",
                        quality   = p[Keys.QUALITY] ?: "MED",
                        maxMp     = p[Keys.MAX_MP] ?: 12,
                        watermark = when ((p[Keys.WATERMARK] ?: "ON").lowercase()) {
                            "off","false","0","none" -> false
                            else -> true
                        },
                        flash     = p[Keys.FLASH] ?: "AUTO"
                    )
                }
                .collectLatest { _state.value = it }
        }
    }

    fun setLanguage(lang: String) = viewModelScope.launch {
        ctx.settingsDataStore.edit { it[Keys.LANGUAGE] = lang }
        // Apply immediately
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
    }

    fun setQuality(q: String) = viewModelScope.launch {
        ctx.settingsDataStore.edit { it[Keys.QUALITY] = q }
    }

    fun setMaxMp(mp: Int) = viewModelScope.launch {
        ctx.settingsDataStore.edit { it[Keys.MAX_MP] = mp }
    }

    fun setWatermark(on: Boolean) = viewModelScope.launch {
        // store as "ON"/"OFF" (your Key is String type)
        ctx.settingsDataStore.edit { it[Keys.WATERMARK] = if (on) "ON" else "OFF" }
    }

    fun setFlash(mode: String) = viewModelScope.launch {
        ctx.settingsDataStore.edit { it[Keys.FLASH] = mode }
    }
}
