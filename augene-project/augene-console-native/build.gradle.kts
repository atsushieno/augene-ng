plugins {
    id("org.jetbrains.kotlin.multiplatform")  version "1.6.0"
}

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native") { // on macOS
            binaries.executable {
                freeCompilerArgs += "-Xdisable-phases=EscapeAnalysis"
            }
        }
        hostOs == "Linux" ->  linuxX64("native") { // on Linux
            binaries.executable {
                freeCompilerArgs += "-Xdisable-phases=EscapeAnalysis"
            }
        }
        isMingwX64 -> mingwX64("native") { // on Windows
            binaries.executable {
                freeCompilerArgs += "-Xdisable-phases=EscapeAnalysis"
            }
        }
        else -> {}
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation(project(":augene"))
            }
        }
    }
}

tasks.withType<Wrapper> {
  gradleVersion = "7.1.1"
  distributionType = Wrapper.DistributionType.BIN
}
