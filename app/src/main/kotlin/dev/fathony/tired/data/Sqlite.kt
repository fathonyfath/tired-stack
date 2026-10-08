package dev.fathony.tired.data

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.nio.file.Path

/**
 * One SQLite file in WAL mode, so a read never waits on the write in progress.
 * Opening it brings the tables up to date and fails if that cannot be done, so nothing is ever served from a
 * database that is behind or ahead of this build. Each of [afterMigrations] runs in code right after the migration
 * it names, in the same transaction, for what a migration cannot say in SQL.
 */
class Sqlite(
    file: Path,
    readers: Int,
    vararg afterMigrations: AfterVersion,
) {
    private val lock = Mutex()
    private val reading = Dispatchers.IO.limitedParallelism(readers)
    private val database: Database

    init {
        val url = "jdbc:sqlite:$file"
        val settings =
            SQLiteConfig().apply {
                setJournalMode(SQLiteConfig.JournalMode.WAL)
                setSynchronous(SQLiteConfig.SynchronousMode.NORMAL)
                busyTimeout = 5000
                enforceForeignKeys(true)
            }

        /**
         * The connections stay open and are handed out per transaction: one for each reader and one for the writer.
         */
        val pool =
            HikariDataSource().apply {
                dataSource = SQLiteDataSource(settings).apply { setUrl(url) }
                maximumPoolSize = readers + 1
            }
        val version =
            pool.connection.use { connection ->
                connection.createStatement().use { it.executeQuery("PRAGMA user_version").getLong(1) }
            }
        check(version <= Database.Schema.version) {
            "The database is at version $version, newer than the ${Database.Schema.version} this build knows"
        }
        try {
            JdbcSqliteDriver(url, settings.toProperties(), Database.Schema, callbacks = afterMigrations)
        } catch (failure: Exception) {
            throw IllegalStateException(
                "Could not bring the database from version $version to ${Database.Schema.version}",
                failure,
            )
        }
        database = Database.on(pool.asJdbcDriver())
    }

    /**
     * One snapshot: every query in [query] sees the database as it was at the same moment.
     * No more than the given number of readers run at once; the rest wait their turn.
     */
    suspend fun <T> read(query: (Database) -> T): T =
        withContext(reading) { database.transactionWithResult { query(database) } }

    /**
     * One transaction: everything [update] did is kept when it returns and undone when it throws.
     * Writes take turns, so a transaction never meets another one halfway and never needs a retry.
     */
    suspend fun <T> write(update: (Database) -> T): T =
        lock.withLock { withContext(Dispatchers.IO) { database.transactionWithResult { update(database) } } }
}
