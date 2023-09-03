plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
}

group = "dev.atsushieno"
version = "1.0"

dependencies {
    implementation(project(":common"))
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("dev.atsushieno:augene:0.2")
}

kotlin {
    jvmToolchain(11)
}

android {
    namespace = "dev.atsushieno.augene.gui"
    compileSdk = 33
    defaultConfig {
        applicationId = "dev.atsushieno.augene.gui"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "0.1"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}