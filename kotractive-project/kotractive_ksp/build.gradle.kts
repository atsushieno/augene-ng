import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id ("org.jetbrains.kotlin.jvm") version "1.5.21"
}

buildscript {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.5.21"))
    }
}

/*
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}*/

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:+")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.21-1.0.0-beta07")
}

repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
