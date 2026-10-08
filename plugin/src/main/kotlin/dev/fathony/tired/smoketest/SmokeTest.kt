package dev.fathony.tired.smoketest

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Starts the app with [command] and requests [paths] from it, along with the stylesheets, scripts and icons those
 * pages link to. It fails when the app doesn't start or anything answers with an error.
 */
@DisableCachingByDefault(because = "It checks that the app runs on this machine")
abstract class SmokeTest : DefaultTask() {
    @get:Input
    abstract val command: ListProperty<String>

    /**
     * What [command] runs, so the task is only up to date while the app is unchanged.
     */
    @get:Classpath
    abstract val app: ConfigurableFileCollection

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val paths: ListProperty<String>

    @get:Input
    abstract val startTimeoutSeconds: Property<Int>

    /**
     * Everything the app printed.
     */
    @get:OutputFile
    abstract val log: RegularFileProperty

    fun configureFrom(settings: SmokeTestExtension) {
        port.convention(settings.port)
        paths.convention(settings.paths)
        startTimeoutSeconds.convention(settings.startTimeoutSeconds)
    }

    @TaskAction
    fun smokeTest() {
        val root = URI("http://127.0.0.1:${port.get()}/")
        requireFree(port.get())

        val log = log.get().asFile
        val process =
            ProcessBuilder(command.get())
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start()
        try {
            val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()
            awaitStart(process, client, root, log)

            val pages = paths.get().map { root.resolve(it) }
            val assets =
                pages.flatMap { page ->
                    ASSET_LINK.findAll(client.get(page).body()).map { page.resolve(it.groupValues[1]) }
                }
            val failed =
                (pages + assets).distinct().mapNotNull { uri ->
                    val status = client.get(uri).statusCode()
                    val answer = "${uri.rawPath}${uri.rawQuery?.let { "?$it" }.orEmpty()} answered $status"
                    logger.lifecycle(answer)
                    answer.takeIf { status >= 400 }
                }
            if (failed.isNotEmpty()) {
                throw GradleException("The app failed its smoke test: ${failed.joinToString()}.\n${tail(log)}")
            }
        } finally {
            process.destroy()
            process.waitFor()
        }
    }

    /**
     * Otherwise the requests would be answered by whatever already listens there.
     */
    private fun requireFree(port: Int) {
        try {
            ServerSocket(port).close()
        } catch (_: IOException) {
            throw GradleException("Port $port is in use, so the app can't be started on it. Is it already running?")
        }
    }

    private fun awaitStart(
        process: Process,
        client: HttpClient,
        root: URI,
        log: File,
    ) {
        val deadline = System.nanoTime() + Duration.ofSeconds(startTimeoutSeconds.get().toLong()).toNanos()
        while (true) {
            if (!process.isAlive) {
                throw GradleException(
                    "The app exited with ${process.exitValue()} before answering on port ${root.port}.\n${tail(log)}",
                )
            }
            try {
                client.get(root)
                return
            } catch (_: IOException) {
                if (System.nanoTime() > deadline) {
                    throw GradleException("The app didn't answer on port ${root.port} in time.\n${tail(log)}")
                }
                Thread.sleep(50)
            }
        }
    }

    private fun HttpClient.get(uri: URI): HttpResponse<String> =
        send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString())

    private fun tail(log: File) =
        (log.readLines().takeLast(100) + "The app's whole output is in $log").joinToString("\n")

    private companion object {
        /**
         * The fragment is left off: an icon is referenced as one symbol of the sprite.
         */
        val ASSET_LINK = Regex("""(?:href|src)="([^"#]+\.(?:css|js|svg))[^"]*"""")
    }
}
