package dev.fathony.tired.assets

import io.ktor.http.HttpHeaders
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route

/**
 * Set by the tired plugin on `./gradlew run`.
 */
val isTiredDev: Boolean
    get() = System.getProperty("tired.dev") == "true"

/**
 * Uncached under `./gradlew run`, where assets keep stable names and are rebuilt in place.
 */
fun Route.staticAssets(remotePath: String = "/") =
    staticResources(remotePath, "static") {
        if (isTiredDev) {
            modify { _, call -> call.response.headers.append(HttpHeaders.CacheControl, "no-cache") }
        }
    }
