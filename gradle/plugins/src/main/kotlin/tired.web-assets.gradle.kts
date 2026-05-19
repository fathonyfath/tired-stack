import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("com.github.node-gradle.node")
}

node {
    download = true
    version = "24.15.0"
    nodeProjectDir = file("${project.projectDir}/web-assets")
}

val isDev = gradle.startParameter.taskNames.any { it == "run" || it.endsWith(":run") }

val npmInstallPackage by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmSetup"))
    args = listOf("install", "--save", project.findProperty("package")?.toString() ?: "")
}

val npmBuildCss by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args = buildList {
        addAll(listOf("run", "build:css", "--", "--outdir", "dist/stylesheets"))
        if (!isDev) add("--minify")
    }
    inputs.dir("${project.projectDir}/web-assets/src")
    inputs.dir("${project.projectDir}/src/main/kotlin")
    inputs.file("${project.projectDir}/web-assets/postcss.config.js")
    inputs.file("${project.projectDir}/web-assets/package.json")
    outputs.dir("${project.projectDir}/web-assets/dist/stylesheets")
}

val npmBuildJs by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args = buildList {
        addAll(listOf("run", "build:js", "--", "--outdir", "dist/scripts"))
        if (!isDev) add("--minify")
    }
    inputs.dir("${project.projectDir}/web-assets/src")
    inputs.file("${project.projectDir}/web-assets/scripts/build-js.js")
    inputs.file("${project.projectDir}/web-assets/package.json")
    outputs.dir("${project.projectDir}/web-assets/dist/scripts")
}

val npmBuildSvg by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    val icons = Icons.entries.joinToString(",") { it.lucideIcon }
    args = listOf("run", "build:svg", "--", icons)
    inputs.file("${project.projectDir}/web-assets/scripts/build-icons.js")
    inputs.dir("${project.projectDir}/web-assets/node_modules/lucide-static/icons")
    outputs.dir("${project.projectDir}/web-assets/dist/icons")
}

val prettierSources = fileTree("${project.projectDir}/web-assets") {
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
    inputs.file("${project.projectDir}/web-assets/.prettierrc")
}

val prettierFormat by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args = if (webDiffFiles.isNotEmpty()) listOf("exec", "--", "prettier", "--write") + webDiffFiles
    else listOf("run", "format")
    inputs.files(prettierSources)
    inputs.file("${project.projectDir}/web-assets/.prettierrc")
}

tasks.named("check") {
    dependsOn(prettierCheck)
}

tasks.named("format") {
    dependsOn(prettierFormat)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(npmBuildCss, npmBuildJs, npmBuildSvg)
    from("${project.projectDir}/web-assets/dist/stylesheets") { into("static") }
    from("${project.projectDir}/web-assets/dist/scripts") { into("static") }
    from("${project.projectDir}/web-assets/dist/icons") { into("static") }
    from("${project.projectDir}/web-assets/dist/manifest.json")
}
