package dev.fathony.tired.webassets

import org.gradle.api.Project

internal const val WEB_ASSETS_BUILD_DIR = "tired/web-assets"

/**
 * Keeps project-relative paths, so paths between source folders work in the copy.
 */
internal const val COPIED_SOURCES = "sources"

/**
 * Unminified assets with stable names under `./gradlew run`.
 */
internal val Project.isRunBuild: Boolean
    get() = gradle.startParameter.taskNames.any { it == "run" || it.endsWith(":run") }

internal fun Project.scriptArgs(
    input: String,
    output: String,
    extraArgs: List<String> = emptyList(),
): List<String> =
    buildList {
        addAll(listOf("--input", input, "--outdir", "dist/$output", "--meta", "dist/meta/$output.meta"))
        addAll(extraArgs)
        if (!isRunBuild) add("--minify")
    }

internal fun Project.buildScriptArgs(
    script: String,
    input: String,
    output: String,
    extraArgs: List<String> = emptyList(),
): List<String> = listOf("run", "build:$script", "--") + scriptArgs(input, output, extraArgs)
