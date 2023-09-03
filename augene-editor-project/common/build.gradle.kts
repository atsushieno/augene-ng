plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
    id("maven-publish")
}

kotlin {
    jvmToolchain(11)

    androidTarget {
        //compilations.configureEach { kotlinOptions.jvmTarget = "11" }
    }
    jvm("desktop") {
        compilations.configureEach { kotlinOptions.jvmTarget = "17" }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)

                implementation("com.squareup.okio:okio-multiplatform:3.0.0-alpha.9")

                implementation("dev.atsushieno:ktmidi:0.6.0")
                implementation("dev.atsushieno:mugene:0.4.4")
                implementation("dev.atsushieno:kotractive:0.2")
                implementation("dev.atsushieno:augene:0.2")
                implementation("dev.atsushieno:missingdot:0.1.5")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")
            }
        }
        /*
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }*/
        val desktopMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0")
                implementation("org.slf4j:slf4j-api:2.0.4")
                implementation("org.slf4j:slf4j-simple:1.7.32")
            }
        }
        val desktopTest by getting
    }
}

android {
    namespace = "dev.atsushieno.augene.gui"
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
