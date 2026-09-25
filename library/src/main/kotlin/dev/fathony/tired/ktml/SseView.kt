package dev.fathony.tired.ktml

import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent

/**
 * Kept apart from the other helpers so apps without `ktor-server-sse` never load it.
 */
suspend fun ServerSSESession.sendView(
    event: String,
    view: KtmlView,
) = send(ServerSentEvent(data = call.application.renderView(view).trim(), event = event))
