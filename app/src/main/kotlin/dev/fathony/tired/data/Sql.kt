package dev.fathony.tired.data

import java.sql.Connection
import java.sql.ResultSet

/**
 * Statements on one connection, with every value bound, never spliced into the text.
 */
class Sql(
    private val connection: Connection,
) {
    fun <T> query(
        text: String,
        vararg args: Any?,
        row: (ResultSet) -> T,
    ): List<T> =
        connection.prepareStatement(text).use { statement ->
            args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
            statement.executeQuery().use { rows -> buildList { while (rows.next()) add(row(rows)) } }
        }

    fun execute(
        text: String,
        vararg args: Any?,
    ): Int =
        connection.prepareStatement(text).use { statement ->
            args.forEachIndexed { index, arg -> statement.setObject(index + 1, arg) }
            statement.executeUpdate()
        }
}
