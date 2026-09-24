package dev.fathony.tired.webassets

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import kotlin.concurrent.thread

/**
 * Closed when the build ends, so the watcher stops with `run`.
 */
abstract class WebAssetsWatcher :
    BuildService<WebAssetsWatcher.Params>,
    AutoCloseable {
    interface Params : BuildServiceParameters {
        val command: ListProperty<String>
        val workingDir: DirectoryProperty
    }

    private val logger = Logging.getLogger(WebAssetsWatcher::class.java)
    private var process: Process? = null

    @Synchronized
    fun start() {
        if (process != null) return
        val started =
            ProcessBuilder(parameters.command.get())
                .directory(parameters.workingDir.get().asFile)
                .redirectErrorStream(true)
                .start()
        thread(isDaemon = true, name = "tired-web-assets-watcher") {
            started.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        }
        process = started
    }

    @Synchronized
    override fun close() {
        process?.destroy()
        process = null
    }
}
