package dev.fathony.tired

import dev.fathony.tired.components.icon
import dev.fathony.tired.layout.layout
import dev.fathony.tired.layout.page
import dev.fathony.tired.pages.htmxTest
import dev.fathony.tired.pages.sseDemo
import dev.fathony.tired.plugins.respondHtml
import dev.fathony.tired.routing.Htmx
import dev.fathony.tired.routing.HtmxTest
import dev.fathony.tired.routing.Index
import dev.fathony.tired.routing.SseDemo
import io.ktor.htmx.HxResponseHeaders
import io.ktor.http.ContentType
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.htmx.hx
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.coroutines.delay
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalKtorApi::class)
fun main() {
    embeddedServer(Netty, port = 3000) {
        install(AutoHeadResponse)
        install(Resources)
        install(SSE)
        routing {
            staticResources("/", "static")
            get<Index> {
                call.respondHtml {
                    layout(name = "Hello world!", css = true, js = true) {
                        page(title = "Hello world!") {
                            h1(classes = "flex items-center gap-2 text-2xl font-bold") {
                                icon(Icons.Search)
                                +"Hello world!"
                            }
                            p(classes = "mt-2") { +"Welcome to tired stack." }
                        }
                    }
                }
            }
            get<HtmxTest> {
                call.respondHtml {
                    layout(name = "HTMX Test", css = true, js = true) {
                        page(title = "HTMX Test") {
                            htmxTest()
                        }
                    }
                }
            }
            hx {
                get<Htmx> {
                    call.response.hx[HxResponseHeaders.Reswap] = "outerHTML"

                    call.respondText(ContentType.Text.Html) {
                        createHTML().div(classes = "p-4 border border-green-400 bg-green-50 mt-4") {
                            id = "htmx"
                            +"HTMX fragment response — swapped in at ${System.currentTimeMillis()}"
                        }
                    }
                }
            }
            get<Htmx> {
                call.respondHtml {
                    layout(name = "HTMX", css = true, js = true) {
                        page(title = "HTMX") {
                            p { +"This is the full page response for /htmx (browser access)." }
                        }
                    }
                }
            }
            get<SseDemo> {
                call.respondHtml {
                    layout(name = "SSE Demo", css = true, js = true) {
                        page(title = "SSE Demo") {
                            sseDemo()
                        }
                    }
                }
            }
            sse("/sse-demo/stream") {
                send(
                    ServerSentEvent(
                        data = statusHtml("Connected — streaming will begin shortly...", StatusColor.GREEN),
                        event = "status",
                    ),
                )
                delay(1.seconds)

                for (i in 5 downTo 1) {
                    send(ServerSentEvent(data = messageHtml("Countdown: $i..."), event = "message"))
                    send(
                        ServerSentEvent(
                            data = statusHtml("Streaming — ${i - 1} events remaining", StatusColor.BLUE),
                            event = "status",
                        ),
                    )
                    delay(1.seconds)
                }

                send(ServerSentEvent(data = messageHtml("Done! Stream complete."), event = "message"))
                send(ServerSentEvent(data = statusHtml("Stream finished", StatusColor.GRAY), event = "status"))
            }
        }
    }.start(wait = true)
}

private fun messageHtml(text: String): String =
    createHTML(
        prettyPrint = false,
    ).div(classes = "rounded-md border border-blue-200 bg-blue-50 px-4 py-2 text-sm text-blue-800") {
        +text
    }

private enum class StatusColor(
    val textClass: String,
    val dotClass: String,
) {
    GREEN("text-green-600", "bg-green-500"),
    BLUE("text-blue-600", "bg-blue-500"),
    GRAY("text-gray-600", "bg-gray-500"),
}

private fun statusHtml(
    text: String,
    color: StatusColor,
): String =
    createHTML(prettyPrint = false).span(classes = "inline-flex items-center gap-1.5 text-sm ${color.textClass}") {
        span(classes = "h-2 w-2 rounded-full ${color.dotClass}") {}
        +text
    }
