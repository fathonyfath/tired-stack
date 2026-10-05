package dev.fathony.tired.data.bank

import dev.fathony.tired.data.Database
import dev.fathony.tired.data.Name
import dev.fathony.tired.data.Sql
import java.time.Clock
import java.time.Instant

interface Ledger {
    suspend fun statement(count: Int): BankStatement

    /**
     * Applies every leg or none of them, and throws the [Refused] that stopped it.
     * A posting whose key is already in the ledger is returned as recorded, not applied again.
     */
    suspend fun post(posting: Posting): List<Entry>
}

/**
 * An account's balance is its latest entry's, so there is no second number to keep in step with the entries.
 */
class SqliteLedger(
    private val database: Database,
    private val clock: Clock = Clock.systemUTC(),
) : Ledger {
    private val balance = "COALESCE((SELECT balance FROM entries WHERE account_id = a.id ORDER BY id DESC LIMIT 1), 0)"

    override suspend fun statement(count: Int): BankStatement =
        database.read { sql ->
            BankStatement(
                accounts =
                    sql.query("SELECT a.id, a.name, $balance FROM accounts a ORDER BY a.id") {
                        Account(AccountId(it.getLong(1)), Name(it.getString(2)), Balance(it.getLong(3)))
                    },
                entries = entries(sql, "ORDER BY e.id DESC LIMIT ?", count),
            )
        }

    override suspend fun post(posting: Posting): List<Entry> =
        database.write { sql ->
            val key = posting.key.toString()
            val recorded = entries(sql, "WHERE e.posting = ? ORDER BY e.id", key)
            if (recorded.isNotEmpty()) return@write recorded
            val at = clock.millis()
            posting.legs.forEach { leg ->
                val previous =
                    sql
                        .query("SELECT $balance FROM accounts a WHERE a.id = ?", leg.account.number) {
                            Balance(it.getLong(1))
                        }.firstOrNull() ?: throw UnknownAccount(leg.account)
                sql.execute(
                    "INSERT INTO entries (account_id, posting, at, previous, change, balance) VALUES (?, ?, ?, ?, ?, ?)",
                    leg.account.number,
                    key,
                    at,
                    previous.units,
                    leg.change.units,
                    (previous + leg.change).units,
                )
            }
            entries(sql, "WHERE e.posting = ? ORDER BY e.id", key)
        }

    private fun entries(
        sql: Sql,
        clause: String,
        vararg args: Any?,
    ): List<Entry> =
        sql.query(
            """
            SELECT e.id, a.name, e.posting, e.at, e.previous, e.change, e.balance
            FROM entries e JOIN accounts a ON a.id = e.account_id $clause
            """.trimIndent(),
            *args,
        ) {
            Entry(
                id = it.getLong(1),
                account = Name(it.getString(2)),
                posting = PostingKey(it.getString(3)),
                at = Instant.ofEpochMilli(it.getLong(4)),
                previous = Balance(it.getLong(5)),
                change = Change(it.getLong(6)),
                balance = Balance(it.getLong(7)),
            )
        }
}
