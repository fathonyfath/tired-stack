package dev.fathony.tired.layout

import kotlinx.html.BODY
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.lang
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.title


fun HTML.layout(
    name: String? = null,
    css: Boolean = false,
    js: Boolean = false,
    content: BODY.() -> Unit = {},
) {
    lang = "en"
    head {
        meta(charset = "utf-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1")
        title { name?.let { +it } }
        if (css) link(rel = "stylesheet", href = "stylesheet.css")
        if (js) script(src = "index.js") { defer = true }
    }
    body {
        content()
    }
}
