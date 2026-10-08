package dev.fathony.tired.data.contacts

import dev.fathony.tired.data.Name
import dev.fathony.tired.data.Sqlite

class AddressBook(
    private val sqlite: Sqlite,
) {
    /**
     * The newest [count] contacts whose name or email contains [matching], whatever its case or accents,
     * among those added before [before].
     */
    suspend fun newest(
        count: Int,
        matching: String = "",
        before: Long = Long.MAX_VALUE,
    ): List<Contact> =
        sqlite.read {
            it.addressBookQueries.newest("%${folded(matching)}%", before, count.toLong(), ::Contact).executeAsList()
        }

    suspend fun add(
        name: Name,
        email: Email,
    ): Contact =
        sqlite.write {
            Contact(it.addressBookQueries.add(name, email, searchText(name, email)).executeAsOne(), name, email)
        }

    suspend fun remove(id: Long) {
        sqlite.write { it.addressBookQueries.remove(id) }
    }
}
