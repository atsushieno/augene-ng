import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.0.0-alpha1-rc4"
    id("com.android.library")
    id("maven-publish")
    kotlin("plugin.serialization") version "1.5.20"
}

kotlin {
    android()
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "16"
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)

                implementation("com.squareup.okio:okio:3.0.0-alpha.9")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")

                implementation("dev.atsushieno:ktmidi:0.3.10")
                implementation("dev.atsushieno:mugene:0.2.17")
                implementation("dev.atsushieno:kotractive:0.1")
                implementation("dev.atsushieno:augene:0.1")
                implementation("dev.atsushieno:missingdot:0.1")

                implementation("com.arkivanov.decompose:decompose:0.3.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.3.1")
                api("androidx.core:core-ktx:1.6.0")
                implementation("com.arkivanov.decompose:extensions-compose-jetbrains:0.3.1")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("com.arkivanov.decompose:extensions-compose-jetpack:0.3.1")
            }
        }
        val desktopTest by getting
    }
}

android {
    compileSdkVersion(31)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(31)
    }
}