plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")   // ✅
    kotlin("kapt")
}

android {
    namespace = "com.example.proofmark.feature.capture"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }            // ✅
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
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // If this module directly uses CameraX classes (it does in your screen):
    val cameraX = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    // Hilt navigation helpers for Compose VMs if used here
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
}
