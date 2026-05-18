package dev.fathony.tired.plugins

import io.ktor.http.ContentType
import io.ktor.http.withCharset
import kotlin.text.Charsets
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.appendHTML

suspend fun ApplicationCall.respondHtml(
    status: HttpStatusCode = HttpStatusCode.OK,
    block: HTML.() -> Unit,
) {
    val text = buildString {
        append("<!DOCTYPE html>")
        appendHTML(prettyPrint = false).html(block = block)
    }
    respond(TextContent(text, ContentType.Text.Html.withCharset(Charsets.UTF_8), status))
}
