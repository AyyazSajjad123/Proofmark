package com.example.proofmark

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.proofmark.core.data.prefs.settingsDataStore
import com.example.proofmark.core.data.prefs.Keys
import com.google.firebase.BuildConfig

@HiltAndroidApp
class ProofApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase core
        FirebaseApp.initializeApp(this)

        // Crashlytics ON only in release
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // App Check (Play Integrity) — best-effort
        runCatching {
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }

        // Remote Config (safe defaults; all non-null)
        val rc = FirebaseRemoteConfig.getInstance()
        rc.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
            }
        )
        // NOTE: all defaults are non-null types
        rc.setDefaultsAsync(
            mapOf(
                "max_mp_default" to 12,
                "quality_default" to "MED",
                "warn_jpg_share" to true,
                "pdf_default_quality" to "MED"
            )
        )
        rc.fetchAndActivate()

        // Apply saved locale (null-safe read from DataStore)
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = applicationContext.settingsDataStore.data.first()
            val lang: String = prefs[Keys.LANGUAGE] ?: "en"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        }
    }
}
