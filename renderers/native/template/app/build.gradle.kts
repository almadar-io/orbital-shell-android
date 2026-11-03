plugins {
    id("com.android.application")
}

android {
    namespace = "{{APP_PACKAGE}}"
    compileSdk = 34

    defaultConfig {
        applicationId = "{{APP_PACKAGE}}"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.games:games-activity:2.0.2")
}
