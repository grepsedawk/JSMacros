import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.PublishModTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import xyz.wagyourtail.unimined.internal.minecraft.task.RemapJarTaskImpl

plugins {
    id("xyz.wagyourtail.unimined")
    alias(libs.plugins.shadow)
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
}

val archives_base_name: String by project.properties
val mod_version: String by project.properties
val maven_group: String by project.properties

base {
    archivesName.set(archives_base_name)
}

// Release builds get the full version from the release tag (mod_version); dev builds
// get a -dev-<sha> suffix. channel selects the Modrinth release type.
val channel: String = (findProperty("channel") as String?) ?: "release"
val buildSha: String = ((findProperty("build_sha") as String?)
    ?: System.getenv("GITHUB_SHA") ?: "local").take(7)
val computedVersion: String = when (channel) {
    "dev" -> "$mod_version-dev-$buildSha"
    else -> mod_version
}

version = computedVersion
group = maven_group

tasks.register("printVersion") {
    group = "distribution"
    description = "Prints the computed project version for CI workflows"
    doLast {
        println(project.version)
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get().toInt())
    targetCompatibility = JavaVersion.toVersion(libs.versions.java.get().toInt())

    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://files.minecraftforge.net/maven/")
    maven("https://jitpack.io")
    mavenCentral()
}

val core by sourceSets.creating {
    compileClasspath += configurations.implementation.get()
    runtimeClasspath += configurations.implementation.get()
}

val client by sourceSets.creating {
    compileClasspath += core.output + core.compileClasspath + sourceSets.main.get().output
    runtimeClasspath += core.output + core.runtimeClasspath + sourceSets.main.get().output
}

val fabric by sourceSets.creating {
    compileClasspath += core.output + core.compileClasspath + sourceSets.main.get().output + client.output
    runtimeClasspath += core.output + core.runtimeClasspath + sourceSets.main.get().output + client.output
}

sourceSets.main {
    compileClasspath += core.output + core.compileClasspath
    runtimeClasspath += core.output + core.runtimeClasspath
}

unimined.minecraft {
    version(libs.versions.minecraft.get())
    side("server")

    mappings {
        mojmap()
    }

    accessWidener {
        accessWidener(file("src/main/resources/jsmacros.accesswidener"))
    }
    if (sourceSet == sourceSets.main.get() || sourceSet == client) {
        defaultRemapJar = false
        runs.off = true
    }
}

unimined.minecraft(client) {
    combineWith(":core")
    combineWith(":main")
    side("joined")
}

unimined.minecraft(fabric) {
    combineWith(":main")
    side("joined")

    fabric {
        loader(libs.versions.fabric.loader.get())
        accessWidener(file("src/main/resources/jsmacros.accesswidener"))
    }
}


configurations.implementation.configure {
    isCanBeResolved = true
}

val minecraftLibraries by configurations.getting
val jsmacrosExtensionInclude by configurations.creating

val clientCompileOnly by configurations.getting {
    extendsFrom(configurations.compileOnly.get())
}

core.apply {
    compileClasspath += minecraftLibraries
    runtimeClasspath += minecraftLibraries
}

dependencies {
    val coreImplementation by configurations.getting
    val coreCompileOnly by configurations.getting
    val fabricModImplementation by configurations.getting
    val fabricInclude by configurations.getting
    val fabricRuntimeOnly by configurations.getting

    // ExtensionLoader discovers companion-mod extensions via the Fabric entrypoint API.
    // FabricLoader is present at runtime (single fabric mod jar); core only needs it to compile.
    coreCompileOnly(libs.fabric.loader)

    compileOnly(libs.mixin)
    compileOnly(libs.mixin.extra)
    implementation(libs.asm)

    implementation(libs.prism4j)
    coreImplementation(libs.joor)
    coreImplementation(libs.nv.websocket)
    coreImplementation(libs.javassist)

    jsmacrosExtensionInclude(project(":extension:graal")) { isTransitive = false }
    jsmacrosExtensionInclude(project(":extension:graal:js")) { isTransitive = false }

    fabricModImplementation(fabricApi.fabricModule("fabric-api-base", libs.versions.fapi.get()))
    fabricModImplementation(fabricApi.fabricModule("fabric-lifecycle-events-v1", libs.versions.fapi.get()))
    fabricModImplementation(fabricApi.fabricModule("fabric-key-mapping-api-v1", libs.versions.fapi.get()))
    fabricModImplementation(fabricApi.fabricModule("fabric-resource-loader-v1", libs.versions.fapi.get()))
    fabricModImplementation(fabricApi.fabricModule("fabric-command-api-v2", libs.versions.fapi.get()))
    fabricModImplementation(fabricApi.fabricModule("fabric-rendering-v1", libs.versions.fapi.get()))

    fabricModImplementation(libs.modmenu)
    fabricModImplementation(fabricApi.fabricModule("fabric-screen-api-v1", libs.versions.fapi.get()))

    fabricInclude(fabricApi.fabricModule("fabric-api-base", libs.versions.fapi.get()))
    fabricInclude(fabricApi.fabricModule("fabric-rendering-v1", libs.versions.fapi.get()))
    fabricInclude(fabricApi.fabricModule("fabric-lifecycle-events-v1", libs.versions.fapi.get()))
    fabricInclude(fabricApi.fabricModule("fabric-key-mapping-api-v1", libs.versions.fapi.get()))
    fabricInclude(fabricApi.fabricModule("fabric-resource-loader-v1", libs.versions.fapi.get()))
    fabricInclude(fabricApi.fabricModule("fabric-command-api-v2", libs.versions.fapi.get()))

    fabricInclude(libs.prism4j)
    fabricInclude(libs.nv.websocket)
    fabricInclude(libs.javassist)
    fabricInclude(libs.joor)



    for (file in file("extension").listFiles() ?: emptyArray()) {
        if (!file.isDirectory || file.name in listOf("build", "src", ".gradle", "gradle")) continue

        fabricRuntimeOnly(project(":extension:${file.name}"))

        if (file.resolve("subprojects.txt").exists()) {
            for (subproject in file.resolve("subprojects.txt").readLines()) {
                fabricRuntimeOnly(project(":extension:${file.name}:$subproject"))
            }
        }
    }
}

val removeDist by tasks.registering(Delete::class) {
    delete(File(rootProject.rootDir, "dist"))
}

tasks.clean.configure {
    finalizedBy(removeDist)
}

val processCoreResources by tasks.getting(ProcessResources::class) {
    inputs.property("dependencies", jsmacrosExtensionInclude.files)

    filesMatching("jsmacros.extension.json") {
        expand("dependencies" to jsmacrosExtensionInclude.files.map { "\"META-INF/jsmacrosdeps/${it.name}\"" }.joinToString(", "))
    }
}

tasks.jar {
    enabled = false
}

val processFabricResources by tasks.getting(ProcessResources::class) {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }

}

val fabricJar by tasks.getting(Zip::class) {
    dependsOn(":extension:graal:jar")
    dependsOn(":extension:graal:js:jar")
    from(fabric.output, sourceSets.main.get().output, core.output, client.output)

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(jsmacrosExtensionInclude.files) {
        include("*")
        into("META-INF/jsmacrosdeps")
    }
}

val remapFabricJar by tasks.getting(RemapJarTaskImpl::class) {

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val generatePyDoc by tasks.registering(Javadoc::class) {
    group = "documentation"
    description = "Generates the python documentation for the project"

    source = sourceSets.main.get().allJava + core.allJava
    setDestinationDir(File(rootProject.layout.buildDirectory.get().asFile, "docs/python/JsMacrosAC/"))
    options.doclet = "xyz.wagyourtail.doclet.pydoclet.Main"
    options.docletpath(File(rootProject.rootDir, "buildSrc/build/libs/buildSrc.jar"))
    (options as CoreJavadocOptions).addStringOption("v", mod_version)

    doFirst {
        classpath = sourceSets.main.get().compileClasspath + core.compileClasspath
    }
}

val copyPyDoc by tasks.registering(Copy::class) {
    group = "documentation"
    dependsOn(generatePyDoc)

    description = "Copies the python documentation to the build folder"
    from(File(rootProject.rootDir, "docs/python"))
    into(File(rootProject.layout.buildDirectory.get().asFile, "docs/python"))
}

val generateTSDoc by tasks.registering(Javadoc::class) {
    group = "documentation"
    description = "Generates the typescript documentation for the project"

    source = sourceSets.main.get().allJava + core.allJava
    doFirst {
        classpath = sourceSets.main.get().compileClasspath + core.compileClasspath
    }
    setDestinationDir(File(rootProject.layout.buildDirectory.get().asFile, "docs/typescript/headers/"))
    options.doclet = "xyz.wagyourtail.doclet.tsdoclet.Main"
    options.docletpath(File(rootProject.rootDir, "buildSrc/build/libs/buildSrc.jar"))
    (options as CoreJavadocOptions).addStringOption("v", mod_version)
}

val copyTSDoc by tasks.registering(Copy::class) {
    group = "documentation"
    description = "Copies the typescript files to the build folder"
    dependsOn(generateTSDoc)

    from(File(rootProject.rootDir, "docs/typescript"))
    into(File(rootProject.layout.buildDirectory.get().asFile, "docs/typescript"))
}

val generateWebDoc by tasks.registering(Javadoc::class) {
    group = "documentation"
    description = "Generates the web documentation for the project"

    source = sourceSets.main.get().allJava + core.allJava
    setDestinationDir(File(rootProject.layout.buildDirectory.get().asFile, "docs/web/"))
    options.doclet = "xyz.wagyourtail.doclet.webdoclet.Main"
    options.docletpath(File(rootProject.rootDir, "buildSrc/build/libs/buildSrc.jar"))
    (options as CoreJavadocOptions).addStringOption("v", mod_version)
    (options as CoreJavadocOptions).addStringOption("mcv", libs.versions.minecraft.get())
    (options as StandardJavadocDocletOptions).links("https://docs.oracle.com/javase/8/docs/api/", "https://www.javadoc.io/doc/org.slf4j/slf4j-api/1.7.30/", "https://javadoc.io/doc/com.neovisionaries/nv-websocket-client/latest/")

    doFirst {
        classpath = sourceSets.main.get().compileClasspath + core.compileClasspath
    }
}

val copyWebDoc by tasks.registering(Copy::class) {
    group = "documentation"
    description = "Copies the web documentation to the build folder"
    dependsOn(generateWebDoc)

    from(File(rootProject.rootDir, "docs/web"))
    into(File(rootProject.layout.buildDirectory.get().asFile, "docs/web"))

    inputs.property("version", project.version)

    filesMatching("index.html") {
        expand("version" to project.version)
    }
}

val createDist by tasks.registering(Copy::class) {
    group = "build"
    description = "Creates all files for the distribution of the project"
    dependsOn(copyPyDoc, copyTSDoc, copyWebDoc)
    // build/libs is copied as a raw dir, so its producers must be explicit deps (Gradle 8.14 fails the build otherwise on a clean run).
    dependsOn(fabricJar, remapFabricJar)

    from(File(rootProject.layout.buildDirectory.get().asFile, "docs"))
    from(File(rootProject.layout.buildDirectory.get().asFile, "libs"))
    from(project(":extension:graal:python").tasks.jar.get().outputs)
    from(project(":extension:ruby").tasks.jar.get().outputs)
    into(File(rootProject.rootDir, "dist"))
}

tasks.build.configure {
    finalizedBy(createDist)
}

val releaseType = when (channel) {
    "release" -> ReleaseType.STABLE
    "beta" -> ReleaseType.BETA
    else -> ReleaseType.ALPHA
}

val mcVersion = libs.versions.minecraft.get()

val modrinthProjectId = providers.gradleProperty("modrinth_id")
    .orElse(providers.environmentVariable("MODRINTH_PROJECT"))
val rubyModrinthProjectId = providers.gradleProperty("modrinth_ruby_id")
    .orElse(providers.environmentVariable("MODRINTH_RUBY_PROJECT"))
val modrinthToken = providers.gradleProperty("modrinth_token")
    .orElse(providers.environmentVariable("MODRINTH_TOKEN"))

fun modrinthChangelog(): String = """
    JsMacros Reloaded ${project.version} for fabric on Minecraft $mcVersion.

    The first release of JsMacros Reloaded — a clean re-fork built directly on the original JsMacros, updated for Minecraft $mcVersion (fabric-only).

    Thanks to WagYourTail for the original JsMacros, and to Pablete1234 for the Minecraft $mcVersion port work.
""".trimIndent()

fun rubyChangelog(): String = """
    JsMacros Ruby ${project.version} for fabric on Minecraft $mcVersion.
    Requires JsMacros Reloaded.
""".trimIndent()

publishMods {
    val publishModrinth = modrinthToken.isPresent && channel != "dev"

    if (publishModrinth) {
        modrinth("modrinthFabric") {
            projectId.set(modrinthProjectId)
            accessToken.set(modrinthToken)
            minecraftVersions.add(mcVersion)
            modLoaders.set(listOf("fabric"))

            version.set("${project.version}+$mcVersion-fabric")
            displayName.set("JsMacros Reloaded ${project.version} (fabric $mcVersion)")
            changelog.set(modrinthChangelog())
            type.set(releaseType)
            file.set(
                tasks.named("remapFabricJar", AbstractArchiveTask::class.java)
                    .flatMap { it.archiveFile }
            )
        }
    }

    val publishRubyModrinth = modrinthToken.isPresent && rubyModrinthProjectId.isPresent && channel != "dev"
    if (publishRubyModrinth) {
        modrinth("modrinthRuby") {
            projectId.set(rubyModrinthProjectId)
            accessToken.set(modrinthToken)
            minecraftVersions.add(mcVersion)
            modLoaders.set(listOf("fabric"))

            version.set("${project.version}+$mcVersion-fabric")
            displayName.set("JsMacros Ruby ${project.version} (fabric $mcVersion)")
            changelog.set(rubyChangelog())
            type.set(releaseType)
            file.set(
                project(":extension:ruby").tasks.named("jar", AbstractArchiveTask::class.java)
                    .flatMap { it.archiveFile }
            )
            requires { slug.set("jsmacros-reloaded") }
        }
    }
}

tasks.named("publishMods") {
    dependsOn("createDist")
}

tasks.withType(PublishModTask::class.java).configureEach {
    dependsOn("createDist")
}