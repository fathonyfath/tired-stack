package dev.fathony.tired.internal

import java.io.File

/**
 * Resolved from the repo root, which may be above an included build's root.
 */
internal fun diffFiles(
    rootDir: File,
    diffBase: String?,
    diffOnly: Boolean,
    vararg extensions: String,
): List<File> {
    val diff =
        when {
            diffBase != null -> listOf("$diffBase...HEAD")
            diffOnly -> listOf("--cached")
            else -> return emptyList()
        }
    val repoRoot = File(git(rootDir, "rev-parse", "--show-toplevel").single())
    return git(rootDir, "diff", "--name-only", "--diff-filter=ACM", *diff.toTypedArray())
        .filter { path -> extensions.any { path.endsWith(it) } }
        .map { repoRoot.resolve(it) }
}

private fun git(
    dir: File,
    vararg args: String,
): List<String> =
    ProcessBuilder("git", *args)
        .directory(dir)
        .start()
        .inputStream
        .bufferedReader()
        .readLines()
        .filter { it.isNotBlank() }
