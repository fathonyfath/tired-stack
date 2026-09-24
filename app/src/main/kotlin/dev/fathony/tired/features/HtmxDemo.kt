package dev.fathony.tired.features

import dev.fathony.tired.ktml.KtmlView
import dev.fathony.tired.ktml.page
import dev.fathony.tired.ktml.respondView
import io.ktor.htmx.HxResponseHeaders
import io.ktor.resources.Resource
import io.ktor.server.htmx.hx
import io.ktor.server.resources.get
import io.ktor.server.routing.Route
import io.ktor.utils.io.ExperimentalKtorApi

@Resource("/htmx-test")
class HtmxTest

@Resource("/htmx")
class Htmx

data object HtmxTestPage : KtmlView {
    override val template = "pages/htmx-test"
}

data object HtmxPage : KtmlView {
    override val template = "pages/htmx"
}

data class HtmxSwapFragment(
    val renderedAt: Long,
) : KtmlView {
    override val template = "fragments/htmx-swap"
}

@OptIn(ExperimentalKtorApi::class)
fun Route.htmxDemo() {
    page<HtmxTest> { HtmxTestPage }
    /**
     * Registered before the full page, so HTMX requests get the fragment.
     */
    hx {
        get<Htmx> {
            call.response.hx[HxResponseHeaders.Reswap] = "outerHTML"
            call.respondView(HtmxSwapFragment(renderedAt = System.currentTimeMillis()))
        }
    }
    page<Htmx> { HtmxPage }
}
