plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")   // ✅ Kotlin 2.x compose compiler
    kotlin("kapt")
}

android {
    namespace = "com.example.proofmark.core.ui"
    compileSdk = 35
    defaultConfig { minSdk = 24 }

    buildFeatures { compose = true }            // ✅ turn on compose
    // ❌ REMOVE any composeOptions { kotlinCompilerExtensionVersion = "..." }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.09.01")
    implementation(bom); androidTestImplementation(bom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
