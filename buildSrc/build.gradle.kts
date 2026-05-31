plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "2.2.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(gradleApi())
    // buildSrc output sits in the parent classloader scope of every project build
    // script, so its gson shadows Loom's. Loom 1.15.4 deserializes the MC version
    // manifest into a Java record, which needs record-aware gson (2.10+); 2.9.0
    // throws IllegalAccessException on the record's final fields.
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("commons-io:commons-io:2.7")
    // fletching-table's J52J converter needs JsonBuilder.allowComments (kotlinx-
    // serialization-json 1.7.0+), but Loom drags an older 1.6.3 onto the shared
    // plugin classpath. buildSrc output sits in the parent classloader scope of
    // every project build script, so declaring a newer version here makes it win.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}
