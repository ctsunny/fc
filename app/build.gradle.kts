import java.time.Instant
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val baseVersionName = "1.0.0"
val autoBuildId = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull
    ?.takeIf { it.isNotBlank() }
    ?: providers.environmentVariable("BUILD_NUMBER").orNull
        ?.takeIf { it.isNotBlank() }
    ?: Instant.now().epochSecond.toString()
val autoVersionCode = autoBuildId.toIntOrNull()?.coerceAtLeast(1) ?: 1
val autoVersionName = "$baseVersionName+$autoBuildId"

// ── Signing ────────────────────────────────────────────────────────────────
// Priority order:
//  1. keystore.properties file next to this build file (local developer machine)
//  2. Environment variables KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD (CI)
//  3. Fall back to the Android debug keystore (local builds without explicit config)
val keystorePropsFile = rootProject.file("keystore.properties")
val signingProps = Properties().also { props ->
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { props.load(it) }
}

fun signingProp(name: String): String? =
    signingProps.getProperty(name) ?: providers.environmentVariable(name).orNull

android {
    namespace = "com.fc.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fc.app"
        minSdk = 24
        targetSdk = 34
        versionCode = autoVersionCode
        versionName = autoVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Consistent signing: both debug and release use the same key so that
    // updating from one build to the next never triggers a "signature conflict"
    // as long as the same keystore is used.  See keystore.properties.template
    // in the repo root for the required keys.
    val keystorePath = signingProp("KEYSTORE_PATH")
    val keystorePassword = signingProp("KEYSTORE_PASSWORD")
    val keyAlias = signingProp("KEY_ALIAS")
    val keyPassword = signingProp("KEY_PASSWORD")

    if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Remove the .debug suffix: debug and release builds share the same
            // applicationId (com.fc.app) so a user can upgrade between them without
            // a package-manager signature conflict.
            // applicationIdSuffix = ".debug"  ← intentionally removed
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig != null) signingConfig = releaseSigningConfig
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig != null) signingConfig = releaseSigningConfig
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.common)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.video)
    implementation(libs.camerax.view)
    implementation(libs.okhttp)
    implementation(libs.security.crypto)
    debugImplementation(libs.androidx.ui.tooling)
}
