import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.library") version "4.1.3"
    id("com.google.devtools.ksp") version "1.5.21-1.0.0-beta07"
    id("org.jetbrains.kotlin.multiplatform") version "1.5.21"
    id("org.jetbrains.dokka") version "1.5.0"
    id("maven-publish")
    id("signing")
}

kotlin {
    android {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("debug", "release")
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(BOTH) {
        nodejs {
            testTask {
                // FIXME: we want to enable tests, but can't until this error gets fixed.
                //   :kotractive:jsNodeTest: java.lang.IllegalStateException: command '/home/atsushi/.gradle/nodejs/node-v14.15.4-linux-x64/bin/node' exited with errors (exit code: 1)
                enabled = false
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
            useCommonJs()
        }
        browser()
    }
    // e: Could not find "/media/atsushi/extssd0/sources/ktmidi/augene-ng/kotractive-project/kotractive/build/generated/ksp/nativeMain/classes" in [/media/atsushi/extssd0/sources/ktmidi/augene-ng/kotractive-project, /home/atsushi/.konan/klib, /home/atsushi/.konan/kotlin-native-prebuilt-linux-1.5.21/klib/common, /home/atsushi/.konan/kotlin-native-prebuilt-linux-1.5.21/klib/platform/linux_x64]
    /*
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }*/

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation("androidx.startup:startup-runtime:1.1.0")
                if (configurations.get("ksp").dependencies.all { p -> p.name != ":kotractive_ksp" })
                    configurations.get("ksp").dependencies.add(project(":kotractive_ksp"))
            }
            // This cannot be enabled because it seems to bring in extraneous files from different targets.
            //kotlin.srcDir("build/generated/ksp/androidMain/kotlin")
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
                implementation("dev.atsushieno.missing-dot:missingdot:v0.1")
                if (configurations.get("ksp").dependencies.all { p -> p.name != ":kotractive_ksp" })
                    configurations.get("ksp").dependencies.add(project(":kotractive_ksp"))
            }
            // This cannot be enabled because it seems to bring in extraneous files from different targets.
            //kotlin.srcDir("build/generated/ksp/commonMain/kotlin")
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
            //kotlin.srcDir("build/generated/ksp/commonTest/kotlin")
        }
        val jvmMain by getting {
            dependencies {
                if (configurations.get("ksp").dependencies.all { p -> p.name != ":kotractive_ksp" })
                    configurations.get("ksp").dependencies.add(project(":kotractive_ksp"))
            }
            // This cannot be enabled because it seems to bring in extraneous files from different targets.
            //kotlin.srcDir("build/generated/ksp/jvmMain/kotlin")
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                if (configurations.get("ksp").dependencies.all { p -> p.name != ":kotractive_ksp" })
                    configurations.get("ksp").dependencies.add(project(":kotractive_ksp"))
            }
            // This cannot be enabled because it seems to bring in extraneous files from different targets.
            //kotlin.srcDir("build/generated/ksp/jsMain/kotlin")
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
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].assets.srcDir("src/commonMain/resources") // kind of hack...
    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(30)
    }
    buildTypes {
        val debug by getting {
            minifyEnabled(false)
        }
        val release by getting {
            minifyEnabled(false)
        }
    }
}
