package dev.fathony.tired.data.contacts

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.fathony.tired.data.Database
import dev.fathony.tired.data.Name
import dev.fathony.tired.data.Sqlite
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class AddressBookTest {
    private val file = Files.createTempFile("contacts", ".db").also { it.toFile().deleteOnExit() }

    @Test
    fun `a search ignores case and accents`() =
        runBlocking {
            val book = AddressBook(Sqlite(file, readers = 2))
            book.add(Name("José Álvarez"), Email("jose@example.com"))
            book.add(Name("Grace Hopper"), Email("grace@example.com"))

            val found = book.newest(count = 10, matching = "ALVAREZ")

            assertEquals(listOf(Name("José Álvarez")), found.map { it.name })
        }

    @Test
    fun `a contact from before the search text existed is found once the file is migrated`() =
        runBlocking {
            val old = JdbcSqliteDriver("jdbc:sqlite:$file")
            Database.Schema.migrate(old, oldVersion = 1, newVersion = 4)
            old.execute(null, "INSERT INTO contacts (name, email) VALUES ('Zoë Kravitz', 'zoe@example.com')", 0)
            old.execute(null, "PRAGMA user_version = 4", 0)

            val book = AddressBook(Sqlite(file, readers = 2, fillSearchText))

            assertEquals(listOf(Name("Zoë Kravitz")), book.newest(count = 10, matching = "zoe kra").map { it.name })
        }
}
