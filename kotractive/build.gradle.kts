plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp)
    id("maven-publish")
    id("signing")
}

kotlin {
    jvmToolchain(17)
    androidTarget {
        compilations.all { kotlinOptions.jvmTarget = "17" }
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("debug", "release")
    }
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "17" }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    // FIXME: we want to bring it back, but KMP fails to resolve very basic stdlib APIs such as mutableListOf().
    /*
    js {
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
    }*/

    /*
    val hostOs = System.getProperty("os.name")
    if (hostOs == "Mac OS X") {
        macosArm64()
        macosX64()
    }
    linuxArm64()
    linuxX64()
    mingwX64()
     */

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.io.core)
                implementation(libs.missingdot)
            }
            // In the latest build, we only generate ksp outputs for commonMain and add the sources here.
            // Nothing else for the actual targets.
            kotlin.srcDir(project.layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
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
                implementation(libs.junit)
            }
        }*/
        /*
        val jsMain by getting {
            /*
            dependencies {
                implementation(project(":kotractive_ksp"))
            }*/
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }*/
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
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].assets.srcDir("src/commonMain/resources") // kind of hack...
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
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

// Hush Gradle's dependency resolution nonsense.
// It is silly because Gradle still fails to resolve task dependencies and
// runs compileDebugKotlinAndroid before kspCommonMainKotlinMetadata completes.
// If it fails at that level, what's the point of requiring deps?
tasks.all {
    if (name.startsWith("ksp") && name != "ksp" && name != "kspCommonMainKotlinMetadata")
        mustRunAfter("kspCommonMainKotlinMetadata")
    if (name.startsWith("compile"))
        mustRunAfter("kspCommonMainKotlinMetadata")
}

dependencies.add("kspCommonMainMetadata", project(":kotractive_ksp"))

afterEvaluate {
    tasks.named("androidDependencies").configure { dependsOn(":kotractive:kspCommonMainKotlinMetadata") }
}
