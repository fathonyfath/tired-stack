package dev.fathony.tired.data.bank

import dev.fathony.tired.data.Name
import java.time.Instant

/**
 * One line of the ledger: [previous] plus [change] is [balance].
 */
data class Entry(
    val id: Long,
    val account: Name,
    val posting: PostingKey,
    val at: Instant,
    val previous: Balance,
    val change: Change,
    val balance: Balance,
)

data class Account(
    val id: AccountId,
    val name: Name,
    val balance: Balance,
)

/**
 * The accounts and the latest entries, newest first, read at the same moment.
 */
data class BankStatement(
    val accounts: List<Account>,
    val entries: List<Entry>,
)
