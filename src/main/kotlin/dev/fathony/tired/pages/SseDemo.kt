package dev.fathony.tired.pages

import kotlinx.html.*

fun FlowContent.sseDemo() {
    div {
        h1(classes = "text-2xl font-bold") { +"SSE Streaming Demo" }
        p(classes = "mt-2 text-gray-600") {
            +"This page uses Server-Sent Events to stream HTML from the server in real time."
        }
        p(classes = "mt-1 text-gray-600") {
            +"The server will send a countdown, then a final message — all pushed over a single persistent connection."
        }

        div(classes = "mt-6") {
            attributes["hx-ext"] = "sse"
            attributes["sse-connect"] = "/sse-demo/stream"

            div {
                id = "sse-messages"
                attributes["sse-swap"] = "message"
                attributes["hx-swap"] = "beforeend"
                classes = setOf("space-y-2")
            }

            div(classes = "mt-4") {
                id = "sse-status"
                attributes["sse-swap"] = "status"
                attributes["hx-swap"] = "innerHTML"

                span(classes = "inline-flex items-center gap-1.5 text-sm text-amber-600") {
                    span(classes = "h-2 w-2 rounded-full bg-amber-500 animate-pulse") {}
                    +"Connecting..."
                }
            }
        }
    }
}
