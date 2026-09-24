package dev.fathony.tired.webassets

import dev.fathony.tired.internal.readResource
import java.security.MessageDigest

internal object Toolchain {
    const val PACKAGE_JSON = "package.json"
    const val LOCKFILE = "package-lock.json"

    val files =
        listOf(
            PACKAGE_JSON,
            LOCKFILE,
            "scripts/build-css.js",
            "scripts/build-js.js",
            "scripts/build-icons.js",
            "scripts/watch.js",
        )

    /**
     * Formats this repo's own sources; apps bring their own formatter.
     */
    val repoOnlyTools = setOf("prettier")

    /**
     * Installed only when their part is enabled.
     */
    val optionalTools =
        mapOf(
            ToolchainPart.ICONS to setOf("lucide-static"),
            ToolchainPart.POSTCSS to setOf("postcss"),
        )

    fun read(name: String): ByteArray = readResource("toolchain/$name")

    /**
     * Changes with the toolchain, so plugin upgrades re-extract it.
     */
    val fingerprint: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        files.forEach { digest.update(read(it)) }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}

enum class ToolchainPart { ICONS, POSTCSS }
