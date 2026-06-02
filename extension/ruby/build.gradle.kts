import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("multiloader-loader")
    id("net.fabricmc.fabric-loom")
}

val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.prop("minecraft_version")

base {
    archivesName.set("$mod_id-ruby-$minecraft_version")
}

repositories {
    mavenCentral()
}

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraft_version")
    implementation("net.fabricmc:fabric-loader:${commonMod.prop("fabric_loader_version")}")

    // jsmacros API — provided by the base mod at runtime
    compileOnly(project(":fabric"))
    implementation(project(":extension"))
    compileOnly("org.jetbrains:annotations:20.1.0")

    // JRuby runtime, nested into the mod jar via Fabric jar-in-jar
    include("org.jruby:jruby-complete:9.4.5.0")
    implementation("org.jruby:jruby-complete:9.4.5.0")

    // provided by the base mod at runtime
    compileOnly("com.google.guava:guava:31.1-jre")
    compileOnly("com.google.code.gson:gson:2.10")
    compileOnly("org.slf4j:slf4j-api:2.0.16")

    testImplementation(project(":extension"))
    testImplementation(project(":fabric"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.jetbrains:annotations:20.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    // Gradle 9 no longer ships the JUnit Platform launcher implicitly.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.8.1")
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to project.version,
        "minecraft_version" to minecraft_version
    )
    filesMatching("fabric.mod.json") {
        expand(props)
    }
    inputs.properties(props)
}

tasks.test {
    useJUnitPlatform()
}
