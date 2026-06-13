
dependencies {
    jsmacrosExtensionInclude(graalLibs.graal.sdk)
    jsmacrosExtensionInclude(graalLibs.truffle)
    jsmacrosExtensionInclude(graalLibs.regex)
}

var projectPath = project.path

subprojects {

    dependencies {
        implementation(project(projectPath))

        for (dependency in parent!!.configurations.jsmacrosExtensionInclude.get().dependencies) {
            implementation(dependency)
        }

        testImplementation(parent!!.sourceSets.test.get().output)
        for (dependency in parent!!.configurations.testImplementation.get().dependencies) {
            testImplementation(dependency)
        }
        // ExtensionLoader (loaded by these tests) links against FabricLoader.
        testRuntimeOnly(rootProject.libs.fabric.loader)
    }

    repositories {
        maven("https://maven.fabricmc.net/")
    }
}

tasks.test {
    useJUnitPlatform()
}