package dev.fathony.tired

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.variant.VariantComputer
import com.github.gradle.node.variant.computeNodeExec
import dev.fathony.tired.internal.TiredVersions
import dev.fathony.tired.webassets.COPIED_SOURCES
import dev.fathony.tired.webassets.PrepareWebAssetsToolchain
import dev.fathony.tired.webassets.Toolchain
import dev.fathony.tired.webassets.ToolchainPart
import dev.fathony.tired.webassets.WEB_ASSETS_BUILD_DIR
import dev.fathony.tired.webassets.WebAssetsExtension
import dev.fathony.tired.webassets.WebAssetsWatcher
import dev.fathony.tired.webassets.buildScriptArgs
import dev.fathony.tired.webassets.isRunBuild
import dev.fathony.tired.webassets.scriptArgs
import groovy.json.JsonOutput
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File
import java.util.Properties

/**
 * esbuild bundles the stylesheet and script into hashed files, exposed as `AssetManifest`.
 */
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.github.node-gradle.node")
}

val webAssets =
    extensions.create<WebAssetsExtension>("webAssets").apply {
        sourceDir.convention(layout.projectDirectory.dir("src/main/web"))
        stylesheet.convention("stylesheet.css")
        script.convention("index.js")
        mirroredDirs.convention(listOf("src/main/kotlin", "src/main/ktml"))
    }

val webAssetsDir = layout.buildDirectory.dir(WEB_ASSETS_BUILD_DIR)

val sourcePath = webAssets.sourceDir.map { projectRelative(it.asFile, "webAssets.sourceDir") }

val usesPostcss = webAssets.postcssPlugins.map { it.isNotEmpty() }

val copiedPaths =
    sourcePath.zip(usesPostcss) { source, postcss -> source to postcss }.zip(webAssets.mirroredDirs) { (source, postcss), mirrored ->
        val extra = if (postcss) mirrored.map { projectRelative(file(it), "webAssets.mirroredDirs") } else emptyList()
        (listOf(source) + extra).distinct()
    }

fun projectRelative(
    dir: File,
    setting: String,
): String {
    val relative = dir.relativeTo(projectDir).invariantSeparatorsPath.trimEnd('/')
    require(relative.isNotEmpty() && !relative.startsWith("..")) { "$setting must be a folder inside the project, but is $dir" }
    return relative
}

node {
    download = true
    version = TiredVersions.NODE
    nodeProjectDir.set(webAssetsDir)
}

val prepareWebAssetsToolchain =
    tasks.register<PrepareWebAssetsToolchain>("prepareWebAssetsToolchain") {
        npmDependencies.set(webAssets.npmDependencies)
        parts.set(
            webAssets.toolchainParts.zip(usesPostcss) { parts, postcss ->
                if (postcss) parts + ToolchainPart.POSTCSS else parts
            },
        )
        toolchainDir.set(webAssetsDir)
        generatedFiles.from(webAssetsDir.map { dir -> (Toolchain.files - Toolchain.LOCKFILE).map { dir.file(it) } })
    }

/**
 * Copied so npm imports resolve from the toolchain's `node_modules`.
 * The project layout is kept, so relative paths between source folders still work.
 */
val syncWebAssetsSources =
    tasks.register<Sync>("syncWebAssetsSources") {
        into(webAssetsDir.map { it.dir(COPIED_SOURCES) })
        /**
         * Pruned to the copied folders while walking.
         */
        from(layout.projectDirectory) {
            include { element ->
                val path = element.relativePath.pathString
                copiedPaths.get().any { it == path || path.startsWith("$it/") || it.startsWith("$path/") }
            }
            includeEmptyDirs = false
        }
        inputs.property("copiedPaths", copiedPaths)
    }

tasks.named("npmInstall") {
    dependsOn(prepareWebAssetsToolchain)
}

fun copiedEntry(entry: Property<String>) = sourcePath.zip(entry) { dir, name -> "$COPIED_SOURCES/$dir/$name" }

fun entryExists(entry: Property<String>) = webAssets.sourceDir.zip(entry) { dir, name -> dir.file(name).asFile.exists() }

val postcssArgs =
    webAssets.postcssPlugins.map { plugins ->
        if (plugins.isEmpty()) emptyList() else listOf("--postcss", plugins.joinToString(",", "[", "]"))
    }

fun NpmTask.buildEntry(
    script: String,
    entry: Property<String>,
    output: String,
    extraArgs: Provider<List<String>> = provider { emptyList() },
) {
    dependsOn(tasks.named("npmInstall"), syncWebAssetsSources)
    args.set(copiedEntry(entry).zip(extraArgs) { path, extra -> buildScriptArgs(script, path, output, extra) })
    inputs.files(syncWebAssetsSources, prepareWebAssetsToolchain)
    outputs.dir(webAssetsDir.map { it.dir("dist/$output") })
    outputs.file(webAssetsDir.map { it.file("dist/meta/$output.meta") })

    val exists = entryExists(entry)
    onlyIf("the entry exists") { exists.get() }
}

val npmBuildCss =
    tasks.register<NpmTask>("npmBuildCss") {
        buildEntry("css", webAssets.stylesheet, "stylesheets", postcssArgs)
    }

val npmBuildJs =
    tasks.register<NpmTask>("npmBuildJs") {
        buildEntry("js", webAssets.script, "scripts")
    }

/**
 * Other plugins add their build tasks as inputs; each `.meta` file becomes constants.
 */
val generateAssetManifest =
    tasks.register("generateAssetManifest") {
        dependsOn(npmBuildCss, npmBuildJs)
        inputs.files(npmBuildCss, npmBuildJs)

        val outputDir = layout.buildDirectory.dir("generated/source/webAssets")
        outputs.dir(outputDir)

        doLast {
            val metaFiles = inputs.files.filter { it.extension == "meta" && it.exists() }.sortedBy { it.name }
            val nameMap = mutableMapOf<String, String>()
            metaFiles.forEach { f ->
                val props = Properties()
                f.inputStream().use { props.load(it) }
                props.entries.forEach { (k, v) ->
                    var name = k.toString().replace(Regex("[^a-zA-Z0-9]"), "_")
                    var counter = 2
                    while (nameMap.containsKey(name)) {
                        name = "${name}_$counter"
                        counter++
                    }
                    nameMap[name] = v.toString()
                }
            }

            val consts =
                nameMap.entries.joinToString("\n") { (name, value) ->
                    "    const val $name = \"$value\""
                }

            val content =
                """
                |object AssetManifest {
                |$consts
                |}
                """.trimMargin()

            val outDir = outputDir.get().asFile
            outDir.mkdirs()
            outDir.resolve("AssetManifest.kt").writeText(content)
        }
    }

extensions.configure<KotlinJvmProjectExtension> {
    sourceSets.named("main") {
        kotlin.srcDir(generateAssetManifest)
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateAssetManifest)
    from(webAssetsDir.map { it.dir("dist/stylesheets") }) { into("static") }
    from(webAssetsDir.map { it.dir("dist/scripts") }) { into("static") }
}

tasks.register<NpmTask>("npmLatest") {
    group = "help"
    description = "Prints the latest version of an npm package, e.g. -Ppackage=basecoat-css, to pin in webAssets { npm(...) }."
    dependsOn(tasks.named("npmSetup"))
    args = listOf("view", project.findProperty("package")?.toString() ?: "", "version")
}

/**
 * Rebuilds assets on change during `run`. They keep stable names there, so they can be replaced in place.
 */
if (isRunBuild) {
    pluginManager.withPlugin("application") {
        val staticDir = the<SourceSetContainer>()["main"].output.resourcesDir!!.resolve("static")
        val nodeBinDir = VariantComputer().computeNodeBinDir(node.resolvedNodeDir, node.resolvedPlatform)
        val nodeExec = computeNodeExec(node, nodeBinDir)
        val command =
            provider {
                fun json(value: Any) = JsonOutput.toJson(value)
                buildList {
                    addAll(listOf(nodeExec.get(), "scripts/watch.js"))
                    addAll(listOf("--project", projectDir.absolutePath))
                    addAll(
                        listOf(
                            "--copy",
                            webAssetsDir
                                .get()
                                .dir(COPIED_SOURCES)
                                .asFile.absolutePath,
                        ),
                    )
                    addAll(listOf("--watch", json(copiedPaths.get()), "--scripts", sourcePath.get()))
                    addAll(listOf("--static", staticDir.absolutePath))
                    if (entryExists(webAssets.stylesheet).get()) {
                        val css = scriptArgs(copiedEntry(webAssets.stylesheet).get(), "stylesheets", postcssArgs.get())
                        addAll(listOf("--css", json(listOf("scripts/build-css.js") + css)))
                    }
                    if (entryExists(webAssets.script).get()) {
                        val js = scriptArgs(copiedEntry(webAssets.script).get(), "scripts")
                        addAll(listOf("--js", json(listOf("scripts/build-js.js") + js)))
                    }
                }
            }
        val watcher =
            gradle.sharedServices.registerIfAbsent("tiredWebAssetsWatcher${project.path}", WebAssetsWatcher::class) {
                parameters.command.set(command)
                parameters.workingDir.set(webAssetsDir)
            }

        tasks.named<JavaExec>("run") {
            usesService(watcher)
            doFirst { watcher.get().start() }
        }
    }
}
