import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Release signing — read from keystore.properties (gitignored). Absent on machines
// that only build debug (e.g. CI/emulator); release signing is skipped there.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation("androidx.exifinterface:exifinterface:1.3.7")
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            // Native MapLibre + OSM map (Android only; iOS uses native SwiftUI MapLibreView).
            implementation(libs.maplibre.android)
            implementation(libs.maplibre.annotation)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // Force the 16 KB-page-aligned build (Compose pulls an older one transitively). Play
            // requires 16 KB support for targetSdk 35+ — this .so was flagged as unaligned.
            implementation("androidx.graphics:graphics-path:1.0.1")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.components.resources)
            implementation(libs.markdown.renderer.m3)
            implementation(libs.okio)
            // Cloud sync via raw PostgREST over ktor (backend-agnostic: swap base URL for Cloudflare later).
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}

android {
    namespace = "com.carettafriends"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.carettafriends"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.carettafriends.resources"
}

// A transitive dep may pull kotlinx-datetime 0.7 (Clock moved to kotlin.time, breaks our 0.6 code).
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    }
}
