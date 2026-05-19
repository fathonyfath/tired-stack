import io.ktor.plugin.features.DockerImageRegistry
import io.ktor.plugin.features.DockerPortMapping

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.ktor.plugin")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("dev.fathony.tired.MainKt")
    // Netty uses native libraries (e.g. epoll, SSL) loaded via System::loadLibrary.
    // JVM 25 restricts this by default — without this flag it warns and will eventually block.
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC")
}

ktor {
    docker {
        jreVersion = JavaVersion.VERSION_25
        localImageName = "tired-stack"
        imageTag = "latest"
        portMappings = listOf(
            DockerPortMapping(3000, 3000)
        )
        externalRegistry = DockerImageRegistry.externalRegistry(
            username = providers.environmentVariable("GHCR_USERNAME"),
            password = providers.environmentVariable("GHCR_TOKEN"),
            project = provider { "tired-stack" },
            hostname = provider { "ghcr.io" },
            namespace = provider { "fathonyfath" },
        )

        environmentVariable("JAVA_TOOL_OPTIONS", "--enable-native-access=ALL-UNNAMED -XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport")
    }
}

kotlin {
    jvmToolchain(25)
}

val generatedDir = layout.buildDirectory.dir("generated/kotlin")

val copyIcons by tasks.registering(Copy::class) {
    from(rootProject.file("gradle/plugins/src/main/kotlin/Icons.kt"))
    into(generatedDir)
}

sourceSets["main"].kotlin.srcDir(generatedDir)

tasks.named("compileKotlin") {
    dependsOn(copyIcons)
}

tasks.test {
    useJUnitPlatform()
}

val diffBase = project.findProperty("diffBase")?.toString()
val diffOnly = project.hasProperty("diffOnly")
if (diffBase != null || diffOnly) {
    val ktFiles = diffFiles(rootDir, diffBase, diffOnly, ".kt").map { rootDir.resolve(it) }.toSet()
    ktlint {
        filter {
            include { element -> element.file in ktFiles }
        }
    }
}

tasks.register<Exec>("setupGitHooks") {
    group = "setup"
    description = "Point git at .githooks so the pre-commit hook runs automatically."
    commandLine("git", "config", "core.hooksPath", ".githooks")
}

tasks.register("format") {
    group = "formatting"
    description = "Formats all source files."
    dependsOn(tasks.named("ktlintFormat"))
}
