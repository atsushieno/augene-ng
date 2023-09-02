import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id ("org.jetbrains.kotlin.jvm") version "1.9.0"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
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
