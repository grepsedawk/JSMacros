import org.gradle.api.Project

val Project.mod: ModData get() = ModData(this)
fun Project.prop(key: String): String? = findProperty(key)?.toString()

val Project.commonProject get() = rootProject.project(":fabric")
val Project.commonMod get() = commonProject.mod

val Project.loader: String? get() = prop("loader")

@JvmInline
value class ModData(private val project: Project) {
    val mc: String get() = prop("minecraft_version")

    fun propOrNull(key: String): String? = project.findProperty(key)?.toString()
    fun prop(key: String): String = requireNotNull(propOrNull(key)) { "Missing property '$key'" }
}
