package dev.fathony.tired.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

interface Database {
    /**
     * One snapshot: every query in [query] sees the database as it was at the same moment.
     */
    suspend fun <T> read(query: (Sql) -> T): T

    /**
     * One transaction: everything [update] did is kept when it returns and undone when it throws.
     */
    suspend fun <T> write(update: (Sql) -> T): T
}

/**
 * SQLite in WAL mode: one connection writes while the others read, none of them waiting on each other.
 * Writes take turns, so a transaction never meets another one halfway and never needs a retry.
 * Every connection opens on first use, and SQLite refuses a write on the reading ones.
 */
class WalSqlite(
    private val file: EphemeralFile,
    private val readers: Int,
) : Database {
    private val lock = Mutex()
    private val opened by lazy {
        val url = "jdbc:sqlite:${file.fresh()}"
        val writer = connection(url, "journal_mode = WAL")
        val pool = Channel<Connection>(readers)
        repeat(readers) { pool.trySend(connection(url, "query_only = ON")) }
        Opened(writer, pool)
    }

    override suspend fun <T> read(query: (Sql) -> T): T =
        withContext(Dispatchers.IO) {
            val reader = opened.readers.receive()
            try {
                query(Sql(reader))
            } finally {
                try {
                    reader.rollback()
                } finally {
                    opened.readers.trySend(reader)
                }
            }
        }

    override suspend fun <T> write(update: (Sql) -> T): T =
        lock.withLock {
            withContext(Dispatchers.IO) {
                val writer = opened.writer
                try {
                    update(Sql(writer)).also { writer.commit() }
                } catch (failure: Throwable) {
                    writer.rollback()
                    throw failure
                }
            }
        }

    private fun connection(
        url: String,
        pragma: String,
    ): Connection =
        DriverManager.getConnection(url).also { connection ->
            connection.createStatement().use {
                it.execute("PRAGMA synchronous = NORMAL")
                it.execute("PRAGMA busy_timeout = 5000")
                it.execute("PRAGMA foreign_keys = ON")
                it.execute("PRAGMA $pragma")
            }
            connection.autoCommit = false
        }

    private class Opened(
        val writer: Connection,
        val readers: Channel<Connection>,
    )
}
