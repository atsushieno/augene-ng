pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "augene-ng-project"

include("kotractive_ksp")
include("kotractive")
include("augene")
include("augene-console")
//include("augene-console-native")

