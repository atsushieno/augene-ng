import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp)
    id("maven-publish")
    id("signing")
}

kotlin {
    androidTarget {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        publishLibraryVariantsGroupedByFlavor = true
        //publishLibraryVariants("debug", "release")
    }
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(IR) {
        nodejs {
            testTask(Action {
                // FIXME: we want to enable tests, but can't until this error gets fixed.
                //   :kotractive:jsNodeTest: java.lang.IllegalStateException: command '/home/atsushi/.gradle/nodejs/node-v14.15.4-linux-x64/bin/node' exited with errors (exit code: 1)
                enabled = false
                useKarma {
                    useChromeHeadless()
                    //webpackConfig.cssSupport.enabled = true
                }
            })
            useCommonJs()
        }
        browser {
            testTask(Action {
                // FIXME: we want to enable tests, but can't until this error gets fixed.
                //   :kotractive:jsNodeTest: java.lang.IllegalStateException: command '/home/atsushi/.gradle/nodejs/node-v14.15.4-linux-x64/bin/node' exited with errors (exit code: 1)
                enabled = false
            })
            useCommonJs()
        }
    }
    // e: Could not find "/media/atsushi/extssd0/sources/ktmidi/augene-ng/kotractive-project/kotractive/build/generated/ksp/nativeMain/classes" in [/media/atsushi/extssd0/sources/ktmidi/augene-ng/kotractive-project, /home/atsushi/.konan/klib, /home/atsushi/.konan/kotlin-native-prebuilt-linux-1.5.21/klib/common, /home/atsushi/.konan/kotlin-native-prebuilt-linux-1.5.21/klib/platform/linux_x64]
    /*
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native") {
            binaries {
                staticLib()
                sharedLib()
            }
        }
        hostOs == "Linux" -> linuxX64("native") {
            binaries {
                staticLib()
                sharedLib()
            }
        }
        isMingwX64 -> mingwX64("native") {
            binaries {
                staticLib()
                sharedLib()
            }
        }
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }*/

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.datetime)
                implementation(libs.missingdot)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val androidMain by getting
        /*
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }*/
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        /*
        val nativeMain by getting {
            dependencies {
            }
        }
        val nativeTest by getting
        */
    }
}

android {
    namespace = "dev.atsushieno.kotractive"
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].assets.srcDir("src/commonMain/resources") // kind of hack...
    defaultConfig {
        targetSdk = 34
        minSdk = 24
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

dependencies {
    configurations.get("kspJvm").dependencies.add(implementation(project(":kotractive_ksp")))
    configurations.get("kspJs").dependencies.add(implementation(project(":kotractive_ksp")))
    configurations.get("kspAndroid").dependencies.add(implementation(project(":kotractive_ksp")))

    /*
    if (configurations.get("kspJvm").dependencies.all { p -> p.name != "dev.atsushieno:kotractive_ksp:0.2" })
        configurations.get("kspJvm").dependencies.add(implementation("dev.atsushieno:kotractive_ksp:0.2"))
    if (configurations.get("kspJs").dependencies.all { p -> p.name != "dev.atsushieno:kotractive_ksp:0.2" })
        configurations.get("kspJs").dependencies.add(implementation("dev.atsushieno:kotractive_ksp:0.2"))
//    if (configurations.get("kspNative").dependencies.all { p -> p.name != "dev.atsushieno:kotractive_ksp:0.2" })
//        configurations.get("kspNative").dependencies.add(implementation("dev.atsushieno:kotractive_ksp:0.2"))
    if (configurations.get("kspAndroid").dependencies.all { p -> p.name != "dev.atsushieno:kotractive_ksp:0.2" })
        configurations.get("kspAndroid").dependencies.add(implementation("dev.atsushieno:kotractive_ksp:0.2"))
    */
}
