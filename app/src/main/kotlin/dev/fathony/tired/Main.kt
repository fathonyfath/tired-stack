package dev.fathony.tired

import dev.fathony.tired.data.EphemeralFile
import dev.fathony.tired.data.Sqlite
import dev.fathony.tired.data.bank.Ledger
import dev.fathony.tired.data.contacts.AddressBook
import dev.fathony.tired.data.contacts.fillSearchText
import dev.fathony.tired.data.seed
import dev.fathony.tired.data.tickets.BoxOffice
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
    val sqlite = Sqlite(EphemeralFile("tired-contacts.db").fresh(), readers = 4, fillSearchText)
    runBlocking {
        seed(sqlite, contacts = 10_000, seats = 100)
    }
    embeddedServer(Netty, port = 3000) {
        installTired()
        install(Resources)
        install(SSE)
        install(AutoHeadResponse)
        routing {
            home()
            htmxDemo()
            contacts(AddressBook(sqlite))
            bank(Ledger(sqlite))
            tickets(BoxOffice(sqlite))
            sseDemo()
        }
    }.start(wait = true)
}
