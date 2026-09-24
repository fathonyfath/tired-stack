package dev.fathony.tired

import dev.fathony.tired.assets.staticAssets
import dev.ktml.KtmlRegistry
import dev.ktml.ktor.KtmlPlugin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE

/**
 * Installs what the tired helpers rely on and serves the bundled web assets.
 * [registry] defaults to the one KTML generates from `src/main/ktml`.
 */
fun Application.installTired(registry: KtmlRegistry? = null) {
    install(KtmlPlugin) {
        registry?.let { this.registry = it }
    }
    install(Resources)
    install(SSE)
    routing {
        staticAssets()
    }
}
