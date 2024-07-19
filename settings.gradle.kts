pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "augene-ng-project"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("kotractive_ksp")
include("kotractive")
include("augene")
include("augene-console")
include("augene-console-native")

