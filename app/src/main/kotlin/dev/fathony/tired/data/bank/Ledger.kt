package dev.fathony.tired.data.bank

import dev.fathony.tired.data.Sqlite
import kotlin.time.Clock

/**
 * An account's balance is its latest entry's, so there is no second number to keep in step with the entries.
 */
class Ledger(
    private val sqlite: Sqlite,
    private val clock: Clock = Clock.System,
) {
    suspend fun statement(count: Int): BankStatement =
        sqlite.read { database ->
            val ledger = database.ledgerQueries
            BankStatement(
                accounts =
                    ledger.accounts { id, name, balance -> Account(id, name, Balance(balance)) }.executeAsList(),
                entries = ledger.latest(count.toLong(), ::Entry).executeAsList(),
            )
        }

    /**
     * Applies every leg or none of them, and throws the [Refused] that stopped it.
     * A posting whose key is already in the ledger is returned as recorded, not applied again.
     */
    suspend fun post(posting: Posting): List<Entry> =
        sqlite.write { database ->
            val ledger = database.ledgerQueries
            val recorded = ledger.posted(posting.key, ::Entry).executeAsList()
            if (recorded.isNotEmpty()) return@write recorded
            val at = clock.now()
            posting.legs.forEach { leg ->
                val previous =
                    ledger.balance(leg.account).executeAsOneOrNull()?.let(::Balance)
                        ?: throw UnknownAccount(leg.account)
                ledger.record(leg.account, posting.key, at, previous, leg.change, previous + leg.change)
            }
            ledger.posted(posting.key, ::Entry).executeAsList()
        }
}
