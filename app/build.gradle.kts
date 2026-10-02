import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.snapcrop.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.snapcrop.app"
        minSdk = 29
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    // The release key never lives in the repo. It comes from keystore.properties
    // (local builds, gitignored) or SNAPCROP_KEYSTORE* env vars (CI). Without
    // either, release builds come out unsigned, which is what F-Droid's own
    // build server and contributors need.
    val keystoreProps = Properties().apply {
        rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    val keystorePath = System.getenv("SNAPCROP_KEYSTORE") ?: keystoreProps.getProperty("storeFile")
    val keystorePassword = System.getenv("SNAPCROP_KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = rootProject.file(keystorePath)
                storePassword = keystorePassword
                keyAlias = System.getenv("SNAPCROP_KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias", "snapcrop")
                keyPassword = keystorePassword
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
}
