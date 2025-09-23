package com.example.proofmark

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.BuildConfig
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ProofMarkApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1) Firebase core
        FirebaseApp.initializeApp(this)

        // 2) App Check: dev = Debug provider, prod = Play Integrity
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            FirebaseCrashlytics.getInstance().log("Crashlytics DEBUG collection ENABLED")
        }


        else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        // 3) Crashlytics: ON release, OFF debug
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // 4) Remote Config: defaults + fetch
        val rc = FirebaseRemoteConfig.getInstance()
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()
        rc.setConfigSettingsAsync(settings)

        rc.setDefaultsAsync(
            mapOf(
                "pdfDefaultQuality" to "MED",
                "maxMpDefault" to 12,     // int/long OK
                "warnJpgShare" to true
            )
        )

        rc.fetchAndActivate()
    }
}
