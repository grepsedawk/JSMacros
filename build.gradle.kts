import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.io.File

plugins {
    // java-base is needed at the root so the aggregate javadoc tasks can
    // resolve a JavaToolchainService for pinning to JDK 25 (required to read
    // 26.1's class file version 69).
    `java-base`
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
    // Loom is applied only in subprojects, but it must be loaded on the root
    // build-script classloader so every Loom subproject shares one set of Loom
    // classes. Otherwise Loom's cross-project mod scan (FabricModJsonHelpers)
    // casts a LoomGradleExtension from another classloader and throws.
    id("net.fabricmc.fabric-loom") apply false
}

repositories {
    mavenLocal()
    mavenCentral()

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

    exclusiveContent {
        forRepository {
            maven {
                name = "TerraformersMC"
                url = uri("https://maven.terraformersmc.com/releases/")
            }
        }
        filter {
            includeGroupAndSubgroups("com.terraformersmc")
        }
    }

    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }
}

// Root-level distribution setup
val distDir = layout.projectDirectory.dir("dist")
val distDirFile = distDir.asFile
val docsBuildDir = layout.buildDirectory.dir("docs").get().asFile
val docletJarFile = layout.projectDirectory.file("buildSrc/build/libs/buildSrc.jar").asFile

// Root-level properties
val modIdProvider = providers.gradleProperty("mod_id")
val channelProvider = providers.gradleProperty("channel").orElse("release")
val modVersionBaseProvider = providers.gradleProperty("mod_version").orElse(providers.gradleProperty("version"))
val buildShaProvider = providers.gradleProperty("build_sha")

val computedBuildShaProvider = providers.provider {
    val sha = buildShaProvider.orElse(
        providers.provider { System.getenv("GITHUB_SHA") ?: "local" }
    ).get()
    sha.take(7)
}

val computedVersionProvider = providers.provider {
    val base = modVersionBaseProvider.get()
    when (channelProvider.get()) {
        "dev" -> "$base-dev-${computedBuildShaProvider.get()}"
        else -> base   // release/beta/alpha: the full version is supplied by the release tag
    }
}

val modId = modIdProvider.get()
val channel = channelProvider.get()
version = computedVersionProvider.get()

val mcVersion = providers.gradleProperty("minecraft_version").get()

data class ExtensionSpec(val path: String, val extId: String)
val jsmExtensions: List<ExtensionSpec> = listOf(
    ExtensionSpec(path = ":extension:graal:python", extId = "graalpy"),
    ExtensionSpec(path = ":extension:ruby", extId = "jruby")
)

val artifactBaseName = providers.provider { "$modId-$mcVersion-$channel-$version" }

tasks.register("prepareDist") {
    group = "distribution"
    description = "Cleans and recreates the dist directory"
    doLast {
        project.delete(distDirFile)
        distDirFile.mkdirs()
    }
}

tasks.register("printVersion") {
    group = "distribution"
    description = "Prints the computed project version for CI workflows"
    doLast {
        println(project.version)
    }
}

tasks.register("runFabricClient") {
    group = "run"
    description = "Runs the Fabric client"
    dependsOn(":fabric:runClient")
}

tasks.register("printArtifactName") {
    group = "distribution"
    description = "Prints the canonical artifact name for CI workflows"
    doLast {
        println(artifactBaseName.get())
    }
}

tasks.register("printMinecraftVersion") {
    group = "distribution"
    description = "Prints the targeted Minecraft version for CI workflows"
    doLast {
        println(mcVersion)
    }
}

// Create the doc-classpath export configuration eagerly, in each docs project's own
// afterEvaluate (registered here, before the project is configured, so it runs before
// Loom's afterEvaluate). Loom resolves the configurations of projects a Loom subproject
// depends on (e.g. :extension:ruby -> :fabric, :extension) during their afterEvaluate; a
// docClasspathExport added afterward (in projectsEvaluated) is then invisible to the
// cross-project consumer below, which breaks createDist's docs aggregation. Creating it
// first keeps it resolvable.
fun Project.isDocsProject() =
    (path.startsWith(":fabric") || path.startsWith(":extension")) &&
        extensions.findByType(SourceSetContainer::class.java) != null

allprojects {
    if (path.startsWith(":fabric") || path.startsWith(":extension")) {
        afterEvaluate {
            if (!isDocsProject()) return@afterEvaluate
            val export = configurations.maybeCreate("docClasspathExport").apply {
                isCanBeConsumed = true
                isCanBeResolved = false
            }
            if (export.artifacts.isEmpty()) {
                // Publish the RESOLVED compile-classpath files (not coordinates): the
                // deobfuscated Minecraft jar is a Loom-internal alias that only resolves
                // inside this project, so we expose the actual files via a lenient view.
                export.outgoing.artifacts(
                    provider {
                        configurations.getByName("compileClasspath")
                            .incoming.artifactView { lenient(true) }.files.files
                    }
                ) {
                    // The exported files include sibling docs projects' compiled output, so
                    // the consuming javadoc tasks must run after every docs project is
                    // compiled. Resolved lazily — siblings may be unevaluated right now.
                    builtBy(rootProject.provider {
                        rootProject.allprojects.filter { it.isDocsProject() }
                            .map { it.tasks.named("classes") }
                    })
                }
            }
        }
    }
}

gradle.projectsEvaluated {
    val docsProjects = allprojects
        .filter { it.path.startsWith(":fabric") || it.path.startsWith(":extension") }
        .mapNotNull { p ->
            val ss = p.extensions.findByType(SourceSetContainer::class.java)
            if (ss == null) null else p
        }

    val mainSourceSets = docsProjects.map { p ->
        p.extensions.getByType(SourceSetContainer::class.java).named("main").get()
    }

    val documentationSources = files(mainSourceSets.map { it.allJava })
    // Aggregate each docs project's compile classpath across project boundaries. Gradle
    // forbids the root resolving a subproject's compileClasspath directly, and the
    // deobfuscated Minecraft jar (net.minecraft:minecraft-merged-*) only resolves inside
    // the Loom-applied subproject anyway. Each docs project re-exports its compile classpath
    // as a consumable "docClasspathExport" configuration (created eagerly above), which we
    // consume here via project dependencies — resolution then happens in each producer's
    // context.
    val documentationClasspath = configurations.maybeCreate("documentationClasspath").apply {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
    docsProjects.forEach { docsProject ->
        dependencies.add(
            documentationClasspath.name,
            dependencies.project(mapOf("path" to docsProject.path, "configuration" to "docClasspathExport"))
        )
    }

    // 26.1.x targets Java 25 bytecode, so the aggregate javadoc tasks must run
    // on a JDK that can read class file version 69. Pin all three to JDK 25 via
    // the toolchain service; foojay auto-provisions if missing.
    val docsJavadocTool = javaToolchains.javadocToolFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.register("generatePyDoc", Javadoc::class.java) {
        group = "documentation"
        description = "Generates the python documentation for the project"
        source(documentationSources)
        classpath = documentationClasspath
        destinationDir = File(docsBuildDir, "python/JsMacrosAC")
        javadocTool.set(docsJavadocTool)
        options.doclet = "com.jsmacrosce.doclet.pydoclet.Main"
        options.docletpath = mutableListOf(docletJarFile)
        (options as CoreJavadocOptions).addStringOption("v", project.version.toString())
    }

    tasks.register("copyPyDoc", Copy::class.java) {
        group = "documentation"
        description = "Copies the python documentation to the build folder"
        dependsOn("generatePyDoc")
        from(rootProject.file("docs/python"))
        into(File(docsBuildDir, "python"))
    }

    tasks.register("generateTSDoc", Javadoc::class.java) {
        group = "documentation"
        description = "Generates the typescript documentation for the project"
        source(documentationSources)
        classpath = documentationClasspath
        destinationDir = File(docsBuildDir, "typescript/headers")
        javadocTool.set(docsJavadocTool)
        options.doclet = "com.jsmacrosce.doclet.tsdoclet.Main"
        options.docletpath = mutableListOf(docletJarFile)
        (options as CoreJavadocOptions).addStringOption("v", project.version.toString())
    }

    tasks.register("copyTSDoc", Copy::class.java) {
        group = "documentation"
        description = "Copies the typescript files to the build folder"
        dependsOn("generateTSDoc")
        from(rootProject.file("docs/typescript"))
        into(File(docsBuildDir, "typescript"))
    }

    tasks.register("generateWebDoc", Javadoc::class.java) {
        group = "documentation"
        description = "Generates the web documentation for the project"
        source(documentationSources)
        classpath = documentationClasspath
        destinationDir = File(docsBuildDir, "web")
        javadocTool.set(docsJavadocTool)
        options.doclet = "com.jsmacrosce.doclet.webdoclet.Main"
        options.docletpath = mutableListOf(docletJarFile)
        (options as CoreJavadocOptions).addStringOption("v", project.version.toString())
        (options as CoreJavadocOptions).addStringOption("mcv", mcVersion)
        (options as StandardJavadocDocletOptions).links(
            "https://docs.oracle.com/javase/8/docs/api/",
            "https://www.javadoc.io/doc/org.slf4j/slf4j-api/1.7.30/",
            "https://javadoc.io/doc/com.neovisionaries/nv-websocket-client/latest/"
        )
    }

    tasks.register("copyWebDoc", Copy::class.java) {
        group = "documentation"
        description = "Copies the web documentation to the build folder"
        dependsOn("generateWebDoc")
        from(rootProject.file("docs/web"))
        into(File(docsBuildDir, "web"))
        inputs.property("version", project.version.toString())
        filesMatching("index.html") {
            expand(mapOf("version" to project.version.toString()))
        }
    }

    tasks.register("createDistDocs", Copy::class.java) {
        group = "distribution"
        description = "Packages generated documentation into the dist directory"
        dependsOn("prepareDist", "copyPyDoc", "copyTSDoc", "copyWebDoc")
        from(docsBuildDir)
        into(distDirFile)
    }

    // Package the Fabric mod jar (26.1 ships deobfuscated, so the plain jar task
    // is the artifact; there is no remapJar).
    val fabricProject = project(":fabric")
    val packageFabricModJar = tasks.register("packageFabricModJar", Copy::class.java) {
        group = "distribution"
        description = "Packages the Fabric mod jar into dist"
        dependsOn("prepareDist", fabricProject.tasks.named("jar"))

        val jarFile = fabricProject.tasks.named("jar").flatMap {
            (it as AbstractArchiveTask).archiveFile
        }

        from(jarFile)
        rename { "$modId-$mcVersion-fabric-${project.version}.jar" }
        into(distDirFile)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    val extensionJarTasks = jsmExtensions.map { ext: ExtensionSpec ->
        tasks.register("package${ext.extId.replaceFirstChar { ch -> ch.uppercase() }}Extension", Copy::class.java) {
            group = "distribution"
            description = "Packages ${ext.extId} extension"

            val extJar = project(ext.path).tasks.named("jar").flatMap {
                (it as AbstractArchiveTask).archiveFile
            }

            dependsOn("prepareDist", extJar)
            from(extJar)
            rename { "$modId-ext-${ext.extId}-$mcVersion-${project.version}.jar" }
            into(File(distDirFile, "extensions"))
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

    val devkitTask = tasks.register("packageDevkit", Zip::class.java) {
        group = "distribution"
        description = "Packages devkit bundle"
        dependsOn("prepareDist", "copyPyDoc", "copyTSDoc", "copyWebDoc")
        destinationDirectory.set(distDir)
        archiveFileName.set("$modId-devkit-$mcVersion-${project.version}.zip")
        from(docsBuildDir) {
            include("web/**")
            include("typescript/**")
            include("python/**")
        }
    }

    val extensionPackTask = tasks.register("packageExtensionsPack", Zip::class.java) {
        group = "distribution"
        description = "Bundles all extensions into the config/jsMacros/extensions layout"
        dependsOn(
            "prepareDist",
            extensionJarTasks,
            "createDistExtensions",
            "createDistDocs",
            packageFabricModJar
        )
        destinationDirectory.set(distDir)
        archiveFileName.set("$modId-extensions-$mcVersion-${project.version}.zip")
        into("config/jsMacros/extensions") {
            from(File(distDirFile, "extensions")) {
                include("*-${project.version}.jar")
            }
        }
    }

    tasks.register("createDistMods") {
        group = "distribution"
        description = "Packages the loader jar into the dist directory"
        dependsOn(packageFabricModJar)
    }

    tasks.register("createDistExtensions") {
        group = "distribution"
        description = "Packages standalone extensions into the dist directory"
        dependsOn(extensionJarTasks)
    }

    tasks.register("createDist") {
        group = "distribution"
        description = "Assembles documentation, mods, extensions, devkits, and sources into dist/"
        dependsOn(
            "createDistDocs",
            "createDistMods",
            "createDistExtensions",
            devkitTask,
            extensionPackTask
        )
    }

    val releaseType = when (channel) {
        "release" -> ReleaseType.STABLE
        "beta" -> ReleaseType.BETA
        else -> ReleaseType.ALPHA
    }

    val modrinthProjectId = providers.gradleProperty("modrinth_id")
        .orElse(providers.environmentVariable("MODRINTH_PROJECT"))
    val modrinthToken = providers.gradleProperty("modrinth_token")
        .orElse(providers.environmentVariable("MODRINTH_TOKEN"))
    fun modrinthChangelog(): String = """
        JsMacros Reloaded ${project.version} for fabric on Minecraft $mcVersion.
        Source: https://github.com/grepsedawk/JsMacros
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
                    fabricProject.tasks.named("jar", AbstractArchiveTask::class.java)
                        .flatMap { it.archiveFile }
                )
            }
        }
    }

    tasks.named("publishMods") {
        dependsOn("createDist")
    }

    tasks.withType(PublishModTask::class.java).configureEach {
        dependsOn("createDist")
    }
}
