buildscript {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.gradleJavacppPlatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

subprojects {
    group = "dev.atsushieno"
    version = "0.2"

    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}