package dev.fathony.tired.pages

import io.ktor.htmx.html.hx
import io.ktor.utils.io.ExperimentalKtorApi
import io.ktor.utils.io.InternalAPI
import kotlinx.html.*

@OptIn(InternalAPI::class, ExperimentalKtorApi::class)
fun FlowContent.htmxTest() {
    div {
        h1 { +"HTMX Tester" }
        p {
            +"Try opening "
            code { +"/htmx" }
            +" to see the handler if it is accessed through browser."
        }
        a(href = "/htmx") {
            b { +"[click here]" }
        }
        p { +"Now to use the HTMX mechanism, click the button below to update the content" }
        a(href = "/htmx") {
            attributes.hx {
                get = "/htmx"
                trigger = "click"
                target = "#htmx"
                swap = "afterend"
            }
            b { +"[HTMX-Boosted Click Here]" }
        }
        div {
            id = "htmx"
            style = "padding: 8px; border: solid; margin-top: 8px;"
            +"Will replace this!"
        }
    }
}
