plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp)
    id("maven-publish")
    id("signing")
}

kotlin {
    jvmToolchain(8)
    androidTarget {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("debug", "release")
    }
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(IR) {
        nodejs {
            testTask(Action {
                // FIXME: we want to enable tests, but can't until this error gets fixed.
                //   :kotractive:jsNodeTest: java.lang.IllegalStateException: command '/home/atsushi/.gradle/nodejs/node-v14.15.4-linux-x64/bin/node' exited with errors (exit code: 1)
                enabled = false
                useKarma {
                    useChromeHeadless()
                    //webpackConfig.cssSupport.enabled = true
                }
            })
            useCommonJs()
        }
        browser {
            testTask(Action {
                // FIXME: we want to enable tests, but can't until this error gets fixed.
                //   :kotractive:jsNodeTest: java.lang.IllegalStateException: command '/home/atsushi/.gradle/nodejs/node-v14.15.4-linux-x64/bin/node' exited with errors (exit code: 1)
                enabled = false
            })
            useCommonJs()
        }
    }

    /*
    val hostOs = System.getProperty("os.name")
    if (hostOs == "Mac OS X") {
        macosArm64()
        macosX64()
    }
    linuxArm64()
    linuxX64()
    mingwX64()
     */

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.datetime)
                implementation(libs.missingdot)
            }
//kotlin.srcDir(project.layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val androidMain by getting
        /*
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }*/
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        /*
        val nativeMain by getting {
            dependencies {
            }
        }
        val nativeTest by getting
        */
    }
}

android {
    namespace = "dev.atsushieno.kotractive"
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].assets.srcDir("src/commonMain/resources") // kind of hack...
    defaultConfig {
        targetSdk = 34
        minSdk = 24
    }
    buildTypes {
        val debug by getting {
            //minifyEnabled(false)
        }
        val release by getting {
            //minifyEnabled(false)
        }
    }
}

/*
tasks.all {
    if (name.startsWith("ksp") && name != "ksp" && name != "kspCommonMainKotlinMetadata")
        mustRunAfter("kspCommonMainKotlinMetadata")
    if (name.startsWith("compile")) {
        mustRunAfter("kspCommonMainKotlinMetadata")
        /*
        val kspName = "ksp" + name.substring("compile".length)
        if (tasks.any { it.name == kspName })
            this.dependsOn(tasks[kspName])*/
    }
}*/

dependencies {
    arrayOf("kspCommonMainMetadata", "kspJvm", "kspJs", "kspAndroid").forEach {
        add(it, project(":kotractive_ksp"))
    }
    /*
    val deps = this
    configurations.all {
        //if (name == "kspCommonMainKotlinMetadata") {
        if (name.startsWith("ksp") && name != "ksp" && !name.endsWith("Classpath")) {
            println("KSP: $name")
            deps.add(name, project(":kotractive_ksp"))
        }
        else if (name.endsWith("MainImplementation")) {
            println("MainImplementation: $name")
            //dependencies.add(ksp(project(":kotractive_ksp")))
        }
        //else println(name)
    }*/

    /*
    if (configurations.get("kspCommonMainMetadata").dependencies.all { p -> p.name != "dev.atsushieno:kotractive_ksp:0.2" })
        configurations.get("kspCommonMainMetadata").dependencies.add(implementation(project(":kotractive_ksp")))
    if (configurations.get("kspJvm").dependencies.all { p -> p.name != "dev.atsushieno:kotractive_ksp:0.2" })
        configurations.get("kspJvm").dependencies.add(implementation(project(":kotractive_ksp")))
    if (configurations.get("kspJs").dependencies.all { p -> p.name != "dev.atsushieno:kotractive_ksp:0.2" })
        configurations.get("kspJs").dependencies.add(implementation(project(":kotractive_ksp")))
//    if (configurations.get("kspNative").dependencies.all { p -> p.name != "dev.atsushieno:kotractive_ksp:0.2" })
//        configurations.get("kspNative").dependencies.add(implementation("dev.atsushieno:kotractive_ksp:0.2"))
    if (configurations.get("kspAndroid").dependencies.all { p -> p.name != "dev.atsushieno:kotractive_ksp:0.2" })
        configurations.get("kspAndroid").dependencies.add(implementation(project(":kotractive_ksp")))
    */
}
