package dev.fathony.tired.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import dev.fathony.tired.data.bank.AccountId
import dev.fathony.tired.data.bank.Balance
import dev.fathony.tired.data.bank.Change
import dev.fathony.tired.data.bank.PostingKey
import dev.fathony.tired.data.contacts.Email
import dev.fathony.tired.data.tickets.HoldToken
import kotlin.time.Instant

private val accountId = column(::AccountId) { it.number }
private val name = column(::Name) { it.toString() }
private val instant = column(Instant::fromEpochMilliseconds) { it.toEpochMilliseconds() }
private val balance = column(::Balance) { it.units }

/**
 * The generated queries on [driver], with each column's value converted to what it is in the code,
 * so a query hands back a [Balance] and not a number.
 */
fun Database.Companion.on(driver: SqlDriver): Database =
    Database(
        driver,
        accountsAdapter = Accounts.Adapter(idAdapter = accountId, nameAdapter = name),
        contactsAdapter = Contacts.Adapter(nameAdapter = name, emailAdapter = column(::Email) { it.toString() }),
        entriesAdapter =
            Entries.Adapter(
                account_idAdapter = accountId,
                postingAdapter = column(::PostingKey) { it.toString() },
                atAdapter = instant,
                previousAdapter = balance,
                changeAdapter = column(::Change) { it.units },
                balanceAdapter = balance,
            ),
        seatsAdapter =
            Seats.Adapter(
                holdAdapter = column(::HoldToken) { it.toString() },
                held_untilAdapter = instant,
                sold_atAdapter = instant,
            ),
    )

private fun <T : Any, S> column(
    decode: (S) -> T,
    encode: (T) -> S,
) = object : ColumnAdapter<T, S> {
    override fun decode(databaseValue: S): T = decode(databaseValue)

    override fun encode(value: T): S = encode(value)
}
