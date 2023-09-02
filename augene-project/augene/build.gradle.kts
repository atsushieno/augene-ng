import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.dokka") version "1.8.20"
    id("maven-publish")
    id("signing")
    kotlin("plugin.serialization") version "1.9.0"
}

kotlin {
    androidTarget {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("debug", "release")
    }
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(IR) { // it depends on mugene which does not support BOTH
        nodejs {
            testTask(Action {
                // FIXME: enable this once this error got fixed:
                // Module not found: Error: Can't resolve 'os' in '/media/atsushi/extssd0/sources/ktmidi/augene-ng/augene-project/build/js/node_modules/okio-parent-okio-js-legacy'
                enabled = false
                useKarma {
                    useChromeHeadless()
                    //webpackConfig.cssSupport.enabled = true
                }
            })
            useCommonJs()
        }
        //browser() - okio FileSystem.SYSTEM is not available on browsers yet.
    }
    /*
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    */

    sourceSets {
        /*
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }*/

        val androidMain by getting
        /*
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }*/
        val commonMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio-multiplatform:3.0.0-alpha.9")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

                implementation("dev.atsushieno:ktmidi:0.6.0-preview3")
                implementation("dev.atsushieno:mugene:0.4.3")
                implementation("dev.atsushieno:missingdot:0.1.5")
                implementation("dev.atsushieno:kotractive:0.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio-nodefilesystem-js:3.0.0-alpha.9")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        //val nativeMain by getting
        //val nativeTest by getting
    }
}

android {
    namespace = "dev.atsushieno.augene"
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].assets.srcDir("src/commonMain/resources") // kind of hack...
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
    buildTypes {
        val debug by getting {
            //minifyEnabled(false)
        }
        val release by getting {
            //minifyEnabled(false)
        }
    }
}
