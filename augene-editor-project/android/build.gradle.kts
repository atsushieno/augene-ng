plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
}

group = "dev.atsushieno"
version = "1.0"

dependencies {
    implementation(project(":common"))
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("dev.atsushieno:augene:0.2")
}

android {
    namespace = "dev.atsushieno.augene.gui"
    compileSdk = 31
    defaultConfig {
        applicationId = "dev.atsushieno.augene.gui"
        minSdk = 24
        targetSdk = 31
        versionCode = 1
        versionName = "0.1"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}