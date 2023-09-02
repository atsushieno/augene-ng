buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
        classpath("com.android.tools.build:gradle:8.0.2")
    }
}

subprojects {

    group = "dev.atsushieno"
    version = "0.2"

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
