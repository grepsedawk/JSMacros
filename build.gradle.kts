import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.PublishModTask
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import xyz.wagyourtail.unimined.internal.minecraft.task.RemapJarTaskImpl
import java.util.Properties
import java.util.zip.ZipFile

plugins {
    id("xyz.wagyourtail.unimined")
    alias(libs.plugins.shadow)
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
}

val mod_version: String by project.properties
val maven_group: String by project.properties

val mcProfile = providers.gradleProperty("mcProfile").orElse("26.3").get()
val supportedProfiles = setOf("26.1", "26.2", "26.3")
require(mcProfile in supportedProfiles) {
    "mcProfile must be one of ${supportedProfiles.sorted().joinToString(", ")}; got '$mcProfile'."
}

val profileProperties = Properties().apply {
    val profileFile = file("versions/$mcProfile.properties")
    require(profileFile.isFile) { "Missing Minecraft profile: ${profileFile.path}" }
    profileFile.inputStream().use(::load)
}

fun profileProperty(name: String): String = profileProperties.getProperty(name)
    ?: throw GradleException("versions/$mcProfile.properties is missing '$name'.")

val minecraftVersion = profileProperty("minecraft_version")
val minecraftVersions = profileProperty("minecraft_versions").split(',').map(String::trim).filter(String::isNotEmpty)
require(minecraftVersion in minecraftVersions) {
    "versions/$mcProfile.properties must include minecraft_version in minecraft_versions."
}
val smokeMinecraftVersion = providers.gradleProperty("mcVersion").orElse(minecraftVersion).get()
require(smokeMinecraftVersion in minecraftVersions) {
    "mcVersion '$smokeMinecraftVersion' is not supported by profile $mcProfile (${minecraftVersions.joinToString(", ")})."
}
val fabricLoaderVersion = profileProperty("fabric_loader_version")
val fabricApiVersion = profileProperty("fabric_api_version")
val modMenuVersion = profileProperty("modmenu_version")
val profileArchiveName = "jsmacros-$mcProfile"
val profileAccessWidener = file("src/versions/$mcProfile/main/resources/jsmacros.accesswidener")
    .takeIf(File::isFile)
    ?: file("src/main/resources/jsmacros.accesswidener")

layout.buildDirectory.set(layout.projectDirectory.dir("build/$mcProfile"))
extra["profileFabricLoaderVersion"] = fabricLoaderVersion

subprojects {
    val projectPath = path.removePrefix(":").replace(':', '/')
    layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("projects/$projectPath"))
}

base {
    archivesName.set(profileArchiveName)
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

tasks.register("printMcProfile") {
    group = "help"
    description = "Prints the selected Minecraft profile and its runtime smoke version"
    doLast {
        println("profile=$mcProfile")
        println("baselineMinecraftVersion=$minecraftVersion")
        println("smokeMinecraftVersion=$smokeMinecraftVersion")
        println("minecraftVersions=${minecraftVersions.joinToString(",")}")
        println("fabricLoaderVersion=$fabricLoaderVersion")
        println("fabricApiVersion=$fabricApiVersion")
        println("modMenuVersion=$modMenuVersion")
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

val profileSourceRoot = layout.buildDirectory.dir("sources")
val profileSourceRootFile = profileSourceRoot.get().asFile
val profileExclusions = file("src/versions/$mcProfile/excluded-paths.txt")
    .takeIf(File::isFile)
    ?.readLines()
    ?.map(String::trim)
    ?.filter { it.isNotEmpty() && !it.startsWith("#") }
    ?.toSet()
    ?: emptySet()
val allSourceSets = listOf(sourceSets.main.get(), core, client, fabric)

val prepareProfileSources by tasks.registering {
    group = "build setup"
    description = "Stages base sources with the $mcProfile compatibility overlay"
    inputs.dir("src")
    inputs.files(profileExclusions.map(::file))
    outputs.dir(profileSourceRoot)

    doLast {
        val stagedRoot = profileSourceRootFile
        delete(stagedRoot)
        for (sourceSet in allSourceSets) {
            for (kind in listOf("java", "resources")) {
                val sourceRoot = file("src/${sourceSet.name}/$kind")
                val destination = stagedRoot.resolve("${sourceSet.name}/$kind")
                val overlayRoot = file("src/versions/$mcProfile/${sourceSet.name}/$kind")
                val exclusions = profileExclusions
                    .mapNotNull { path ->
                        val prefix = "src/${sourceSet.name}/$kind/"
                        path.removePrefix(prefix).takeIf { path.startsWith(prefix) }
                    }

                if (sourceRoot.isDirectory) {
                    copy {
                        from(sourceRoot)
                        into(destination)
                        exclude(exclusions)
                    }
                }
                if (overlayRoot.isDirectory) {
                    copy {
                        from(overlayRoot)
                        into(destination)
                    }
                }
            }
        }
    }
}

for (sourceSet in allSourceSets) {
    sourceSet.java.setSrcDirs(listOf(profileSourceRootFile.resolve("${sourceSet.name}/java")))
    val resourceRoot = file("src/${sourceSet.name}/resources")
    val resourcePrefix = "src/${sourceSet.name}/resources/"
    val resourceOverlay = file("src/versions/$mcProfile/${sourceSet.name}/resources")
    val replacedResources = resourceOverlay.takeIf(File::isDirectory)
        ?.walkTopDown()
        ?.filter(File::isFile)
        ?.map { it.relativeTo(resourceOverlay).invariantSeparatorsPath }
        ?.toList()
        ?: emptyList()
    val excludedResources = profileExclusions
        .mapNotNull { path -> path.removePrefix(resourcePrefix).takeIf { path.startsWith(resourcePrefix) } }
        .plus(replacedResources)

    sourceSet.resources.setSrcDirs(listOf(resourceRoot))
    sourceSet.resources.exclude(excludedResources)
    val processResourcesTask = if (sourceSet.name == "main") {
        "processResources"
    } else {
        "process${sourceSet.name.replaceFirstChar(Char::uppercase)}Resources"
    }
    tasks.named(processResourcesTask, ProcessResources::class) {
        from(resourceOverlay)
    }
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(prepareProfileSources)
}
tasks.withType<Javadoc>().configureEach {
    dependsOn(prepareProfileSources)
}
tasks.withType<ProcessResources>().configureEach {
    dependsOn(prepareProfileSources)
}
tasks.withType<JavaExec>().configureEach {
    workingDir(file("run/$mcProfile"))
}

sourceSets.main {
    compileClasspath += core.output + core.compileClasspath
    runtimeClasspath += core.output + core.runtimeClasspath
}

unimined.minecraft {
    version(minecraftVersion)
    side("server")

    mappings {
        mojmap()
    }

    accessWidener {
        accessWidener(profileAccessWidener)
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
        loader(fabricLoaderVersion)
        accessWidener(profileAccessWidener)
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
    coreCompileOnly("net.fabricmc:fabric-loader:$fabricLoaderVersion")

    compileOnly(libs.mixin)
    compileOnly(libs.mixin.extra)
    implementation(libs.asm)

    implementation(libs.prism4j)
    coreImplementation(libs.joor)
    coreImplementation(libs.nv.websocket)
    coreImplementation(libs.javassist)

    jsmacrosExtensionInclude(project(":extension:graal")) { isTransitive = false }
    jsmacrosExtensionInclude(project(":extension:graal:js")) { isTransitive = false }

    fabricModImplementation(fabricApi.fabricModule("fabric-api-base", fabricApiVersion))
    fabricModImplementation(fabricApi.fabricModule("fabric-lifecycle-events-v1", fabricApiVersion))
    fabricModImplementation(fabricApi.fabricModule("fabric-key-mapping-api-v1", fabricApiVersion))
    fabricModImplementation(fabricApi.fabricModule("fabric-resource-loader-v1", fabricApiVersion))
    fabricModImplementation(fabricApi.fabricModule("fabric-command-api-v2", fabricApiVersion))
    fabricModImplementation(fabricApi.fabricModule("fabric-rendering-v1", fabricApiVersion))

    fabricModImplementation("com.terraformersmc:modmenu:$modMenuVersion")
    fabricModImplementation(fabricApi.fabricModule("fabric-screen-api-v1", fabricApiVersion))

    fabricInclude(fabricApi.fabricModule("fabric-api-base", fabricApiVersion))
    fabricInclude(fabricApi.fabricModule("fabric-rendering-v1", fabricApiVersion))
    fabricInclude(fabricApi.fabricModule("fabric-lifecycle-events-v1", fabricApiVersion))
    fabricInclude(fabricApi.fabricModule("fabric-key-mapping-api-v1", fabricApiVersion))
    fabricInclude(fabricApi.fabricModule("fabric-resource-loader-v1", fabricApiVersion))
    fabricInclude(fabricApi.fabricModule("fabric-command-api-v2", fabricApiVersion))

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
    delete(File(rootProject.rootDir, "dist/$mcProfile"))
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
    inputs.property("minecraftVersion", minecraftVersion)
    inputs.property("minecraftVersions", minecraftVersions)
    inputs.property("fabricLoaderVersion", fabricLoaderVersion)
    val metadataMinecraftVersions = minecraftVersions.joinToString(", ", prefix = "[", postfix = "]", transform = { "\"$it\"" })

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to minecraftVersion,
            "minecraft_versions" to metadataMinecraftVersions,
            "fabric_loader_version" to fabricLoaderVersion,
        )
    }

    doLast {
        val metadata = destinationDir.resolve("fabric.mod.json")
        val baselineMinecraftMetadata = "\"minecraft\": [\"$minecraftVersion\"]"
        check(metadata.isFile) { "Missing generated Fabric metadata: ${metadata.path}" }
        check(baselineMinecraftMetadata in metadata.readText()) {
            "Generated Fabric metadata does not contain the baseline Minecraft version."
        }
        metadata.writeText(metadata.readText().replace(baselineMinecraftMetadata, "\"minecraft\": $metadataMinecraftVersions"))
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
    (options as CoreJavadocOptions).addStringOption("mcv", minecraftVersion)
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

val createDist by tasks.registering(Sync::class) {
    group = "build"
    description = "Creates all files for the distribution of the project"
    dependsOn(copyPyDoc, copyTSDoc, copyWebDoc)
    // Distribution contains the remapped mod, never the intermediate development JAR.
    dependsOn(fabricJar, remapFabricJar)

    from(File(rootProject.layout.buildDirectory.get().asFile, "docs"))
    from(remapFabricJar.archiveFile)
    from(project(":extension:graal:python").tasks.jar.get().outputs)
    into(File(rootProject.rootDir, "dist/$mcProfile"))
}

tasks.build.configure {
    finalizedBy(createDist)
}

val releaseType = when (channel) {
    "release" -> ReleaseType.STABLE
    "beta" -> ReleaseType.BETA
    else -> ReleaseType.ALPHA
}

val mcVersion = minecraftVersion

val modrinthProjectId = providers.gradleProperty("modrinth_id")
    .orElse(providers.environmentVariable("MODRINTH_PROJECT"))
val modrinthToken = providers.gradleProperty("modrinth_token")
    .orElse(providers.environmentVariable("MODRINTH_TOKEN"))

fun modrinthChangelog(): String = """
    JsMacros Reloaded ${project.version} for fabric on Minecraft $mcVersion.

    Full changelog: https://github.com/grepsedawk/JsMacros/releases/tag/v${project.version}
""".trimIndent()

publishMods {
    val publishModrinth = modrinthToken.isPresent && channel != "dev"

    if (publishModrinth) {
        modrinth("modrinthFabric") {
            projectId.set(modrinthProjectId)
            accessToken.set(modrinthToken)
            minecraftVersions.addAll(minecraftVersions)
            modLoaders.set(listOf("fabric"))

            version.set("${project.version}+$mcProfile-fabric")
            displayName.set("JsMacros Reloaded ${project.version} (fabric $mcProfile)")
            changelog.set(modrinthChangelog())
            type.set(releaseType)
            file.set(
                tasks.named("remapFabricJar", AbstractArchiveTask::class.java)
                    .flatMap { it.archiveFile }
            )
        }
    }
}

tasks.named("publishMods") {
    dependsOn("createDist")
}

val checkProfileArtifacts by tasks.registering {
    group = "verification"
    description = "Verifies the $mcProfile artifact and generated Fabric metadata"
    dependsOn(createDist, processFabricResources)

    doLast {
        val artifact = remapFabricJar.archiveFile.get().asFile
        check(artifact.isFile) { "Missing remapped artifact: ${artifact.path}" }
        check(artifact.name.contains("-$mcProfile-")) { "Artifact is not profile-qualified: ${artifact.name}" }
        val distributionArtifact = file("dist/$mcProfile/${artifact.name}")
        check(distributionArtifact.isFile) { "Missing distribution artifact: ${distributionArtifact.path}" }
        val pythonArtifact = project(":extension:graal:python").tasks.named("jar", AbstractArchiveTask::class.java)
            .get().archiveFile.get().asFile
        check(file("dist/$mcProfile/${pythonArtifact.name}").isFile) {
            "Missing Python extension artifact: dist/$mcProfile/${pythonArtifact.name}"
        }

        ZipFile(artifact).use { jar ->
            fun entry(name: String) = jar.getEntry(name)
            fun requireEntry(name: String) = check(entry(name) != null) { "Missing $name in ${artifact.name}" }
            fun readEntry(name: String): String = jar.getInputStream(entry(name)).bufferedReader().use { it.readText() }

            requireEntry("fabric.mod.json")
            requireEntry("xyz/wagyourtail/jsmacros/core/Core.class")
            val metadata = JsonSlurper().parseText(readEntry("fabric.mod.json")) as Map<*, *>
            check(metadata["id"] == "jsmacros") { "Fabric metadata has the wrong id." }
            check(metadata["version"] == project.version.toString()) { "Fabric metadata has the wrong version." }
            val dependencies = metadata["depends"] as Map<*, *>
            check(dependencies["minecraft"] == minecraftVersions) { "Fabric metadata has the wrong Minecraft versions." }
            check(dependencies["fabricloader"] == ">=$fabricLoaderVersion") { "Fabric metadata has the wrong Fabric Loader floor." }

            val entrypoints = metadata["entrypoints"] as Map<*, *>
            entrypoints.values.flatMap { it as List<*> }.forEach { entrypoint ->
                requireEntry("${entrypoint.toString().replace('.', '/')}.class")
            }

            requireEntry("jsmacros.extension.json")
            val extensionMetadata = JsonSlurper().parseText(readEntry("jsmacros.extension.json")) as Map<*, *>
            val nestedDependencies = extensionMetadata["dependencies"] as List<*>
            check(nestedDependencies.any { it.toString().contains("graal", ignoreCase = true) }) {
                "Core metadata does not declare a Graal extension dependency."
            }
            nestedDependencies.forEach { nestedDependency -> requireEntry(nestedDependency.toString()) }
            check(jar.entries().asSequence().any { it.name.startsWith("META-INF/jsmacrosdeps/") && it.name.contains("graal", ignoreCase = true) }) {
                "The remapped artifact does not embed a Graal extension dependency."
            }
        }

        val rawArtifact = fabricJar.archiveFile.get().asFile
        if (rawArtifact.name != artifact.name) {
            check(!file("dist/$mcProfile/${rawArtifact.name}").exists()) {
                "Distribution includes the intermediate artifact: ${rawArtifact.name}"
            }
        }
    }
}

tasks.withType(PublishModTask::class.java).configureEach {
    dependsOn("createDist")
}
