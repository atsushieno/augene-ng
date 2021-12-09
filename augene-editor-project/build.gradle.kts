buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath("org.jetbrains.compose:compose-gradle-plugin:1.0.0")
    }
}

subprojects {

    group = "dev.atsushieno"
    version = "0.1"

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven { url = uri("https://jitpack.io") }
    }
}