import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("com.github.node-gradle.node")
}

val webAssetsDir = "${project.projectDir}/web-assets"

node {
    download = true
    version = "24.15.0"
    nodeProjectDir = file(webAssetsDir)
}

val isDev = gradle.startParameter.taskNames.any { it == "run" || it.endsWith(":run") }

val npmInstallPackage by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmSetup"))
    args = listOf("install", "--save", project.findProperty("package")?.toString() ?: "")
}

val npmBuildCss by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args = buildList {
        addAll(
            listOf(
                "run",
                "build:css",
                "--",
                "--input",
                "src/stylesheet.css",
                "--outdir",
                "dist/stylesheets",
                "--meta",
                "dist/meta/stylesheet.meta"
            )
        )
        if (!isDev) add("--minify")
    }
    inputs.dir("$webAssetsDir/src")
    inputs.dir("${project.projectDir}/src/main/kotlin")
    inputs.file("$webAssetsDir/package.json")
    outputs.dir("$webAssetsDir/dist/stylesheets")
    outputs.file("$webAssetsDir/dist/meta/stylesheet.meta")
}

val npmBuildJs by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args = buildList {
        addAll(
            listOf(
                "run",
                "build:js",
                "--",
                "--input",
                "src/index.js",
                "--outdir",
                "dist/scripts",
                "--meta",
                "dist/meta/index.meta"
            )
        )
        if (!isDev) add("--minify")
    }
    inputs.dir("$webAssetsDir/src")
    inputs.file("$webAssetsDir/scripts/build-js.js")
    inputs.file("$webAssetsDir/package.json")
    outputs.dir("$webAssetsDir/dist/scripts")
    outputs.file("$webAssetsDir/dist/meta/index.meta")
}

val npmBuildSvg by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    val icons = Icons.entries.joinToString(",") { it.lucideIcon }
    args =
        listOf("run", "build:svg", "--", "--icons", icons, "--outdir", "dist/icons", "--meta", "dist/meta/icons.meta")
    inputs.file("$webAssetsDir/scripts/build-icons.js")
    inputs.dir("$webAssetsDir/node_modules/lucide-static/icons")
    outputs.dir("$webAssetsDir/dist/icons")
    outputs.file("$webAssetsDir/dist/meta/icons.meta")
}

val generateAssetManifest by tasks.registering {
    dependsOn(npmBuildCss, npmBuildJs, npmBuildSvg)

    val metaFiles =
        files(npmBuildCss, npmBuildJs, npmBuildSvg)
            .filter { it.extension == "meta" }
    inputs.files(metaFiles)

    val outputDir = layout.buildDirectory.dir("generated/source/webAssets")
    outputs.dir(outputDir)

    doLast {
        val nameMap = mutableMapOf<String, String>()
        metaFiles.forEach { f ->
            val props = java.util.Properties()
            props.load(f.inputStream())
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

        val consts = nameMap.entries.joinToString("\n") { (name, value) ->
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

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    sourceSets.named("main") {
        kotlin.srcDir(generateAssetManifest)
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateAssetManifest)
    from("$webAssetsDir/dist/stylesheets") { into("static") }
    from("$webAssetsDir/dist/scripts") { into("static") }
    from("$webAssetsDir/dist/icons") { into("static") }
}

val prettierSources = fileTree(webAssetsDir) {
    include("src/**/*.js", "src/**/*.css", "scripts/**/*.js")
}

val webDiffFiles: List<String> by lazy {
    diffFiles(rootDir, project.findProperty("diffBase")?.toString(), project.hasProperty("diffOnly"), ".js", ".css")
        .map { it.removePrefix("web-assets/") }
}

val prettierCheck by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args = if (webDiffFiles.isNotEmpty()) listOf("exec", "--", "prettier", "--check") + webDiffFiles
    else listOf("run", "lint")
    inputs.files(prettierSources)
    inputs.file("$webAssetsDir/.prettierrc")
}

val prettierFormat by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args = if (webDiffFiles.isNotEmpty()) listOf("exec", "--", "prettier", "--write") + webDiffFiles
    else listOf("run", "format")
    inputs.files(prettierSources)
    inputs.file("$webAssetsDir/.prettierrc")
}

tasks.named("check") {
    dependsOn(prettierCheck)
}

tasks.named("format") {
    dependsOn(prettierFormat)
}
