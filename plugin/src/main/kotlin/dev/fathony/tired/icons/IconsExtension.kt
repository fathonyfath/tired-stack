package dev.fathony.tired.icons

import org.gradle.api.provider.MapProperty

abstract class IconsExtension {
    /**
     * Keyed by icon name, so clashing constants are reported instead of collapsing.
     */
    abstract val constants: MapProperty<String, String>

    /**
     * [alias] is only needed when two icons derive the same constant.
     */
    fun add(
        lucideIcon: String,
        alias: String? = null,
    ) {
        val constant = alias ?: pascalCase(lucideIcon)
        require(constant.matches(IDENTIFIER)) {
            "Icon '$lucideIcon' maps to '$constant', which is not a valid Kotlin identifier."
        }
        constants.put(lucideIcon, constant)
    }
}

private val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

private fun pascalCase(lucideIcon: String) =
    lucideIcon
        .split('-')
        .joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }
