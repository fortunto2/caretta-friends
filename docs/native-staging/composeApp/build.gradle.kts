        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
            // --- MapLibre (Android only) ---
            implementation(libs.maplibre.android)                 // org.maplibre.gl:android-sdk:13.3.1
            implementation(libs.maplibre.annotation)              // android-plugin-annotation-v9:3.0.2 (SymbolManager)
            implementation(libs.androidx.lifecycle.runtime.compose) // LocalLifecycleOwner; drop if already transitive
        }
// No repository changes needed: settings.gradle.kts already has mavenCentral() in
// dependencyResolutionManagement (org.maplibre.gl is published to Maven Central).