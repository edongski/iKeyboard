plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.ikey.keyboard"
    compileSdk = 35
    defaultConfig { applicationId = "com.ikey.keyboard"; minSdk = 28; targetSdk = 35; versionCode = 1; versionName = "0.1-phase1" }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
