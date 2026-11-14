import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val runNum = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val keystoreB64File = file("keystore/keystore.b64")
val keystoreFile = file("keystore/smartfinance.jks")
val keystorePassword = "smart1234"
val keystoreAlias = "smartfinance"

// Fester Base64-Keystore direkt im Repository (app/keystore/keystore.b64) + automatische Dekodierung
val embeddedKeystoreBase64 =
    "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"

keystoreFile.parentFile?.mkdirs()
val b64Content = if (keystoreB64File.exists()) {
    keystoreB64File.readText().replace("\\s".toRegex(), "")
} else {
    embeddedKeystoreBase64
}
keystoreFile.writeBytes(Base64.getDecoder().decode(b64Content))

android {
    namespace = "de.smartfinance.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.smartfinance.app"
        minSdk = 29
        targetSdk = 35
        versionCode = (System.currentTimeMillis() / 10000).toInt()
        versionName = "1.1.$runNum"

        buildConfigField("int", "BUILD_NUMBER", "$runNum")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreFile
            storePassword = keystorePassword
            keyAlias = keystoreAlias
            keyPassword = keystorePassword
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Room Database (Strikt ohne destruktive Migrationen!)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Offline Beleg-Scanner (Google ML Kit OCR On-Device)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
