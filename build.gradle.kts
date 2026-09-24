import com.github.gradle.node.NodeExtension
import com.github.gradle.node.npm.task.NpmTask

/**
 * Loads the Kotlin Gradle plugin once for all modules.
 */
plugins {
    id("dev.fathony.tired") apply false
}

/**
 * Prettier for this repo's JS and CSS, run from the plugin's toolchain. Apps using the plugin bring their own.
 */
apply(plugin = "com.github.node-gradle.node")

configure<NodeExtension> {
    download = true
    version = libs.versions.nodejs.get()
    nodeProjectDir = layout.projectDirectory.dir("plugin/src/main/resources/dev/fathony/tired/toolchain")
}

val prettierFiles =
    files(
        fileTree("app/src/main/web") { include("**/*.js", "**/*.css") },
        fileTree("plugin/src/main/resources/dev/fathony/tired/toolchain/scripts") { include("**/*.js") },
    )

/**
 * Only the staged files with `-PdiffOnly`, or those changed since a ref with `-PdiffBase`, for the pre-commit hook.
 */
val changedFiles: Set<File>? =
    run {
        val diffBase = findProperty("diffBase")?.toString()
        if (diffBase == null && !hasProperty("diffOnly")) return@run null
        val diff = if (diffBase != null) "$diffBase...HEAD" else "--cached"
        providers
            .exec { commandLine("git", "diff", "--name-only", "--diff-filter=ACM", diff) }
            .standardOutput.asText
            .get()
            .lines()
            .filter { it.isNotBlank() }
            .map { rootDir.resolve(it) }
            .toSet()
    }

fun NpmTask.prettier(mode: String) {
    dependsOn(tasks.named("npmInstall"))
    val targets =
        provider {
            val all = prettierFiles.files
            (changedFiles?.let { changed -> all.filter { it in changed } } ?: all).map { it.absolutePath }.sorted()
        }
    args.set(targets.map { listOf("exec", "--", "prettier", mode) + it })
    onlyIf("there are files to check") { targets.get().isNotEmpty() }
    inputs.files(prettierFiles)
}

val prettierCheck = tasks.register<NpmTask>("prettierCheck") { prettier("--check") }
val prettierFormat = tasks.register<NpmTask>("prettierFormat") { prettier("--write") }

/**
 * Included builds don't receive tasks run by name, so these forward them.
 */
mapOf(
    "check" to prettierCheck,
    "format" to prettierFormat,
    "ktlintCheck" to null,
    "test" to null,
).forEach { (name, prettier) ->
    tasks.register(name) {
        dependsOn(listOf("plugin", "library").map { gradle.includedBuild(it).task(":$name") })
        prettier?.let { dependsOn(it) }
    }
}
