package dev.fathony.tired.nativeimage

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
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
 * Starts the native executable and requests [paths] from it. What a native image is missing only shows when the code
 * that needs it runs, so a page that renders on the JVM can still fail here.
 */
@DisableCachingByDefault(because = "It checks the executable on this machine")
abstract class NativeSmokeTest : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val executable: RegularFileProperty

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

    @TaskAction
    fun smokeTest() {
        val port = port.get()
        requireFree(port)

        val log = log.get().asFile
        val app =
            ProcessBuilder(executable.get().asFile.absolutePath)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start()
        try {
            val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()
            awaitStart(app, client, port, log)
            val failed =
                paths.get().mapNotNull { path ->
                    val status = client.status(port, path)
                    logger.lifecycle("$path answered $status")
                    "$path answered $status".takeIf { status >= 400 }
                }
            if (failed.isNotEmpty()) {
                throw GradleException("The native image failed its smoke test: ${failed.joinToString()}.\n${tail(log)}")
            }
        } finally {
            app.destroy()
            app.waitFor()
        }
    }

    /**
     * Otherwise the requests would be answered by whatever already listens there.
     */
    private fun requireFree(port: Int) {
        try {
            ServerSocket(port).close()
        } catch (_: IOException) {
            throw GradleException(
                "Port $port is in use, so the native image can't be started on it. Is the app already running?",
            )
        }
    }

    private fun awaitStart(
        app: Process,
        client: HttpClient,
        port: Int,
        log: File,
    ) {
        val deadline = System.nanoTime() + Duration.ofSeconds(startTimeoutSeconds.get().toLong()).toNanos()
        while (true) {
            if (!app.isAlive) {
                throw GradleException(
                    "The native image exited with ${app.exitValue()} before answering on port $port.\n${tail(log)}",
                )
            }
            try {
                client.status(port, "/")
                return
            } catch (_: IOException) {
                if (System.nanoTime() > deadline) {
                    throw GradleException("The native image didn't answer on port $port in time.\n${tail(log)}")
                }
                Thread.sleep(50)
            }
        }
    }

    private fun HttpClient.status(
        port: Int,
        path: String,
    ): Int {
        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path")).timeout(Duration.ofSeconds(10)).build()
        return send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
    }

    private fun tail(log: File) =
        (log.readLines().takeLast(100) + "The app's whole output is in $log").joinToString("\n")
}
