package dev.fathony.tired

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.ServerSocket
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains

/**
 * Against a small server that knows `/`, the stylesheet it links to and `/unstyled`, whose stylesheet is missing.
 */
class SmokeTestTest {
    @TempDir
    lateinit var dir: File

    private lateinit var project: TestProject

    private val port = ServerSocket(0).use { it.localPort }

    @BeforeTest
    fun setUp() {
        project = TestProject(dir)
        val server = javaClass.getResource("/smoke-test/Main.kt")!!.readText()
        project.file("src/main/kotlin/Main.kt", server.replace("__PORT__", port.toString()))
    }

    private fun smokeTesting(vararg paths: String) =
        project.buildScript(
            """
            plugins {
                id("dev.fathony.tired.ktor-app")
            }

            application {
                mainClass = "MainKt"
            }

            smokeTest {
                port = $port
                paths = listOf(${paths.joinToString { "\"$it\"" }})
            }
            """,
        )

    @Test
    fun `passes when the pages and what they link to answer`() {
        smokeTesting("/")

        val output = project.build("smokeTest").output

        assertContains(output, "/ answered 200")
        assertContains(output, "/app.css answered 200")
    }

    @Test
    fun `fails on a page that answers with an error`() {
        smokeTesting("/", "/gone")

        val output = project.fail("smokeTest").output

        assertContains(output, "The app failed its smoke test: /gone answered 404.")
    }

    @Test
    fun `fails on a missing asset that a page links to`() {
        smokeTesting("/unstyled")

        val output = project.fail("smokeTest").output

        assertContains(output, "The app failed its smoke test: /missing.css answered 404.")
    }

    @Test
    fun `fails with the app's output when it doesn't start`() {
        smokeTesting("/")
        project.file("src/main/kotlin/Main.kt", """fun main(): Unit = error("no database")""")

        val output = project.fail("smokeTest").output

        assertContains(output, "The app exited with 1 before answering on port $port.")
        assertContains(output, "no database")
    }
}
