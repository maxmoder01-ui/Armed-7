
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kayser.armed7"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kayser.armed7"
        minSdk = 23
        targetSdk = 35
        versionCode = 100
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf("armeabi-v7a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
