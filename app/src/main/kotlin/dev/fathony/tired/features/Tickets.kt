package dev.fathony.tired.features

import dev.fathony.tired.data.Refused
import dev.fathony.tired.data.tickets.BoxOffice
import dev.fathony.tired.data.tickets.Hold
import dev.fathony.tired.data.tickets.HoldToken
import dev.fathony.tired.data.tickets.Tally
import dev.fathony.tired.data.tickets.Ticket
import dev.fathony.tired.ktml.KtmlView
import dev.fathony.tired.ktml.page
import dev.fathony.tired.ktml.respondView
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

@Resource("/tickets")
class Tickets {
    @Resource("holds")
    class Holds(
        val parent: Tickets = Tickets(),
    ) {
        @Resource("{token}/confirm")
        class Confirm(
            val parent: Holds = Holds(),
            val token: String,
        )
    }
}

data class TicketsPage(
    val panel: TicketsPanelFragment,
) : KtmlView {
    override val template = "pages/tickets"
}

/**
 * At most one of [hold], [ticket] and [refusal] is set: what the last request led to.
 */
data class TicketsPanelFragment(
    val tally: Tally,
    val hold: Hold? = null,
    val ticket: Ticket? = null,
    val refusal: String? = null,
) : KtmlView {
    override val template = "fragments/tickets-panel"
}

fun Route.tickets(boxOffice: BoxOffice) {
    page<Tickets> { TicketsPage(TicketsPanelFragment(boxOffice.tally())) }

    /**
     * A refusal is still a 200, so HTMX swaps the reason in.
     */
    post<Tickets.Holds> {
        val hold =
            try {
                boxOffice.hold()
            } catch (refused: Refused) {
                call.respondView(TicketsPanelFragment(boxOffice.tally(), refusal = refused.message))
                return@post
            }
        call.respondView(TicketsPanelFragment(boxOffice.tally(), hold = hold))
    }

    post<Tickets.Holds.Confirm> { request ->
        val token =
            try {
                HoldToken(request.token)
            } catch (_: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
        val ticket =
            try {
                boxOffice.confirm(token)
            } catch (refused: Refused) {
                call.respondView(TicketsPanelFragment(boxOffice.tally(), refusal = refused.message))
                return@post
            }
        call.respondView(TicketsPanelFragment(boxOffice.tally(), ticket = ticket))
    }
}
