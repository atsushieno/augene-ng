import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    id("maven-publish")
}

buildscript {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.asProvider().get()))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.ksp)
}

repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks

publishing {
    publications.register<MavenPublication>("kotractive_ksp") {
        val java by components
        from(java)
    }
}
