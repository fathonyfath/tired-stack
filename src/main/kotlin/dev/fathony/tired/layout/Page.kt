package dev.fathony.tired.layout

import kotlinx.html.*

data class NavItem(val label: String, val href: String)

val defaultNavItems =
    listOf(
        NavItem("Home", "/"),
        NavItem("HTMX Test", "/htmx-test"),
        NavItem("HTMX", "/htmx"),
    )

fun BODY.page(
    title: String? = null,
    navItems: List<NavItem> = defaultNavItems,
    content: MAIN.() -> Unit,
) {
    div(classes = "min-h-screen bg-gray-50 text-gray-900 antialiased") {
        header(classes = "border-b bg-white px-6 py-4") {
            nav(classes = "mx-auto flex max-w-4xl items-center justify-between") {
                span(classes = "text-lg font-semibold tracking-tight") { +(title ?: "") }
                div(classes = "flex items-center gap-6") {
                    navItems.forEach { item ->
                        a(href = item.href, classes = "text-sm text-gray-600 hover:text-gray-900") {
                            +item.label
                        }
                    }
                }
            }
        }
        main(classes = "mx-auto max-w-4xl px-6 py-8") {
            content()
        }
    }
}
