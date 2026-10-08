package dev.fathony.tired.data.tickets

import dev.fathony.tired.data.Sqlite
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Every seat is a row, so a seat can only ever have one owner; there is no count that could run past the capacity.
 * A hold that ran out is not cleaned up, it is simply free for the next hold to take.
 */
class BoxOffice(
    private val sqlite: Sqlite,
    private val holdFor: Duration = 30.seconds,
    private val clock: Clock = Clock.System,
) {
    suspend fun tally(): Tally =
        sqlite.read { database ->
            database.boxOfficeQueries
                .counts(clock.now()) { seats, sold, held ->
                    Tally(free = (seats - sold - held).toInt(), held = held.toInt(), sold = sold.toInt())
                }.executeAsOne()
        }

    /**
     * Throws [SoldOut] when every seat is sold or held.
     */
    suspend fun hold(): Hold =
        sqlite.write { database ->
            val now = Instant.fromEpochMilliseconds(clock.now().toEpochMilliseconds())
            val until = now + holdFor
            val token = HoldToken(UUID.randomUUID().toString())
            val seat = database.boxOfficeQueries.hold(token, until, now).executeAsOneOrNull() ?: throw SoldOut()
            Hold(token, seat.toInt(), until)
        }

    /**
     * Confirming the same hold again gives the same ticket. Throws [HoldExpired] once the hold has run out.
     */
    suspend fun confirm(token: HoldToken): Ticket =
        sqlite.write { database ->
            val seats = database.boxOfficeQueries
            val seat =
                seats.sell(clock.now(), token).executeAsOneOrNull()
                    ?: seats.sold(token).executeAsOneOrNull()
                    ?: throw HoldExpired()
            Ticket(seat.toInt())
        }
}
