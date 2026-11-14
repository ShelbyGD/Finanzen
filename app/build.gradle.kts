plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val keystoreFile = file("keystore/smartfinance.jks")
val keystorePassword = "SmartFinance2026Key!"
val keystoreAlias = "smartfinance_key"

// Fallback: Erzeugt den deterministischen Keystore automatisch beim Konfigurieren,
// falls app/keystore/smartfinance.jks lokal noch nicht existiert.
if (!keystoreFile.exists()) {
    keystoreFile.parentFile?.mkdirs()
    val process = ProcessBuilder(
        "keytool", "-genkeypair", "-v",
        "-keystore", keystoreFile.absolutePath,
        "-alias", keystoreAlias,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-storepass", keystorePassword,
        "-keypass", keystorePassword,
        "-dname", "CN=SmartFinance 2026, OU=Mobile, O=SmartFinance, L=Berlin, ST=Berlin, C=DE"
    ).inheritIO().start()
    process.waitFor()
}

android {
    namespace = "de.smartfinance.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.smartfinance.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 100 + runNumber
        versionName = "1.0.$runNumber"

        buildConfigField("int", "BUILD_NUMBER", "$runNumber")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("smartfinance") {
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
            signingConfig = signingConfigs.getByName("smartfinance")
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.getByName("smartfinance")
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
