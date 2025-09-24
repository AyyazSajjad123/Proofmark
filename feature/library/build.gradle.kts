plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.proofmark.feature.library"
    compileSdk = 35
    defaultConfig { minSdk = 24 }

    buildFeatures {
        compose = true         // ✅ TURN COMPOSE ON
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.09.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // ✅ Pick ONE Coil: use v2 (simplest)
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("androidx.core:core-ktx:1.13.1")
}
