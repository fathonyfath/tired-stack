package dev.fathony.tired.data

import dev.fathony.tired.data.contacts.Email
import dev.fathony.tired.data.contacts.searchText
import kotlin.random.Random

private val firstNames = "Ada Alan Barbara Claude Dennis Edsger Frances Grace Hedy Ivan John Katherine".split(" ")
private val lastNames = "Allen Backus Cerf Dijkstra Engelbart Floyd Goldberg Hopper Iverson Johnson Kay".split(" ")

/**
 * What the app starts with, the same on every start: [contacts] contacts, two empty accounts and [seats] free seats.
 */
suspend fun seed(
    sqlite: Sqlite,
    contacts: Int,
    seats: Int,
) = sqlite.write { database ->
    val random = Random(contacts)
    repeat(contacts) { index ->
        val first = firstNames.random(random)
        val last = lastNames.random(random)
        val name = Name("$first $last")
        val email = Email("${first.lowercase()}.${last.lowercase()}$index@example.com")
        database.addressBookQueries.add(name, email, searchText(name, email)).executeAsOne()
    }
    listOf("Checking", "Savings").forEach { database.ledgerQueries.open(Name(it)) }
    repeat(seats) { database.boxOfficeQueries.build(it + 1L) }
}
