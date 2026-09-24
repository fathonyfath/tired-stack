package dev.fathony.tired.webassets

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * The lockfile isn't an output: `npmInstall` updates it in place, which mustn't make this task stale.
 */
@DisableCachingByDefault(because = "Extracting a few bundled files is cheaper than caching them")
abstract class PrepareWebAssetsToolchain : DefaultTask() {
    @get:Input
    abstract val npmDependencies: MapProperty<String, String>

    @get:Input
    abstract val parts: SetProperty<ToolchainPart>

    @get:Input
    val toolchainFingerprint: String = Toolchain.fingerprint

    @get:Internal
    abstract val toolchainDir: DirectoryProperty

    @get:OutputFiles
    abstract val generatedFiles: ConfigurableFileCollection

    @TaskAction
    fun prepare() {
        val dir = toolchainDir.get().asFile
        Toolchain.files.forEach { name ->
            val target = dir.resolve(name)
            target.parentFile.mkdirs()
            val content = Toolchain.read(name)
            target.writeBytes(if (name == Toolchain.PACKAGE_JSON) composePackageJson(content) else content)
        }
    }

    private fun composePackageJson(packageJson: ByteArray): ByteArray {
        val disabledTools = (Toolchain.optionalTools - parts.get()).values.flatten().toSet() + Toolchain.repoOnlyTools

        @Suppress("UNCHECKED_CAST")
        val json = JsonSlurper().parse(packageJson) as MutableMap<String, Any?>

        @Suppress("UNCHECKED_CAST")
        val tools = json["devDependencies"] as Map<String, Any?>
        json["devDependencies"] = tools.filterKeys { it !in disabledTools }

        val dependencies = npmDependencies.get()
        if (dependencies.isNotEmpty()) json["dependencies"] = dependencies.toSortedMap()
        return (JsonOutput.prettyPrint(JsonOutput.toJson(json)) + "\n").toByteArray()
    }
}
