package dev.fathony.tired.data

/**
 * The tables refuse what the code should never send, so a bug in it cannot leave them wrong:
 * an entry that overdraws, does not add up, does not continue from its account's latest one or repeats a posting,
 * any change to an entry once written, and a seat that is sold twice or held and sold at once.
 */
class Schema(
    private val database: Database,
) {
    private val statements =
        listOf(
            "CREATE TABLE contacts (id INTEGER PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL)",
            "CREATE TABLE accounts (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE)",
            """
            CREATE TABLE entries (
                id INTEGER PRIMARY KEY,
                account_id INTEGER NOT NULL REFERENCES accounts (id),
                posting TEXT NOT NULL,
                at INTEGER NOT NULL,
                previous INTEGER NOT NULL,
                change INTEGER NOT NULL CHECK (change <> 0),
                balance INTEGER NOT NULL CHECK (balance >= 0 AND balance = previous + change),
                UNIQUE (posting, account_id)
            )
            """.trimIndent(),
            "CREATE INDEX entries_by_account ON entries (account_id, id)",
            """
            CREATE TRIGGER entries_continue BEFORE INSERT ON entries
            BEGIN
                SELECT RAISE(ABORT, 'entry does not continue from the latest balance')
                WHERE NEW.previous <> COALESCE(
                    (SELECT balance FROM entries WHERE account_id = NEW.account_id ORDER BY id DESC LIMIT 1), 0
                );
            END
            """.trimIndent(),
            """
            CREATE TRIGGER entries_unchanged BEFORE UPDATE ON entries
            BEGIN
                SELECT RAISE(ABORT, 'an entry is never changed');
            END
            """.trimIndent(),
            """
            CREATE TRIGGER entries_kept BEFORE DELETE ON entries
            BEGIN
                SELECT RAISE(ABORT, 'an entry is never removed');
            END
            """.trimIndent(),
            """
            CREATE TABLE seats (
                number INTEGER PRIMARY KEY,
                hold TEXT UNIQUE,
                held_until INTEGER,
                sold_at INTEGER,
                CHECK (held_until IS NULL OR sold_at IS NULL),
                CHECK ((hold IS NULL) = (held_until IS NULL AND sold_at IS NULL))
            )
            """.trimIndent(),
            """
            CREATE TRIGGER seats_sold_once BEFORE UPDATE ON seats WHEN OLD.sold_at IS NOT NULL
            BEGIN
                SELECT RAISE(ABORT, 'a sold seat stays sold');
            END
            """.trimIndent(),
        )

    suspend fun create() = database.write { sql -> statements.forEach { sql.execute(it) } }
}
