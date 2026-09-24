plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.node.gradle)
    alias(libs.plugins.ktlint)
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

group = "dev.fathony.tired"
version = libs.versions.tired.get()

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
    implementation(libs.ktor.gradle.plugin)
    implementation(libs.node.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.ktml.gradle.plugin)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation("org.jetbrains.kotlin:kotlin-test:$embeddedKotlinVersion")
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("tired.version", libs.versions.tired.get())
}

/**
 * Versions of the libraries the plugins add, kept in sync with the catalog.
 */
val generateTiredVersions =
    tasks.register("generateTiredVersions") {
        val tired = libs.versions.tired
        val ktml = libs.versions.ktml
        val logback = libs.versions.logback
        val junit = libs.versions.junit
        val nodejs = libs.versions.nodejs
        inputs.property("tired", tired)
        inputs.property("ktml", ktml)
        inputs.property("logback", logback)
        inputs.property("junit", junit)
        inputs.property("nodejs", nodejs)
        val outputDir = layout.buildDirectory.dir("generated/source/tiredVersions")
        outputs.dir(outputDir)

        doLast {
            val file = outputDir.get().file("dev/fathony/tired/internal/TiredVersions.kt").asFile
            file.parentFile.mkdirs()
            file.writeText(
                """
            |package dev.fathony.tired.internal
            |
            |internal object TiredVersions {
            |    const val TIRED = "${tired.get()}"
            |    const val KTML = "${ktml.get()}"
            |    const val LOGBACK = "${logback.get()}"
            |    const val JUNIT = "${junit.get()}"
            |    const val NODE = "${nodejs.get()}"
            |}
            |
                """.trimMargin(),
            )
        }
    }

kotlin.sourceSets.main {
    kotlin.srcDir(generateTiredVersions)
}

/**
 * A real npm project, so Renovate updates it and `npmInstall` refreshes its lockfile.
 */
val toolchainDir = layout.projectDirectory.dir("src/main/resources/dev/fathony/tired/toolchain")

node {
    download = true
    version = libs.versions.nodejs.get()
    nodeProjectDir = toolchainDir
}

tasks.processResources {
    exclude("**/node_modules/**")
}

/**
 * Honours `-PdiffOnly` and `-PdiffBase` like the app, for the pre-commit hook.
 */
val changedFiles: Set<File>? =
    run {
        val diffBase = findProperty("diffBase")?.toString()
        if (diffBase == null && !hasProperty("diffOnly")) return@run null

        fun git(vararg args: String) =
            providers
                .exec { commandLine("git", *args) }
                .standardOutput.asText
                .get()
                .lines()
                .filter { it.isNotBlank() }
        val repoRoot = File(git("rev-parse", "--show-toplevel").single())
        val diff = if (diffBase != null) listOf("$diffBase...HEAD") else listOf("--cached")
        git("diff", "--name-only", "--diff-filter=ACM", *diff.toTypedArray()).map { repoRoot.resolve(it) }.toSet()
    }

ktlint {
    filter {
        exclude { it.file.startsWith(layout.buildDirectory.get().asFile) }
        changedFiles?.let { files -> include { it.file in files } }
    }
}

tasks.register("format") {
    group = "formatting"
    description = "Formats the plugin's Kotlin."
    dependsOn(tasks.named("ktlintFormat"))
}
