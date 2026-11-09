plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.personal.jarvis"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.personal.jarvis"
        minSdk = 26
        targetSdk = 32
        versionCode = 1
        versionName = "0.1"
    }
}
