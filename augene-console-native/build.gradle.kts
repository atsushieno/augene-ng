@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
        java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    /* TODO
    js(BOTH) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
        nodejs {
        }
    }*/
    /*
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    if (hostOs == "Mac OS X") {
        macosArm64("macosArm64") { binaries { executable { entryPoint = "main" } } }
        macosX64("macosX64") { binaries { executable { entryPoint = "main" } } }
    }
    // I figured Kotlin-Native is not ready enough for linking third-party libraries.
    // FIXME: revisit it when this issue got resolved https://youtrack.jetbrains.com/issue/KT-47061/Cant-compile-project-with-OpenAL-dependecy-Kotlin-Native#focus=Comments-27-4947040.0-0
    //linuxArm64("linuxX64") { binaries.executable.entryPoint = "main" }
    //linuxX64("linuxArm64") { binaries.executable.entryPoint = "main" }
    mingwX64("native") { binaries { executable { entryPoint = "main" } } }
     */

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":augene"))
            }
        }
    }
}
/*
tasks.withType<Wrapper> {
  gradleVersion = "7.1.1"
  distributionType = Wrapper.DistributionType.BIN
}
*/