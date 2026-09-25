package dev.fathony.tired

import com.github.gradle.node.npm.task.NpmTask
import dev.fathony.tired.icons.ICON_REFERENCE
import dev.fathony.tired.icons.constantName
import dev.fathony.tired.icons.readLucideIcons
import dev.fathony.tired.internal.readResource
import dev.fathony.tired.webassets.ToolchainPart
import dev.fathony.tired.webassets.WEB_ASSETS_BUILD_DIR
import dev.fathony.tired.webassets.WebAssetsExtension
import dev.fathony.tired.webassets.isRunBuild
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Lucide icons: every icon as an `Icons` constant, one sprite and, with KTML, the `<icon>` tag.
 */
plugins {
    id("dev.fathony.tired.web-assets")
}

the<WebAssetsExtension>().toolchainParts.add(ToolchainPart.ICONS)

val webAssetsDir = layout.buildDirectory.dir(WEB_ASSETS_BUILD_DIR)

val lucideTags = webAssetsDir.map { it.file("node_modules/lucide-static/tags.json") }

/**
 * Generated sources are left out: they're derived from these, and scanning them would depend on the sprite.
 */
val iconSources =
    files(
        provider {
            val kotlinDirs =
                the<KotlinJvmProjectExtension>()
                    .sourceSets
                    .getByName("main")
                    .kotlin.srcDirs
                    .filterNot { it.startsWith(layout.buildDirectory.get().asFile) }
            kotlinDirs + file("src/main/ktml")
        },
    ).asFileTree.matching { include("**/*.kt", "**/*.ktml") }

/**
 * Every icon under `run`, so any `Icons` constant shows up on refresh; otherwise only those the sources reference.
 */
val listIcons =
    tasks.register("listIcons") {
        dependsOn(tasks.named("npmInstall"))
        val tags = lucideTags
        val all = isRunBuild
        val sources = iconSources
        inputs.file(tags)
        inputs.property("all", all)
        if (!all) inputs.files(sources)

        val output = webAssetsDir.map { it.file("icons.list") }
        outputs.file(output)

        doLast {
            val icons = readLucideIcons(tags.get().asFile)
            val used =
                if (all) {
                    icons
                } else {
                    val byConstant = icons.associateBy { constantName(it) }
                    sources.files
                        .flatMap { source -> ICON_REFERENCE.findAll(source.readText()).map { it.groupValues[1] } }
                        .mapNotNull { byConstant[it] }
                        .distinct()
                        .sorted()
                }
            output.get().asFile.writeText(used.joinToString("\n"))
        }
    }

val npmBuildSvg =
    tasks.register<NpmTask>("npmBuildSvg") {
        dependsOn(tasks.named("npmInstall"))
        val stable = if (isRunBuild) listOf("--stable") else emptyList()
        args.set(
            listOf("run", "build:svg", "--", "--icons", "icons.list", "--outdir", "dist/icons", "--meta", "dist/meta/icons.meta") +
                stable,
        )
        inputs.files(listIcons, tasks.named("prepareWebAssetsToolchain"))
        outputs.dir(webAssetsDir.map { it.dir("dist/icons") })
        outputs.file(webAssetsDir.map { it.file("dist/meta/icons.meta") })
    }

val generateIcons =
    tasks.register("generateIcons") {
        dependsOn(tasks.named("npmInstall"))
        val tags = lucideTags
        inputs.file(tags)

        val outputDir = layout.buildDirectory.dir("generated/source/icons")
        outputs.dir(outputDir)

        doLast {
            val entries =
                readLucideIcons(tags.get().asFile).joinToString("\n") { lucideIcon ->
                    "    ${constantName(lucideIcon)}(\"$lucideIcon\"),"
                }

            val content =
                """
                |enum class Icons(
                |    val lucideIcon: String,
                |) {
                |$entries
                |}
                """.trimMargin()

            val outDir = outputDir.get().asFile
            outDir.mkdirs()
            outDir.resolve("Icons.kt").writeText(content)
        }
    }

extensions.configure<KotlinJvmProjectExtension> {
    sourceSets.named("main") {
        kotlin.srcDir(generateIcons)
    }
}

tasks.named("generateAssetManifest") {
    dependsOn(npmBuildSvg)
    inputs.files(npmBuildSvg)
}

tasks.named<ProcessResources>("processResources") {
    from(webAssetsDir.map { it.dir("dist/icons") }) { into("static") }
}

/**
 * KTML only reads `src/main/ktml`, so the tag is written there, into a folder that ignores itself.
 */
pluginManager.withPlugin("dev.ktml.gradle") {
    val iconTagDir = layout.projectDirectory.dir("src/main/ktml/tired")
    val iconTag = readResource("ktml/icon.ktml").decodeToString()

    val generateIconTag =
        tasks.register("generateIconTag") {
            inputs.property("template", iconTag)
            outputs.dir(iconTagDir)

            doLast {
                val dir = iconTagDir.asFile
                dir.mkdirs()
                dir.resolve(".gitignore").writeText("# Generated by the tired Gradle plugin.\n*\n")
                dir.resolve("icon.ktml").writeText(iconTag)
            }
        }

    tasks.matching { it.name == "generateKtml" }.configureEach { dependsOn(generateIconTag) }
    /**
     * Copied along with `src/main/ktml` for PostCSS.
     */
    tasks.named("syncWebAssetsSources") { dependsOn(generateIconTag) }
    listIcons { dependsOn(generateIconTag) }
}
