import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
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
            implementation(libs.supabase.postgrest)
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

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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

// supabase-kt pulls androidx.browser:1.9.0 (Custom Tabs for OAuth) which needs AGP 8.9.1+.
// We don't use the browser flow yet — pin to 1.8.0 to stay on AGP 8.7.3.
configurations.all {
    resolutionStrategy {
        force("androidx.browser:browser:1.8.0")
        // A transitive dep pulls kotlinx-datetime 0.7 (Clock moved to kotlin.time, breaks our 0.6 code).
        force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    }
}
