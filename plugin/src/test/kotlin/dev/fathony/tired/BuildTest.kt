package dev.fathony.tired

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Builds a whole app, so it downloads Node and the npm packages on its first run.
 */
class BuildTest {
    @TempDir
    lateinit var dir: File

    private lateinit var project: TestProject

    @BeforeTest
    fun setUp() {
        project = TestProject(dir)
        publishLibraryStub()
        project.file("gradle.properties", "tiredRepository=library-repo")
        project.buildScript(
            """
            plugins {
                id("dev.fathony.tired")
                id("dev.fathony.tired.icons")
            }

            application {
                mainClass = "MainKt"
            }

            tasks.register("printRunClasspath") {
                val classpath = tasks.named<JavaExec>("run").map { it.classpath }
                doLast { classpath.get().forEach { println("run classpath: " + it.name) } }
            }
            """,
        )
        project.file(
            "src/main/kotlin/Main.kt",
            """
            fun main() {
                println(AssetManifest.stylesheet_css + AssetManifest.index_js + AssetManifest.icons_svg + Icons.Search)
            }
            """,
        )
        project.file(
            "src/main/ktml/pages/home.ktml",
            """
            <!DOCTYPE html>
            <html lang="en">
            <body>
                <icon name="Icons.Search"/>
            </body>
            </html>
            """,
        )
        project.file("src/main/web/stylesheet.css", "body {\n  margin: 0;\n}\n")
        project.file("src/main/web/index.js", "console.log(\"hello\");\n")
    }

    /**
     * The plugin adds tired-library from `tiredRepository`; the app doesn't use it, so an empty jar will do.
     */
    private fun publishLibraryStub() {
        val version = System.getProperty("tired.version")
        val folder = project.file("library-repo/dev/fathony/tired/tired-library/$version/.keep").parentFile
        folder.resolve("tired-library-$version.pom").writeText(
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>dev.fathony.tired</groupId>
              <artifactId>tired-library</artifactId>
              <version>$version</version>
            </project>
            """.trimIndent(),
        )
        JarOutputStream(folder.resolve("tired-library-$version.jar").outputStream(), Manifest()).close()
    }

    @Test
    fun `builds hashed assets, icons and templates into the app`() {
        project.build("assemble")

        val manifest = project.read("build/generated/source/webAssets/AssetManifest.kt")
        val names =
            listOf("stylesheet_css", "index_js", "icons_svg").associateWith { name ->
                Regex("""const val $name = "([^"]+)"""").find(manifest)?.groupValues?.get(1)
                    ?: error("AssetManifest has no $name:\n$manifest")
            }
        assertTrue(names.getValue("stylesheet_css").matches(Regex("""stylesheet-[0-9A-Z]{8}\.css""")))
        assertTrue(names.getValue("index_js").matches(Regex("""index-[0-9A-Z]{8}\.js""")))
        assertTrue(names.getValue("icons_svg").matches(Regex("""icons-[0-9A-Z]{8}\.svg""")))
        names.values.forEach { assertTrue(project.exists("build/resources/main/static/$it"), "$it is not in static") }

        assertContains(
            project.read("build/resources/main/static/${names.getValue("icons_svg")}"),
            """<symbol id="search"""",
        )
        assertEquals(
            1,
            Regex(
                "<symbol ",
            ).findAll(project.read("build/resources/main/static/${names.getValue("icons_svg")}")).count(),
        )
        assertContains(project.read("build/ktml/main/dev/ktml/templates/pages/Home.kt"), "writeIcon(")
    }

    @Test
    fun `run serves every icon under a stable name`() {
        project.build("run")

        assertContains(
            project.read("build/generated/source/webAssets/AssetManifest.kt"),
            """const val icons_svg = "icons.svg"""",
        )
        assertContains(project.read("build/resources/main/static/icons.svg"), """<symbol id="shopping-cart"""")
    }

    @Test
    fun `resolves tired-library from the plugin's repository`() {
        val compile = project.build("dependencies", "--configuration", "compileClasspath").output
        assertContains(compile, "dev.fathony.tired:tired-library:${System.getProperty("tired.version")}")
        assertFalse("FAILED" in compile, "tired-library did not resolve:\n$compile")
    }

    @Test
    fun `ktml dev mode is only on the run classpath`() {
        val runtime = project.build("dependencies", "--configuration", "runtimeClasspath").output
        assertFalse("ktml-dev-mode" in runtime, "ktml-dev-mode leaked into runtimeClasspath")

        val run = project.build("printRunClasspath").output
        assertContains(run, "run classpath: ktml-dev-mode-")
    }
}
