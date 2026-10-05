package dev.fathony.tired.data

import java.nio.file.Files
import java.nio.file.Path

/**
 * A database file in the temp directory that starts empty, so nothing outlives the container.
 */
class EphemeralFile(
    private val name: String,
) {
    /**
     * Removes what an earlier run left behind, including SQLite's WAL files.
     */
    fun fresh(): Path {
        val file = Path.of(System.getProperty("java.io.tmpdir"), name)
        listOf("", "-wal", "-shm").forEach { Files.deleteIfExists(Path.of("$file$it")) }
        return file
    }
}
