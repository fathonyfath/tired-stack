package dev.fathony.tired.data.bank

import dev.fathony.tired.data.Refused

private const val LARGEST = 1_000_000_000_000

/**
 * What an account holds, in whole units; never below zero.
 */
@JvmInline
value class Balance(
    val units: Long,
) {
    init {
        require(units >= 0) { "Not a balance: $units" }
    }

    operator fun plus(change: Change): Balance {
        val units = Math.addExact(units, change.units)
        if (units < 0) throw InsufficientFunds(this, change)
        return Balance(units)
    }

    override fun toString() = units.toString()
}

/**
 * How much money a request is about; always more than zero.
 */
@JvmInline
value class Amount(
    val units: Long,
) {
    init {
        require(units in 1..LARGEST) { "Not an amount: $units" }
    }

    fun added() = Change(units)

    fun removed() = Change(-units)

    override fun toString() = units.toString()
}

/**
 * What one entry does to a balance: adds when positive, removes when negative; never zero.
 */
@JvmInline
value class Change(
    val units: Long,
) {
    init {
        require(units != 0L && units in -LARGEST..LARGEST) { "Not a change: $units" }
    }

    override fun toString() = if (units > 0) "+$units" else units.toString()
}

class InsufficientFunds(
    balance: Balance,
    change: Change,
) : Refused("Cannot apply $change to a balance of $balance")
