plugins {
    id("org.jetbrains.compose") version "1.0.0-alpha1-rc4"
    id("com.android.application")
    kotlin("android")
}

group = "dev.atsushieno"
version = "1.0"

dependencies {
    implementation(project(":common"))
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation("dev.atsushieno:augene:0.1")
}

android {
    compileSdkVersion(31)
    defaultConfig {
        applicationId = "dev.atsushieno.augene"
        minSdkVersion(24)
        targetSdkVersion(31)
        versionCode = 1
        versionName = "0.1"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}