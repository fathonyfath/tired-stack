package dev.fathony.tired

import dev.fathony.tired.data.EphemeralFile
import dev.fathony.tired.data.Schema
import dev.fathony.tired.data.Seed
import dev.fathony.tired.data.WalSqlite
import dev.fathony.tired.data.bank.SqliteLedger
import dev.fathony.tired.data.contacts.SqliteAddressBook
import dev.fathony.tired.data.tickets.SqliteBoxOffice
import dev.fathony.tired.features.bank
import dev.fathony.tired.features.contacts
import dev.fathony.tired.features.home
import dev.fathony.tired.features.htmxDemo
import dev.fathony.tired.features.sseDemo
import dev.fathony.tired.features.tickets
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import kotlinx.coroutines.runBlocking

fun main() {
    val database = WalSqlite(EphemeralFile("tired-contacts.db"), readers = 4)
    runBlocking {
        Schema(database).create()
        Seed(database, contacts = 10_000, seats = 100).plant()
    }
    embeddedServer(Netty, port = 3000) {
        installTired()
        install(Resources)
        install(SSE)
        install(AutoHeadResponse)
        routing {
            home()
            htmxDemo()
            contacts(SqliteAddressBook(database))
            bank(SqliteLedger(database))
            tickets(SqliteBoxOffice(database))
            sseDemo()
        }
    }.start(wait = true)
}
