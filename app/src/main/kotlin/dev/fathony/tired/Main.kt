package dev.fathony.tired

import dev.fathony.tired.features.home
import dev.fathony.tired.features.htmxDemo
import dev.fathony.tired.features.sseDemo
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE

fun main() {
    embeddedServer(Netty, port = 3000) {
        installTired()
        install(Resources)
        install(SSE)
        install(AutoHeadResponse)
        routing {
            home()
            htmxDemo()
            sseDemo()
        }
    }.start(wait = true)
}
