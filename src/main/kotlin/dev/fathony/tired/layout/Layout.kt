package dev.fathony.tired.layout

import kotlinx.html.*

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
        if (css) link(rel = "stylesheet", href = AssetManifest.stylesheet_css)
        if (js) script(src = AssetManifest.index_js) { defer = true }
    }
    body {
        content()
    }
}
