package dev.fathony.tired.features

import dev.fathony.tired.ktml.KtmlView
import dev.fathony.tired.ktml.page
import dev.fathony.tired.ktml.sendView
import io.ktor.resources.Resource
import io.ktor.server.routing.Route
import io.ktor.server.sse.sse
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Resource("/sse-demo")
class SseDemo

data object SseDemoPage : KtmlView {
    override val template = "pages/sse-demo"
}

data class SseMessageFragment(
    val text: String,
) : KtmlView {
    override val template = "fragments/sse-message"
}

data class SseStatusFragment(
    val text: String,
    val color: StatusColor,
) : KtmlView {
    override val template = "fragments/sse-status"
}

enum class StatusColor(
    val textClass: String,
    val dotClass: String,
) {
    GREEN("text-green-600", "bg-green-500"),
    BLUE("text-blue-600", "bg-blue-500"),
    GRAY("text-gray-600", "bg-gray-500"),
}

fun Route.sseDemo() {
    page<SseDemo> { SseDemoPage }
    sse("/sse-demo/stream") {
        sendView("status", SseStatusFragment("Connected — streaming will begin shortly...", StatusColor.GREEN))
        delay(1.seconds)

        for (i in 5 downTo 1) {
            sendView("message", SseMessageFragment("Countdown: $i..."))
            sendView("status", SseStatusFragment("Streaming — ${i - 1} events remaining", StatusColor.BLUE))
            delay(1.seconds)
        }

        sendView("message", SseMessageFragment("Done! Stream complete."))
        sendView("status", SseStatusFragment("Stream finished", StatusColor.GRAY))
    }
}
