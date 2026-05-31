pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }

        exclusiveContent {
            forRepository {
                maven {
                    name = "Fabric"
                    url = uri("https://maven.fabricmc.net")
                }
            }
            filter {
                includeGroupAndSubgroups("net.fabricmc")
                includeGroupAndSubgroups("fabric-loom")
            }
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "Sponge"
                    url = uri("https://repo.spongepowered.org/repository/maven-public")
                }
            }
            filter {
                includeGroupAndSubgroups("org.spongepowered")
            }
        }
    }

    plugins {
        // 26.1+ ships deobfuscated, so we use the non-obfuscated Loom plugin
        // net.fabricmc.fabric-loom (LoomNoRemapGradlePlugin), which skips mappings.
        // see https://fabricmc.net/develop/ for new versions.
        id("net.fabricmc.fabric-loom") version "1.15.4" apply false
    }
}

plugins {
    // Pre-1.0 foojay-resolver references JvmVendorSpec.IBM_SEMERU, removed in
    // Gradle 9; 1.0.0 is rebuilt for Gradle 9 and auto-provisions JDK 25.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// This should match the folder name of the project, or else IDEA may complain (see https://youtrack.jetbrains.com/issue/IDEA-317606)
rootProject.name = "JsMacrosCE"

include("fabric")
include("extension")
include("extension:graal")
include("extension:graal:js")
include("extension:graal:python")
include("extension:ruby")
