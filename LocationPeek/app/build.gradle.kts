plugins {
    id("com.android.application")
}

android {
    namespace = "me.lordspigot.locationpeek"
    compileSdk = 35

    defaultConfig {
        applicationId = "me.lordspigot.locationpeek"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
