package dev.fathony.tired

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class IconsTest {
    @TempDir
    lateinit var dir: File

    private lateinit var project: TestProject

    @BeforeTest
    fun setUp() {
        project = TestProject(dir)
    }

    private fun icons(block: String) =
        project.buildScript(
            """
            plugins {
                id("dev.fathony.tired")
                id("dev.fathony.tired.icons")
            }

            icons {
                $block
            }
            """,
        )

    @Test
    fun `icons become enum constants`() {
        icons(
            """
            add("search")
            add("chevron-down", alias = "Chevron")
            add("shopping-cart")
            """,
        )

        project.build("generateIcons")

        val icons = project.read("build/generated/source/icons/Icons.kt")
        assertContains(icons, """Chevron("chevron-down"),""")
        assertContains(icons, """Search("search"),""")
        assertContains(icons, """ShoppingCart("shopping-cart"),""")
    }

    @Test
    fun `clashing constants fail the build`() {
        icons(
            """
            add("arrow-down-0-1")
            add("arrow-down-01")
            """,
        )

        val result = project.fail("generateIcons")

        assertContains(
            result.output,
            "Clashing icon constants: arrow-down-0-1 and arrow-down-01 both map to 'ArrowDown01'",
        )
    }

    @Test
    fun `aliases must be kotlin identifiers`() {
        icons("""add("search", alias = "1search")""")

        val result = project.fail("generateIcons")

        assertContains(result.output, "Icon 'search' maps to '1search', which is not a valid Kotlin identifier.")
    }

    @Test
    fun `the icon tag is written to a self-ignoring folder`() {
        icons("""add("search")""")

        project.build("generateIconTag")

        assertContains(project.read("src/main/ktml/tired/icon.ktml"), "<icon name=\"\$Icons\"")
        assertEquals("*", project.read("src/main/ktml/tired/.gitignore").lines().last { it.isNotBlank() })
    }
}
