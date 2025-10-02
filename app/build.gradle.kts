@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")   // Compose compiler for Kotlin 2.0
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("kapt")
}

android {
    namespace = "com.example.proofmark"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.proofmark"
        minSdk = 24
        targetSdk = 35
        versionCode = 10100
        versionName = "1.0.1"
        vectorDrawables.useSupportLibrary = true

        // If any lib module misses the 'env' dimension, fall back safely
        missingDimensionStrategy("env", "dev", "prod")
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationId = "com.example.proofmark.dev"
            versionNameSuffix = "-dev"
        }
        create("prod") {
            dimension = "env"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Crashlytics stays off by default in debug
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true // used in ProofApp
    }

    // With Kotlin 2.0 + compose plugin, no need to set composeOptions version explicitly

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/NOTICE",
            "META-INF/LICENSE*"
        )
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {

    implementation("androidx.profileinstaller:profileinstaller:1.3.1")


    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ---- AndroidX base ----
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")    // keep only this; no version-catalog line

    // ---- Biometric (stable) ----
    implementation("androidx.biometric:biometric:1.1.0")

    // ---- Compose BOM ----
    val composeBom = platform("androidx.compose:compose-bom:2024.09.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Material (Views)
    implementation("com.google.android.material:material:1.12.0")

    // ---- CameraX ----
    val cameraX = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    // EXIF
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Work / Room / DataStore
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ---- Firebase (BoM) ----
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-appcheck")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    // ---- Project modules ----
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:crypto"))
    implementation(project(":core:data"))
    implementation(project(":core:pdf"))
    implementation(project(":feature:capture"))
    implementation(project(":feature:library"))
    implementation(project(":feature:verify"))
    implementation(project(":feature:report"))
    implementation(project(":feature:settings"))
    implementation(project(":work"))

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
}

kapt {
    correctErrorTypes = true
}
