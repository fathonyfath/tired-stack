package dev.fathony.tired

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebAssetsTest {
    @TempDir
    lateinit var dir: File

    private lateinit var project: TestProject

    @BeforeTest
    fun setUp() {
        project = TestProject(dir)
        project.file("src/main/web/index.js", "console.log(1);")
        project.file("src/main/kotlin/Main.kt", "fun main() {}")
        project.file("src/main/ktml/pages/page.ktml", "<html></html>")
        project.file("src/main/resources/templates/mail.html", "<p class=\"underline\"></p>")
    }

    @Test
    fun `base toolchain installs only esbuild`() {
        project.buildScript("""plugins { id("dev.fathony.tired") }""")

        project.build("prepareWebAssetsToolchain")

        val packageJson = project.json("$WEB_ASSETS/package.json")
        assertEquals(setOf("esbuild"), (packageJson["devDependencies"] as Map<*, *>).keys)
        assertFalse("dependencies" in packageJson)
    }

    @Test
    fun `app packages and the tools of enabled parts are added`() {
        project.buildScript(
            """
            plugins {
                id("dev.fathony.tired")
                id("dev.fathony.tired.icons")
            }

            webAssets {
                npm("htmx.org", "2.0.11")
                postcss("some-postcss-plugin")
            }
            """,
        )

        project.build("prepareWebAssetsToolchain")

        val packageJson = project.json("$WEB_ASSETS/package.json")
        assertEquals(
            setOf("esbuild", "lucide-static", "postcss"),
            (packageJson["devDependencies"] as Map<*, *>).keys,
        )
        assertEquals(mapOf("htmx.org" to "2.0.11"), packageJson["dependencies"])
    }

    @Test
    fun `only the web sources are copied without postcss`() {
        project.buildScript("""plugins { id("dev.fathony.tired") }""")

        project.build("syncWebAssetsSources")

        assertTrue(project.exists("$WEB_ASSETS/sources/src/main/web/index.js"))
        assertFalse(project.exists("$WEB_ASSETS/sources/src/main/kotlin"))
        assertFalse(project.exists("$WEB_ASSETS/sources/src/main/ktml"))
    }

    @Test
    fun `kotlin and ktml sources are mirrored with postcss`() {
        project.buildScript(
            """
            plugins { id("dev.fathony.tired") }

            webAssets { postcss("some-postcss-plugin") }
            """,
        )

        project.build("syncWebAssetsSources")

        assertTrue(project.exists("$WEB_ASSETS/sources/src/main/web/index.js"))
        assertTrue(project.exists("$WEB_ASSETS/sources/src/main/kotlin/Main.kt"))
        assertTrue(project.exists("$WEB_ASSETS/sources/src/main/ktml/pages/page.ktml"))
        assertFalse(project.exists("$WEB_ASSETS/sources/src/main/resources"))
    }

    @Test
    fun `mirror adds folders to the copy`() {
        project.buildScript(
            """
            plugins { id("dev.fathony.tired") }

            webAssets {
                postcss("some-postcss-plugin")
                mirror("src/main/resources/templates")
            }
            """,
        )

        project.build("syncWebAssetsSources")

        assertTrue(project.exists("$WEB_ASSETS/sources/src/main/resources/templates/mail.html"))
        assertTrue(project.exists("$WEB_ASSETS/sources/src/main/kotlin/Main.kt"))
    }

    @Test
    fun `folders outside the project are rejected`() {
        project.buildScript(
            """
            plugins { id("dev.fathony.tired") }

            webAssets {
                postcss("some-postcss-plugin")
                mirror("../outside")
            }
            """,
        )

        val result = project.fail("syncWebAssetsSources")

        assertContains(result.output, "webAssets.mirroredDirs must be a folder inside the project")
    }

    @Test
    fun `a custom source dir keeps its path in the copy`() {
        project.file("assets/index.js", "console.log(2);")
        project.buildScript(
            """
            plugins { id("dev.fathony.tired") }

            webAssets { sourceDir = layout.projectDirectory.dir("assets") }
            """,
        )

        project.build("syncWebAssetsSources")

        assertTrue(project.exists("$WEB_ASSETS/sources/assets/index.js"))
        assertFalse(project.exists("$WEB_ASSETS/sources/src/main/web"))
    }
}
