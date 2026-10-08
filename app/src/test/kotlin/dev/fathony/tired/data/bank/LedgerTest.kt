package dev.fathony.tired.data.bank

import dev.fathony.tired.data.Name
import dev.fathony.tired.data.Sqlite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class LedgerTest {
    private val file = Files.createTempFile("ledger", ".db").also { it.toFile().deleteOnExit() }

    @Test
    fun `postings sent at the same moment all land`() {
        val sqlite = Sqlite(file, readers = 2)
        val ledger = Ledger(sqlite)
        runBlocking { sqlite.write { it.ledgerQueries.open(Name("Holiday")) } }

        runBlocking(Dispatchers.Default) {
            repeat(40) { number ->
                launch {
                    ledger.post(Posting(PostingKey("deposit-$number"), listOf(Leg(AccountId(1), Amount(5).added()))))
                }
            }
        }

        val holiday = runBlocking { ledger.statement(count = 1) }.accounts.single()
        assertEquals(Balance(200), holiday.balance)
    }
}
