import java.io.File

fun diffFiles(rootDir: File, diffBase: String?, diffOnly: Boolean, vararg extensions: String): List<String> {
    val cmd = when {
        diffBase != null -> listOf("git", "diff", "--name-only", "--diff-filter=ACM", "$diffBase...HEAD")
        diffOnly -> listOf("git", "diff", "--cached", "--name-only", "--diff-filter=ACM")
        else -> return emptyList()
    }
    return ProcessBuilder(cmd)
        .directory(rootDir)
        .start()
        .inputStream
        .bufferedReader()
        .readLines()
        .filter { path -> extensions.any { path.endsWith(it) } }
}
