import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
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
    }*/

    /*
    val hostOs = System.getProperty("os.name")
    if (hostOs == "Mac OS X") {
        macosArm64()
        macosX64()
    }
    //linuxArm64()
    //linuxX64()
    mingwX64()
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
        val jsMain by getting {
            dependencies {
                implementation(libs.okio.nodefilesystem)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
         */
        //val nativeMain by getting
        //val nativeTest by getting
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
