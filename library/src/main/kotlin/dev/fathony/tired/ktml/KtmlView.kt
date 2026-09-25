package dev.fathony.tired.ktml

import dev.ktml.ContentWriter
import dev.ktml.Context
import dev.ktml.ktor.ktmlEngineKey
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.resources.get
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import java.io.Writer

/**
 * A typed model for a KTML template, which declares it as `@view`.
 */
interface KtmlView {
    val template: String
}

private const val VIEW_KEY = "view"

/**
 * Unlike `respondKtml`, sends a charset, which HTMX fragments need.
 */
suspend fun ApplicationCall.respondView(
    view: KtmlView,
    status: HttpStatusCode = HttpStatusCode.OK,
) = respondTextWriter(ContentType.Text.Html.withCharset(Charsets.UTF_8), status) {
    val context = Context(WriterContentWriter(this), mapOf(VIEW_KEY to view))
    application.attributes[ktmlEngineKey].writePage(context, view.template)
}

/**
 * For responses that aren't a plain HTML body, e.g. SSE events.
 */
suspend fun Application.renderView(view: KtmlView): String =
    attributes[ktmlEngineKey].renderPage(view.template, mapOf(VIEW_KEY to view))

inline fun <reified R : Any> Route.page(noinline view: suspend RoutingContext.(R) -> KtmlView): Route =
    get<R> { call.respondView(view(it)) }

private class WriterContentWriter(
    private val out: Writer,
) : ContentWriter {
    override suspend fun write(
        content: String,
        offset: Int,
        length: Int,
    ) = out.write(content, offset, length)
}
