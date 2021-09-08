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
    id("com.google.devtools.ksp") version "1.5.30-1.0.0"
    id("org.jetbrains.kotlin.multiplatform") version "1.5.30"
    id("org.jetbrains.dokka") version "1.5.0"
    id("maven-publish")
    id("signing")
}

kotlin {
    android {
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
    // FIXME: we want to enable BOTH, but can't until this error gets fixed.
    // > Failed to calculate the value of task ':kotractive:compileTestDevelopmentExecutableKotlinJsIr' property 'entryModule$kotlin_gradle_plugin'.
    //   > Collection has more than one element.
    js(LEGACY) {
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
        browser {
            testTask {
                // FIXME: we want to enable tests, but can't until this error gets fixed.
                //   :kotractive:jsNodeTest: java.lang.IllegalStateException: command '/home/atsushi/.gradle/nodejs/node-v14.15.4-linux-x64/bin/node' exited with errors (exit code: 1)
                enabled = false
            }
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
        val androidMain by getting {
            dependencies {
            }
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
                implementation("dev.atsushieno:missingdot:0.1.4")
                if (configurations.get("ksp").dependencies.all { p -> p.name != ":kotractive_ksp" })
                    configurations.get("ksp").dependencies.add(project(":kotractive_ksp"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
            }
        }
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
