package dev.fathony.tired.features

import dev.fathony.tired.data.Name
import dev.fathony.tired.data.contacts.AddressBook
import dev.fathony.tired.data.contacts.Contact
import dev.fathony.tired.data.contacts.Email
import dev.fathony.tired.ktml.KtmlView
import dev.fathony.tired.ktml.page
import dev.fathony.tired.ktml.respondView
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.htmx.hx
import io.ktor.server.request.receiveParameters
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.href
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.utils.io.ExperimentalKtorApi

@Resource("/contacts")
class Contacts(
    val q: String = "",
    val after: Long? = null,
) {
    @Resource("{id}")
    class ById(
        val parent: Contacts = Contacts(),
        val id: Long,
    )
}

data class ContactsPage(
    val query: String,
    val rows: ContactRowsFragment,
) : KtmlView {
    override val template = "pages/contacts"
}

/**
 * [next] is where the following page loads from, when there is one.
 */
data class ContactRowsFragment(
    val contacts: List<Contact>,
    val next: String? = null,
) : KtmlView {
    override val template = "fragments/contact-rows"
}

private const val PAGE_SIZE = 20
private const val MAX_QUERY_LENGTH = 100

@OptIn(ExperimentalKtorApi::class)
fun Route.contacts(book: AddressBook) {
    /**
     * One row past the page tells whether another page follows.
     */
    suspend fun rows(request: Contacts): ContactRowsFragment {
        val query = request.q.trim().take(MAX_QUERY_LENGTH)
        val matched = if (query.isEmpty()) book else book.matching(query)
        val found = (request.after?.let(matched::olderThan) ?: matched).newest(PAGE_SIZE + 1)
        val shown = found.take(PAGE_SIZE)
        val next = if (found.size > PAGE_SIZE) application.href(Contacts(query, shown.last().id)) else null
        return ContactRowsFragment(shown, next)
    }

    /**
     * Registered before the full page, so searching and scrolling get only the rows.
     */
    hx {
        get<Contacts> { call.respondView(rows(it)) }
    }
    page<Contacts> { ContactsPage(query = it.q, rows = rows(it)) }

    post<Contacts> {
        val form = call.receiveParameters()
        val (name, email) =
            try {
                Name(form["name"].orEmpty().trim()) to Email(form["email"].orEmpty().trim())
            } catch (_: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
        call.respondView(ContactRowsFragment(listOf(book.add(name, email))))
    }

    /**
     * An empty 200, because HTMX skips the swap that removes the row on a 204.
     */
    delete<Contacts.ById> {
        book.remove(it.id)
        call.respondText("")
    }
}
