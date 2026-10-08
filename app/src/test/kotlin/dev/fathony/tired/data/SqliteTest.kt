package dev.fathony.tired.data

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SqliteTest {
    private val file = Files.createTempFile("sqlite", ".db").also { it.toFile().deleteOnExit() }

    private suspend fun Sqlite.accounts(): List<String> =
        read { database -> database.ledgerQueries.accounts { _, name, _ -> name.toString() }.executeAsList() }

    @Test
    fun `a write is there to read afterwards`() =
        runBlocking {
            val sqlite = Sqlite(file, readers = 2)

            sqlite.write { it.ledgerQueries.open(Name("Holiday")) }

            assertEquals(listOf("Holiday"), sqlite.accounts())
        }

    @Test
    fun `a write that throws is undone`() =
        runBlocking {
            val sqlite = Sqlite(file, readers = 2)

            assertFailsWith<IllegalArgumentException> {
                sqlite.write {
                    it.ledgerQueries.open(Name("Holiday"))
                    throw IllegalArgumentException("changed my mind")
                }
            }

            assertEquals(emptyList(), sqlite.accounts())
        }

    @Test
    fun `a file that is opened again keeps its rows`() =
        runBlocking {
            Sqlite(file, readers = 2).write { it.ledgerQueries.open(Name("Holiday")) }

            val reopened = Sqlite(file, readers = 2)

            assertEquals(listOf("Holiday"), reopened.accounts())
        }

    @Test
    fun `a file from a newer build is refused`() {
        DriverManager.getConnection("jdbc:sqlite:$file").use { connection ->
            connection.createStatement().use { it.execute("PRAGMA user_version = 99") }
        }

        val refusal = assertFailsWith<IllegalStateException> { Sqlite(file, readers = 2) }

        assertEquals(
            "The database is at version 99, newer than the ${Database.Schema.version} this build knows",
            refusal.message,
        )
    }
}
