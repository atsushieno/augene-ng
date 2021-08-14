buildscript {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}


/*
// This is a workaround for https://youtrack.jetbrains.com/issue/KT-44884
configurations.matching { it.name != "kotlinCompilerPluginClasspath" }.all {
    resolutionStrategy.eachDependency {
        version = requested.version
        if (requested.group == "org.jetbrains.kotlinx" &&
            requested.name.startsWith("kotlinx-coroutines") &&
            version != null && !version.contains("native-mt")
        ) {
            useVersion("$version-native-mt")
        }
    }
}
*/

allprojects {
    group = "dev.atsushieno"
    version = "0.1"

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}