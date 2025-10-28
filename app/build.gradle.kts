plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tindascan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tindascan"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ✅ FIX 1: Add this block to re-enable BuildConfig
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            // Set to 'false' to BYPASS the splash screen in debug builds
            buildConfigField("boolean", "SHOW_SPLASH", "false")
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // ✅ FIX 2: Set to 'true' to SHOW the splash screen in release builds
            buildConfigField("boolean", "SHOW_SPLASH", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation (libs.gson)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.firebase.crashlytics.buildtools)

    // ✅ CameraX (compatible with compileSdk 35)
    val cameraxVersion = "1.3.3"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ✅ ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // ✅ Permissions + Fragments (API 35 compatible)
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.7.1")

    // ✅ Threading
    implementation("com.google.guava:guava:31.0.1-android")

    // ✅ Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.material:material:1.12.0")
}