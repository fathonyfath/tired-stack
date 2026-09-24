package dev.fathony.tired.webassets

import groovy.json.JsonOutput
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class WebAssetsExtension {
    /**
     * Defaults to `src/main/web`.
     */
    abstract val sourceDir: DirectoryProperty

    /**
     * Relative to [sourceDir]; skipped if missing.
     */
    abstract val stylesheet: Property<String>

    /**
     * Relative to [sourceDir]; skipped if missing.
     */
    abstract val script: Property<String>

    abstract val npmDependencies: MapProperty<String, String>

    /**
     * As `{"name", "options"}` JSON, in order.
     */
    abstract val postcssPlugins: ListProperty<String>

    /**
     * Copied next to the web sources for PostCSS, so paths like `@source "../kotlin"` resolve.
     */
    abstract val mirroredDirs: ListProperty<String>

    /**
     * Set by the tired plugins, not the app.
     */
    abstract val toolchainParts: SetProperty<ToolchainPart>

    /**
     * Pin an exact version: these have no lockfile.
     */
    fun npm(
        name: String,
        version: String,
    ) {
        npmDependencies.put(name, version)
    }

    /**
     * Runs before esbuild bundles the stylesheet, in the order added.
     */
    fun postcss(
        name: String,
        options: Map<String, Any?> = emptyMap(),
    ) {
        postcssPlugins.add(JsonOutput.toJson(mapOf("name" to name, "options" to options)))
    }

    fun mirror(vararg paths: String) {
        mirroredDirs.set(mirroredDirs.get() + paths)
    }
}
