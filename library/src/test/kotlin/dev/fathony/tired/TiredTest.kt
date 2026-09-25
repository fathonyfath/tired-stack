package dev.fathony.tired

import dev.fathony.tired.ktml.KtmlView
import dev.fathony.tired.ktml.page
import dev.fathony.tired.ktml.respondView
import dev.fathony.tired.ktml.sendView
import dev.ktml.Content
import dev.ktml.KtmlRegistry
import dev.ktml.TagDefinition
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.withCharset
import io.ktor.resources.Resource
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.flow.first
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.ktor.server.sse.SSE as ServerSSE

data class Greeting(
    val name: String,
) : KtmlView {
    override val template = "greeting"
}

@Resource("/greet")
class Greet

/**
 * Stands in for the registry KTML generates from an app's templates.
 */
private val registry =
    object : KtmlRegistry {
        private val templates: Map<String, Content> =
            mapOf(
                "greeting" to {
                    raw("<p>Hello, ")
                    write(required<Greeting>("view").name)
                    raw("</p>")
                },
            )

        override val paths = templates.keys.toList()
        override val tags = emptyList<TagDefinition>()

        override fun get(path: String) = templates[path]
    }

private fun Application.tired() = installTired(registry)

class TiredTest {
    @AfterTest
    fun tearDown() {
        System.clearProperty("tired.dev")
    }

    @Test
    fun `respondView renders the view as utf-8 html`() =
        testApplication {
            application {
                tired()
                routing { get("/") { call.respondView(Greeting("Ktor")) } }
            }

            val response = client.get("/")

            assertEquals(
                ContentType.Text.Html
                    .withCharset(Charsets.UTF_8)
                    .toString(),
                response.headers[HttpHeaders.ContentType],
            )
            assertEquals("<p>Hello, Ktor</p>", response.bodyAsText())
        }

    @Test
    fun `page routes a typed resource to its view`() =
        testApplication {
            application {
                tired()
                install(Resources)
                routing { page<Greet> { Greeting("page") } }
            }

            assertEquals("<p>Hello, page</p>", client.get("/greet").bodyAsText())
        }

    @Test
    fun `sendView sends the rendered view as a named event`() =
        testApplication {
            application {
                tired()
                install(ServerSSE)
                routing { sse("/events") { sendView("greeting", Greeting("sse")) } }
            }

            createClient { install(SSE) }.sse("/events") {
                val event = incoming.first()
                assertEquals("greeting", event.event)
                assertEquals("<p>Hello, sse</p>", event.data)
            }
        }

    @Test
    fun `static assets are uncached under run`() =
        testApplication {
            System.setProperty("tired.dev", "true")
            application { tired() }

            val response = client.get("/app.css")

            assertEquals("body { margin: 0; }\n", response.bodyAsText())
            assertEquals("no-cache", response.headers[HttpHeaders.CacheControl])
        }

    @Test
    fun `static assets keep default caching in production`() =
        testApplication {
            application { tired() }

            val response = client.get("/app.css")

            assertEquals("body { margin: 0; }\n", response.bodyAsText())
            assertNull(response.headers[HttpHeaders.CacheControl])
        }
}
