package dev.fathony.tired.data

import kotlin.random.Random

/**
 * What the app starts with, the same on every start: [contacts] contacts, two empty accounts and [seats] free seats.
 */
class Seed(
    private val database: Database,
    private val contacts: Int,
    private val seats: Int,
) {
    private val firstNames = "Ada Alan Barbara Claude Dennis Edsger Frances Grace Hedy Ivan John Katherine".split(" ")
    private val lastNames = "Allen Backus Cerf Dijkstra Engelbart Floyd Goldberg Hopper Iverson Johnson Kay".split(" ")

    suspend fun plant() =
        database.write { sql ->
            val random = Random(contacts)
            repeat(contacts) { index ->
                val first = firstNames.random(random)
                val last = lastNames.random(random)
                sql.execute(
                    "INSERT INTO contacts (name, email) VALUES (?, ?)",
                    "$first $last",
                    "${first.lowercase()}.${last.lowercase()}$index@example.com",
                )
            }
            listOf("Checking", "Savings").forEach { sql.execute("INSERT INTO accounts (name) VALUES (?)", it) }
            repeat(seats) { sql.execute("INSERT INTO seats (number) VALUES (?)", it + 1) }
        }
}
