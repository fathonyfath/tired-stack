package dev.fathony.tired

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

/**
 * A throwaway app in [dir] that applies the tired plugin through TestKit's plugin classpath.
 */
class TestProject(
    val dir: File,
) {
    fun file(
        path: String,
        content: String = "",
    ): File =
        dir.resolve(path).apply {
            parentFile.mkdirs()
            writeText(content.trimIndent() + "\n")
        }

    fun buildScript(content: String) {
        file("settings.gradle.kts", """rootProject.name = "sample"""")
        file("build.gradle.kts", content)
    }

    fun build(vararg arguments: String): BuildResult = runner(*arguments).build()

    fun fail(vararg arguments: String): BuildResult = runner(*arguments).buildAndFail()

    fun exists(path: String) = dir.resolve(path).exists()

    fun read(path: String) = dir.resolve(path).readText()

    @Suppress("UNCHECKED_CAST")
    fun json(path: String) = JsonSlurper().parse(dir.resolve(path)) as Map<String, Any?>

    private fun runner(vararg arguments: String) =
        GradleRunner
            .create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace")
}

const val WEB_ASSETS = "build/tired/web-assets"
