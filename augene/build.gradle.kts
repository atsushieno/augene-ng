import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.dokka)
    id("maven-publish")
    id("signing")
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvmToolchain(21)

    /*
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                enabled = false
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport {}
                }
            }
        }
        //nodejs {}
    }*/
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("debug", "release")
    }
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    /*
    js {
        nodejs {
            testTask {
                useKarma {
                    useChromeHeadless()
                    //webpackConfig.cssSupport.enabled = true
                }
            }
            useCommonJs()
        }
        //browser() - okio FileSystem.SYSTEM is not available on browsers yet.
    }
     */

    /*
    val hostOs = System.getProperty("os.name")
    if (hostOs == "Mac OS X") {
        macosArm64()
        macosX64()
    }
    linuxArm64()
    linuxX64()
    mingwX64()*/

    sourceSets {
        val androidMain by getting
        val commonMain by getting {
            dependencies {
                implementation(libs.okio)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktmidi)
                implementation(libs.mugene)
                implementation(libs.missingdot)
                implementation(project(":kotractive"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        /*
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }*/
        /*
        val nativeMain by creating
        val nativeTest by creating
        val macosArm64Main by getting
        val macosX64Main by getting
        val linuxArm64Main by getting
        val linuxX64Main by getting
        val mingwX64Main by getting
        val wasmJsMain by getting
        val wasmJsTest by getting
         */
    }
}

android {
    namespace = "dev.atsushieno.augene"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].assets.srcDir("src/commonMain/resources") // kind of hack...
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
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
