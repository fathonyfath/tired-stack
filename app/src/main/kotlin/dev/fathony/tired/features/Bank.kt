package dev.fathony.tired.features

import dev.fathony.tired.data.Refused
import dev.fathony.tired.data.bank.AccountId
import dev.fathony.tired.data.bank.Amount
import dev.fathony.tired.data.bank.BankStatement
import dev.fathony.tired.data.bank.Ledger
import dev.fathony.tired.data.bank.Leg
import dev.fathony.tired.data.bank.Posting
import dev.fathony.tired.data.bank.PostingKey
import dev.fathony.tired.ktml.KtmlView
import dev.fathony.tired.ktml.page
import dev.fathony.tired.ktml.respondView
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.resources.Resource
import io.ktor.server.request.receiveParameters
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import java.util.UUID

@Resource("/bank")
class Bank

data class BankPage(
    val panel: BankPanelFragment,
) : KtmlView {
    override val template = "pages/bank"
}

/**
 * Each form carries its own fresh key, so a form sent twice is applied once.
 * [refusal] says why the last posting was not applied, when it was not.
 */
data class BankPanelFragment(
    val statement: BankStatement,
    val movementKey: String,
    val transferKey: String,
    val refusal: String? = null,
) : KtmlView {
    override val template = "fragments/bank-panel"
}

private const val SHOWN_ENTRIES = 20

fun Route.bank(ledger: Ledger) {
    suspend fun panel(refusal: String? = null) =
        BankPanelFragment(
            statement = ledger.statement(SHOWN_ENTRIES),
            movementKey = UUID.randomUUID().toString(),
            transferKey = UUID.randomUUID().toString(),
            refusal = refusal,
        )

    page<Bank> { BankPage(panel()) }

    /**
     * A refused posting is still a 200, so HTMX swaps the reason in.
     */
    post<Bank> {
        val posting =
            try {
                posting(call.receiveParameters())
            } catch (_: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
        val refusal =
            try {
                ledger.post(posting)
                null
            } catch (refused: Refused) {
                refused.message
            }
        call.respondView(panel(refusal))
    }
}

private fun posting(form: Parameters): Posting {
    fun account(field: String) = AccountId(requireNotNull(form[field]?.toLongOrNull()))
    val amount = Amount(requireNotNull(form["amount"]?.toLongOrNull()))
    val legs =
        when (form["kind"]) {
            "deposit" -> listOf(Leg(account("account"), amount.added()))
            "withdraw" -> listOf(Leg(account("account"), amount.removed()))
            "transfer" -> listOf(Leg(account("from"), amount.removed()), Leg(account("to"), amount.added()))
            else -> throw IllegalArgumentException("Not a kind of posting: ${form["kind"]}")
        }
    return Posting(PostingKey(form["key"].orEmpty()), legs)
}
