package dev.fathony.tired.icons

import groovy.json.JsonSlurper
import java.io.File

/**
 * Lucide's canonical icon names, without the deprecated aliases that `icons/` also holds.
 */
internal fun readLucideIcons(tags: File): List<String> =
    (JsonSlurper().parse(tags) as Map<*, *>).keys.map { it.toString() }.sorted()

internal fun constantName(lucideIcon: String): String {
    val constant = lucideIcon.split('-').joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }
    require(constant.matches(IDENTIFIER)) {
        "Icon '$lucideIcon' maps to '$constant', which is not a valid Kotlin identifier."
    }
    return constant
}

/**
 * Finds `Icons.Search` in Kotlin and in KTML attributes alike.
 */
internal val ICON_REFERENCE = Regex("""\bIcons\.([A-Za-z_][A-Za-z0-9_]*)""")

private val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
