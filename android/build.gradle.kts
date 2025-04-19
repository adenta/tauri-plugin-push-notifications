plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Apply the Google services plugin conditionally based on project property
    // id("com.google.gms.google-services") apply false // Don't apply here directly
}

android {
    namespace = "com.tauri.pushnotifications" // Use a unique namespace
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    // Set source compatibility to Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.google.firebase:firebase-messaging:23.4.1") // Use a recent version
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // For coroutines
    // Add Tauri Android dependency if needed (usually provided by the app)
    // compileOnly("app.tauri:core:...")
}

// Apply Google services plugin if the google-services.json file exists in the app project
afterEvaluate {
    // Check if the root project (the app) has the google services file property
    if (rootProject.hasProperty("googleServicesFile")) {
        // Only apply if the property exists, indicating google-services.json is present
        apply(plugin = "com.google.gms.google-services")
    }
}
