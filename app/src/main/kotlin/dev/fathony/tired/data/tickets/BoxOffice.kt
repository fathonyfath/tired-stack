package dev.fathony.tired.data.tickets

import dev.fathony.tired.data.Database
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

interface BoxOffice {
    suspend fun tally(): Tally

    /**
     * Throws [SoldOut] when every seat is sold or held.
     */
    suspend fun hold(): Hold

    /**
     * Confirming the same hold again gives the same ticket. Throws [HoldExpired] once the hold has run out.
     */
    suspend fun confirm(token: HoldToken): Ticket
}

/**
 * Every seat is a row, so a seat can only ever have one owner; there is no count that could run past the capacity.
 * A hold that ran out is not cleaned up, it is simply free for the next hold to take.
 */
class SqliteBoxOffice(
    private val database: Database,
    private val holdFor: Duration = Duration.ofSeconds(30),
    private val clock: Clock = Clock.systemUTC(),
) : BoxOffice {
    override suspend fun tally(): Tally =
        database.read { sql ->
            sql
                .query(
                    """
                    SELECT count(*),
                           count(*) FILTER (WHERE sold_at IS NOT NULL),
                           count(*) FILTER (WHERE sold_at IS NULL AND held_until > ?)
                    FROM seats
                    """.trimIndent(),
                    clock.millis(),
                ) { Tally(free = it.getInt(1) - it.getInt(2) - it.getInt(3), held = it.getInt(3), sold = it.getInt(2)) }
                .single()
        }

    override suspend fun hold(): Hold =
        database.write { sql ->
            val now = clock.millis()
            val until = now + holdFor.toMillis()
            val token = HoldToken(UUID.randomUUID().toString())
            sql
                .query(
                    """
                    UPDATE seats SET hold = ?, held_until = ?
                    WHERE number = (
                        SELECT number FROM seats
                        WHERE sold_at IS NULL AND (hold IS NULL OR held_until <= ?)
                        ORDER BY number LIMIT 1
                    )
                    RETURNING number
                    """.trimIndent(),
                    token.toString(),
                    until,
                    now,
                ) { Hold(token, it.getInt(1), Instant.ofEpochMilli(until)) }
                .firstOrNull() ?: throw SoldOut()
        }

    override suspend fun confirm(token: HoldToken): Ticket =
        database.write { sql ->
            val now = clock.millis()
            sql
                .query(
                    """
                    UPDATE seats SET sold_at = ?, held_until = NULL
                    WHERE hold = ? AND sold_at IS NULL AND held_until > ?
                    RETURNING number
                    """.trimIndent(),
                    now,
                    token.toString(),
                    now,
                ) { Ticket(it.getInt(1)) }
                .firstOrNull()
                ?: sql
                    .query("SELECT number FROM seats WHERE hold = ? AND sold_at IS NOT NULL", token.toString()) {
                        Ticket(it.getInt(1))
                    }.firstOrNull()
                ?: throw HoldExpired()
        }
}
