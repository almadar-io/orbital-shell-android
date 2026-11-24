plugins {
    id("com.android.application")
}

android {
    namespace = "{{APP_PACKAGE}}"
    compileSdk = 34

    defaultConfig {
        applicationId = "{{APP_PACKAGE}}"
        minSdk = {{NATIVE_GPU_MIN_API}}
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.games:games-activity:2.0.2")
    // Sign-in (OrbitalSignIn): Google through Credential Manager, then Firebase Auth.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}

// The app's Firebase config is optional: an app built without google-services.json has no sign-in.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
