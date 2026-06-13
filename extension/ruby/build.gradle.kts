// Ruby ships as its own companion Fabric mod (id jsmacros-ruby) rather than a
// bundled-into-the-base-jar extension. It is discovered at runtime via the
// "jsmacros" Fabric entrypoint (see ExtensionLoader). JRuby is embedded via
// Fabric jar-in-jar (META-INF/jars + a "jars" entry in fabric.mod.json).

val jrubyVersion = "9.4.5.0"

version = rootProject.version
group = rootProject.group

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/")
    maven("https://maven.fabricmc.net/")
}

// JRuby runtime, nested into the mod jar via Fabric jar-in-jar.
val jrubyJar by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    // jsmacros API — provided by the base mod at runtime, so compile-only.
    compileOnly(rootProject.sourceSets["core"].output)
    compileOnly(rootProject.sourceSets["main"].output)
    compileOnly(rootProject.sourceSets["client"].output)
    compileOnly(rootProject.libs.fabric.loader)
    compileOnly("org.jetbrains:annotations:20.1.0")
    for (dependency in rootProject.configurations["minecraftLibraries"].dependencies) {
        compileOnly(dependency)
    }
    // provided by the base mod at runtime (guava, gson, etc.)
    for (dependency in rootProject.configurations.implementation.get().dependencies) {
        compileOnly(dependency)
    }

    // JRuby — compile against it, and nest it into the mod jar.
    compileOnly("org.jruby:jruby-complete:$jrubyVersion")
    jrubyJar("org.jruby:jruby-complete:$jrubyVersion")
}

base {
    archivesName = "${rootProject.property("archives_base_name")}-ruby"
}

tasks.processResources {
    val jrubyJarName = "jruby-complete-$jrubyVersion.jar"
    inputs.property("version", project.version)
    inputs.property("jrubyJar", jrubyJarName)
    inputs.property("minecraftVersion", rootProject.libs.versions.minecraft.get())

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "jruby_jar" to jrubyJarName,
            "minecraft_version" to rootProject.libs.versions.minecraft.get()
        )
    }
}

// Fabric Loader 0.19+ only adds a nested jar-in-jar to the runtime classpath if
// that nested jar contains its own fabric.mod.json. The stock jruby-complete jar
// has none, so we repackage it with a generated fabric.mod.json before nesting.
val jrubyModId = "org_jruby_jruby_complete"

val jrubyFmjDir = layout.buildDirectory.dir("jruby-fmj")
val writeJrubyFmj by tasks.registering {
    val out = jrubyFmjDir.get().file("fabric.mod.json").asFile
    outputs.file(out)
    doLast {
        out.parentFile.mkdirs()
        out.writeText(
            """
            {
              "schemaVersion": 1,
              "id": "$jrubyModId",
              "version": "$jrubyVersion",
              "name": "JRuby (bundled)",
              "environment": "*"
            }
            """.trimIndent()
        )
    }
}

val nestableJruby by tasks.registering(Jar::class) {
    dependsOn(jrubyJar, writeJrubyFmj)
    archiveFileName.set("jruby-complete-$jrubyVersion.jar")
    destinationDirectory.set(layout.buildDirectory.dir("nestable-jruby"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(zipTree(jrubyJar.singleFile))
    from(jrubyFmjDir)
}

tasks.jar {
    dependsOn(nestableJruby)
    from(nestableJruby) {
        into("META-INF/jars")
    }

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
