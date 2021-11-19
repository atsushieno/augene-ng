import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.0.0-beta3"
    id("com.android.library")
    id("maven-publish")
}

kotlin {
    android {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
    }
    jvm("desktop") {
        compilations.all { kotlinOptions.jvmTarget = "11" }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)

                implementation("com.squareup.okio:okio-multiplatform:3.0.0-alpha.9")

                implementation("dev.atsushieno:ktmidi:0.3.15")
                implementation("dev.atsushieno:mugene:0.2.23")
                implementation("dev.atsushieno:kotractive:0.1")
                implementation("dev.atsushieno:augene:0.1")
                implementation("dev.atsushieno:missingdot:0.1.5")
                implementation("dev.atsushieno:compose-mpp:0.1.2")

                implementation("com.arkivanov.decompose:decompose:0.4.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.4.0")
                api("androidx.core:core-ktx:1.7.0")
                implementation("com.arkivanov.decompose:extensions-compose-jetpack:0.4.0")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("com.arkivanov.decompose:extensions-compose-jetpack:0.4.0")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.31")
                implementation("org.slf4j:slf4j-api:1.7.32")
                implementation("org.slf4j:slf4j-simple:1.7.32")
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
