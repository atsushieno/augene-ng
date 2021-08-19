plugins {
    id("org.jetbrains.compose") version "1.0.0-alpha1-rc4"
    id("com.android.application")
    kotlin("android")
}

group = "dev.atsushieno"
version = "1.0"

dependencies {
    implementation(project(":common"))
}

android {
    compileSdkVersion(30)
    defaultConfig {
        applicationId = "dev.atsushieno.android"
        minSdkVersion(24)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}