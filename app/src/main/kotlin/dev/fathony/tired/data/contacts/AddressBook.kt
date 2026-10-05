package dev.fathony.tired.data.contacts

import dev.fathony.tired.data.Database
import dev.fathony.tired.data.Name

/**
 * [matching] and [olderThan] narrow the book without touching the database; [newest] runs the query.
 */
interface AddressBook {
    fun matching(text: String): AddressBook

    fun olderThan(id: Long): AddressBook

    suspend fun newest(count: Int): List<Contact>

    suspend fun add(
        name: Name,
        email: Email,
    ): Contact

    suspend fun remove(id: Long)
}

class SqliteAddressBook(
    private val database: Database,
    private val where: String = "1",
    private val args: List<Any> = emptyList(),
) : AddressBook {
    override fun matching(text: String): AddressBook =
        SqliteAddressBook(database, "$where AND (name LIKE ? OR email LIKE ?)", args + "%$text%" + "%$text%")

    override fun olderThan(id: Long): AddressBook = SqliteAddressBook(database, "$where AND id < ?", args + id)

    override suspend fun newest(count: Int): List<Contact> =
        database.read { sql ->
            sql.query(
                "SELECT id, name, email FROM contacts WHERE $where ORDER BY id DESC LIMIT ?",
                *(args + count).toTypedArray(),
            ) { Contact(it.getLong(1), Name(it.getString(2)), Email(it.getString(3))) }
        }

    override suspend fun add(
        name: Name,
        email: Email,
    ): Contact =
        database.write { sql ->
            sql
                .query(
                    "INSERT INTO contacts (name, email) VALUES (?, ?) RETURNING id",
                    name.toString(),
                    email.toString(),
                ) { Contact(it.getLong(1), name, email) }
                .single()
        }

    override suspend fun remove(id: Long) {
        database.write { sql -> sql.execute("DELETE FROM contacts WHERE id = ?", id) }
    }
}
