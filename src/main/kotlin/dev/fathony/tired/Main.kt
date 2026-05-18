package dev.fathony.tired

import dev.fathony.tired.components.icon
import dev.fathony.tired.layout.layout
import dev.fathony.tired.layout.page
import dev.fathony.tired.pages.htmxTest
import dev.fathony.tired.routing.Htmx
import dev.fathony.tired.routing.HtmxTest
import dev.fathony.tired.routing.Index
import io.ktor.htmx.HxResponseHeaders
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.engine.embeddedServer
import io.ktor.http.ContentType
import dev.fathony.tired.plugins.respondHtml
import io.ktor.server.htmx.hx
import io.ktor.server.response.respondText
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.p
import kotlinx.html.stream.createHTML


@OptIn(ExperimentalKtorApi::class)
fun main() {
    embeddedServer(Netty, port = 3000) {
        install(AutoHeadResponse)
//        install(HtmlMinifier)
        install(Resources)
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
        }
    }.start(wait = true)
}
