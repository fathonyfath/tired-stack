package dev.fathony.tired

import dev.fathony.tired.features.home
import dev.fathony.tired.features.htmxDemo
import dev.fathony.tired.features.sseDemo
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 3000) {
        installTired()
        install(AutoHeadResponse)
        routing {
            home()
            htmxDemo()
            sseDemo()
        }
    }.start(wait = true)
}
