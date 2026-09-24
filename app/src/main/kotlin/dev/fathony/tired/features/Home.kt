package dev.fathony.tired.features

import Icons
import dev.fathony.tired.ktml.KtmlView
import dev.fathony.tired.ktml.page
import io.ktor.resources.Resource
import io.ktor.server.routing.Route

@Resource("/")
class Home

data class HomePage(
    val title: String,
    val icon: Icons,
) : KtmlView {
    override val template = "pages/home"
}

fun Route.home() = page<Home> { HomePage(title = "Hello world!", icon = Icons.Search) }
