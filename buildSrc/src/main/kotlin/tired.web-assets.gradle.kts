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
val distDir = if (isDev) "dist/dev" else "dist/prod"

val npmBuildCss by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args = buildList {
        addAll(listOf("run", "build:css", "--", "--outdir", "$distDir/stylesheets"))
        if (!isDev) add("--minify")
    }
    inputs.dir("${project.projectDir}/web-assets/src")
    inputs.dir("${project.projectDir}/src/main/kotlin")
    inputs.file("${project.projectDir}/web-assets/postcss.config.js")
    inputs.file("${project.projectDir}/web-assets/package.json")
    outputs.dir("${project.projectDir}/web-assets/$distDir/stylesheets")
}

val npmBuildJs by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args = buildList {
        addAll(listOf("run", "build:js", "--", "--outdir", "$distDir/scripts"))
        if (!isDev) add("--minify")
    }
    inputs.dir("${project.projectDir}/web-assets/src")
    inputs.file("${project.projectDir}/web-assets/scripts/build-js.js")
    inputs.file("${project.projectDir}/web-assets/package.json")
    outputs.dir("${project.projectDir}/web-assets/$distDir/scripts")
}

val npmBuildSvg by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    val icons = Icons.entries.joinToString(",") { it.lucideIcon }
    args = listOf("run", "build:svg", "--", icons)
    inputs.file("${project.projectDir}/web-assets/scripts/build-icons.js")
    inputs.dir("${project.projectDir}/web-assets/node_modules/lucide-static/icons")
    outputs.dir("${project.projectDir}/web-assets/dist/icons")
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(npmBuildCss, npmBuildJs, npmBuildSvg)
    from("${project.projectDir}/web-assets/$distDir/stylesheets") { into("static") }
    from("${project.projectDir}/web-assets/$distDir/scripts") { into("static") }
    from("${project.projectDir}/web-assets/dist/icons") { into("static") }
}
