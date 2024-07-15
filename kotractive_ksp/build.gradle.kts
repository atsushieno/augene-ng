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
        classpath(kotlin("gradle-plugin", version = "1.9.0"))
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.0-1.0.11")
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
