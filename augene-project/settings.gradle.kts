pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    
}
rootProject.name = "augene-project"

include(":midi2tracktionedit")
include(":android")
include(":desktop")
include(":common")

