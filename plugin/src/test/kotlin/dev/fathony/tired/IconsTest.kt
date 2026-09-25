package dev.fathony.tired

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IconsTest {
    @TempDir
    lateinit var dir: File

    private lateinit var project: TestProject

    @BeforeTest
    fun setUp() {
        project = TestProject(dir)
        project.buildScript(
            """
            plugins {
                id("dev.fathony.tired")
                id("dev.fathony.tired.icons")
            }
            """,
        )
    }

    @Test
    fun `every lucide icon becomes a constant`() {
        project.build("generateIcons")

        val icons = project.read("build/generated/source/icons/Icons.kt")
        assertContains(icons, """Search("search"),""")
        assertContains(icons, """ShoppingCart("shopping-cart"),""")
        assertContains(icons, """ArrowDown01("arrow-down-0-1"),""")
        assertFalse("\"arrow-down-01\"" in icons, "deprecated aliases are left out")
    }

    @Test
    fun `the sprite only holds icons the sources reference`() {
        project.file("src/main/kotlin/Main.kt", "val icon = Icons.Search\nval notAnIcon = Icons.NotAnIcon\n")
        project.file("src/main/ktml/pages/cart.ktml", """<icon name="Icons.ShoppingCart"/>""")

        project.build("npmBuildSvg")

        assertEquals("search\nshopping-cart", project.read("$WEB_ASSETS/icons.list"))
        val sprite = File(dir, "$WEB_ASSETS/dist/icons").listFiles()!!.single().readText()
        assertContains(sprite, """<symbol id="search"""")
        assertContains(sprite, """<symbol id="shopping-cart"""")
        assertFalse("chevron-down" in sprite)
    }

    @Test
    fun `the icon tag is written to a self-ignoring folder`() {
        project.build("generateIconTag")

        assertContains(project.read("src/main/ktml/tired/icon.ktml"), "<icon name=\"\$Icons\"")
        assertEquals("*", project.read("src/main/ktml/tired/.gitignore").lines().last { it.isNotBlank() })
    }
}
