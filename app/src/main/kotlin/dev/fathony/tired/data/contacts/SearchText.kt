package dev.fathony.tired.data.contacts

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import dev.fathony.tired.data.Name
import java.text.Normalizer

private val accents = Regex("\\p{M}+")

/**
 * Lower case with the accents removed, so that "jose" finds "José".
 */
fun folded(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD).replace(accents, "").lowercase()

/**
 * What a search for a contact is matched against.
 */
fun searchText(
    name: Name,
    email: Email,
): String = folded("$name $email")

/**
 * Runs once on a database that had contacts before `4.sqm` gave them a search text, and fills it in.
 * It speaks plain SQL because the generated queries describe the newest tables, not the ones at this step.
 */
val fillSearchText =
    AfterVersion(4) { driver ->
        val contacts =
            driver
                .executeQuery(
                    identifier = null,
                    sql = "SELECT id, name, email FROM contacts",
                    mapper = { rows ->
                        QueryResult.Value(
                            buildList {
                                while (rows.next().value) {
                                    add(rows.getLong(0)!! to folded("${rows.getString(1)} ${rows.getString(2)}"))
                                }
                            },
                        )
                    },
                    parameters = 0,
                ).value
        contacts.forEach { (id, text) ->
            driver.execute(null, "UPDATE contacts SET search_text = ? WHERE id = ?", 2) {
                bindString(0, text)
                bindLong(1, id)
            }
        }
    }
