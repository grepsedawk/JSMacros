import org.gradle.language.jvm.tasks.ProcessResources
import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    kotlin("jvm") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    // 26.1+ ships deobfuscated; the non-obfuscated Loom plugin
    // (LoomNoRemapGradlePlugin) skips the mappings layer and the remap step.
    id("net.fabricmc.fabric-loom")
    id("multiloader-loader")
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
}

val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.prop("minecraft_version")
var mod_version = project.version.toString()

val loom = extensions.getByType(LoomGradleExtensionAPI::class.java)

base {
    archivesName.set("$mod_id-$minecraft_version-fabric-$mod_version")
}

// Configuration for embedding extension jars
val extensionJars by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

// Gradle is stupid and will throw a `Type mismatch: inferred type is Dependency? but Any was expected` otherwise
fun DependencyHandlerScope.implInclude(notation: Any) {
    val dep = requireNotNull(add("include", notation))
    add("implementation", dep)
}

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraft_version")

    val fabric_loader_version = commonMod.prop("fabric_loader_version")
    val fabric_version = commonMod.prop("fabric_version")
    val mod_menu_version = commonMod.prop("mod_menu_version")

    implementation("net.fabricmc:fabric-loader:$fabric_loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    implementation("com.terraformersmc:modmenu:$mod_menu_version")

    // Mixin processing for the merged-in common mixins (Loom provides mixin at
    // runtime, but the annotation processor and asm-tree are needed at compile).
    compileOnly("org.spongepowered:mixin:0.8.5")
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")
    compileOnly("org.ow2.asm:asm-tree:9.6")

    // Common library dependencies - include for bundling in jar
    implInclude("io.noties:prism4j:2.0.0")
    implInclude("org.jooq:joor:0.9.15")
    implInclude("com.neovisionaries:nv-websocket-client:2.14")
    implInclude("org.javassist:javassist:3.30.2-GA")

    // Extension jars to embed
    add(extensionJars.name, project(mapOf("path" to ":extension:graal", "configuration" to "archives")))
    add(extensionJars.name, project(mapOf("path" to ":extension:graal:js", "configuration" to "archives")))
}

// Collect extension jar names for dependencies property
fun getExtensionJarPaths(): String =
    extensionJars.files.joinToString(", ") { file ->
        "\"META-INF/jsmacroscedeps/${file.name}\""
    }

tasks.named<ProcessResources>("processResources") {
    // Embed extension jars into the final jar
    dependsOn(extensionJars)
    from(extensionJars) {
        into("META-INF/jsmacroscedeps")
    }

    // Add dependencies expansion for jsmacros.extension.json
    filesMatching("jsmacrosce.extension.json") {
        expand(mapOf("dependencies" to getExtensionJarPaths()))
    }

    // Expand fabric.mod.json5 with minecraft version
    filesMatching("fabric.mod.json5") {
        expand(
            mapOf(
                "version" to mod_version,
                "minecraft_version" to minecraft_version
            )
        )
    }
}

loom.apply {
    accessWidenerPath.set(file("src/main/resources/accesswideners/$mod_id.accesswidener"))

    mixin(Action {
        defaultRefmapName.set("$mod_id.refmap.json")
    })
}

fletchingTable {
    fabric {
        applyMixinConfig = false
    }
    mixins.register(sourceSets.main) {
        mixin("default", "jsmacrosce-common.mixins.json5") {
            env("CLIENT")
        }
        mixin("fabric", "jsmacrosce-fabric.mixins.json5") {
            env("CLIENT")
        }
    }
    j52j.register(sourceSets.main) {
        extension(
            "json",
            "fabric.mod.json5",
            "jsmacrosce-common.mixins.json5",
            "jsmacrosce-fabric.mixins.json5"
        )
    }
}
