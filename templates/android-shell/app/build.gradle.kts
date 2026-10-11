plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    // Program C wave C4 render gate (docs/Almadar_Compiler_Gaps.md §157):
    // Robolectric-hosted Compose snapshots via `verifyRoborazziDebug`/
    // `recordRoborazziDebug`, generated into app/src/test/kotlin/generated/
    // PatternSnapshotTests.kt by AndroidBackend.
    id("io.github.takahirom.roborazzi")
}

android {
    namespace = "{{APP_PACKAGE}}"
    compileSdk = 34

    defaultConfig {
        applicationId = "{{APP_PACKAGE}}"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Identity provider for native sign-in (manifest meta-data read by OrbitalSignIn.kt).
        // Pass real values as -PORBITAL_FIREBASE_API_KEY=... ; blank leaves sign-in unconfigured.
        manifestPlaceholders["orbitalFirebaseApiKey"] = providers.gradleProperty("ORBITAL_FIREBASE_API_KEY").getOrElse("")
        manifestPlaceholders["orbitalFirebaseProjectId"] = providers.gradleProperty("ORBITAL_FIREBASE_PROJECT_ID").getOrElse("")
        manifestPlaceholders["orbitalAuthEmulatorHost"] = providers.gradleProperty("ORBITAL_AUTH_EMULATOR_HOST").getOrElse("")
    }

    buildTypes {
        release {
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
    testOptions {
        // Robolectric (JVM-hosted, SDK 34 — matches compileSdk/targetSdk above)
        // needs the real android.jar resources on the unit-test classpath.
        unitTests.isIncludeAndroidResources = true
        // Canvases are drawn by the Rust renderer through JNI; a JVM test loads the host build of
        // `orbital_ffi` (ORBITAL_HOST_LIB_DIR, set by the native smoke test) instead of the device .so.
        unitTests.all { test ->
            providers.environmentVariable("ORBITAL_HOST_LIB_DIR").orNull?.let { test.systemProperty("java.library.path", it) }
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    externalNativeBuild {
        // The orbital-canvas .so is pre-built by the Orbital toolchain
        // and placed in src/main/jniLibs/<ABI>/ before the Gradle build.
        // No CMakeLists.txt needed — the library is loaded at runtime via
        // System.loadLibrary("orbital_shell_android") in OrbitalGameBridge.kt.
    }
}

dependencies {
    // Compose 1.7 line (ui 1.7.4, material3 1.3.0): rememberGraphicsLayer backs the surface backdrop blur.
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    // Live-push subscription (GET /api/events, wave B7) — pinned to the same
    // OkHttp line ktor-client-okhttp:2.3.7 already pulls in (4.12.x), so the
    // ktor HTTP client and the raw SSE client share one OkHttp version.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    // Network image loading for `image`/`avatar` (web `<img src>` parity; iOS uses AsyncImage).
    implementation("io.coil-kt:coil-compose:2.6.0")
    // SVG decoding for image sources (registered once in the shared ImageLoader, selected by MIME type).
    implementation("io.coil-kt:coil-svg:2.6.0")
    // OpenStreetMap tiles for `map-view` (same Mapnik tile source as the web's Leaflet; no API key).
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    // Camera preview + native QR decode for `qr-scanner` (CameraX 1.3 line matches compileSdk 34; ML Kit ships the model in-app, no Play Services needed).
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    // Compose interop for OrbitalGameCanvas (AndroidView)
    implementation("androidx.compose.ui:ui-viewbinding")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Program C wave C4 render gate: Robolectric hosts Compose off-device,
    // Roborazzi captures + compares the snapshot PNG (docs/Almadar_Compiler_Gaps.md §157).
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.43.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.43.1")
}
