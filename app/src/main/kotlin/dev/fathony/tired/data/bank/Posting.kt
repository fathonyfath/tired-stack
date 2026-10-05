package dev.fathony.tired.data.bank

import dev.fathony.tired.data.Refused

private val keyShape = Regex("[A-Za-z0-9-]{8,64}")

@JvmInline
value class AccountId(
    val number: Long,
) {
    init {
        require(number > 0) { "Not an account: $number" }
    }

    override fun toString() = number.toString()
}

/**
 * Chosen by whoever sends a posting, so sending it again is recognised instead of applied twice.
 */
@JvmInline
value class PostingKey(
    private val value: String,
) {
    init {
        require(keyShape.matches(value)) { "Not a posting key: $value" }
    }

    override fun toString() = value
}

data class Leg(
    val account: AccountId,
    val change: Change,
)

/**
 * Everything one request does to the ledger: a deposit or withdrawal is one leg, a transfer is two.
 */
class Posting(
    val key: PostingKey,
    val legs: List<Leg>,
) {
    init {
        require(legs.isNotEmpty() && legs.distinctBy { it.account }.size == legs.size) {
            "A posting touches each of its accounts once"
        }
    }
}

class UnknownAccount(
    account: AccountId,
) : Refused("There is no account $account")
