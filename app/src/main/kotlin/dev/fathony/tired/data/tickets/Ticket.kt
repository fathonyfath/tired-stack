package dev.fathony.tired.data.tickets

import dev.fathony.tired.data.Refused
import java.time.Instant

private val tokenShape = Regex("[A-Za-z0-9-]{8,64}")

/**
 * Proof of who holds a seat; whoever knows it may confirm the hold.
 */
@JvmInline
value class HoldToken(
    private val value: String,
) {
    init {
        require(tokenShape.matches(value)) { "Not a hold token: $value" }
    }

    override fun toString() = value
}

/**
 * A seat kept aside until [until], free again afterwards unless confirmed.
 */
data class Hold(
    val token: HoldToken,
    val seat: Int,
    val until: Instant,
)

data class Ticket(
    val seat: Int,
)

data class Tally(
    val free: Int,
    val held: Int,
    val sold: Int,
)

class SoldOut : Refused("No seat is free right now")

class HoldExpired : Refused("That hold has run out")
