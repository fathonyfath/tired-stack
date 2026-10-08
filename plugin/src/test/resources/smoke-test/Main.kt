import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

val pages =
    mapOf(
        "/" to """<link rel="stylesheet" href="app.css">""",
        "/app.css" to "body {}",
        "/unstyled" to """<link rel="stylesheet" href="missing.css">""",
    )

fun main() {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", __PORT__), 0)
    server.createContext("/") { exchange ->
        val body = pages[exchange.requestURI.path]?.toByteArray()
        exchange.sendResponseHeaders(if (body == null) 404 else 200, body?.size?.toLong() ?: -1)
        body?.let { exchange.responseBody.write(it) }
        exchange.close()
    }
    server.start()
}
