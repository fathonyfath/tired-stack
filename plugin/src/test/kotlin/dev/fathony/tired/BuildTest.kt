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
import kotlin.test.assertNotEquals
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

    /**
     * The asset names `AssetManifest` holds, by constant.
     */
    private fun assetNames(): Map<String, String> {
        val manifest = project.read("build/generated/source/webAssets/AssetManifest.kt")
        return listOf("stylesheet_css", "index_js", "icons_svg").associateWith { name ->
            Regex("""const val $name = "([^"]+)"""").find(manifest)?.groupValues?.get(1)
                ?: error("AssetManifest has no $name:\n$manifest")
        }
    }

    @Test
    fun `builds hashed assets, icons and templates into the app`() {
        project.build("assemble")

        val names = assetNames()
        val stylesheet = names.getValue("stylesheet_css")
        val script = names.getValue("index_js")
        val icons = names.getValue("icons_svg")
        assertTrue(stylesheet.matches(Regex("""stylesheet-[0-9A-Z]{8}\.css""")), "stylesheet_css is $stylesheet")
        assertTrue(script.matches(Regex("""index-[0-9A-Z]{8}\.js""")), "index_js is $script")
        assertTrue(icons.matches(Regex("""icons-[0-9A-Z]{8}\.svg""")), "icons_svg is $icons")
        names.values.forEach { assertTrue(project.exists("build/resources/main/static/$it"), "$it is not in static") }

        val sprite = project.read("build/resources/main/static/$icons")
        val symbols = Regex("""<symbol id="([^"]+)"""").findAll(sprite).map { it.groupValues[1] }.toList()
        assertEquals(listOf("search"), symbols)
        assertContains(project.read("build/ktml/main/dev/ktml/templates/pages/Home.kt"), "writeIcon(")
    }

    @Test
    fun `hashed names change with the content`() {
        project.build("assemble")
        val before = assetNames()

        project.file("src/main/web/stylesheet.css", "body {\n  margin: 1px;\n}\n")
        project.file("src/main/web/index.js", "console.log(\"changed\");\n")
        project.file("src/main/kotlin/Icon.kt", "val cart = Icons.ShoppingCart")
        project.build("assemble")
        val after = assetNames()

        before.forEach { (name, old) -> assertNotEquals(old, after.getValue(name), "$name kept its name") }
    }

    @Test
    fun `run serves assets under stable names and every icon`() {
        project.build("run")

        assertEquals(
            mapOf("stylesheet_css" to "stylesheet.css", "index_js" to "index.js", "icons_svg" to "icons.svg"),
            assetNames(),
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
