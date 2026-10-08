package dev.fathony.tired

import dev.fathony.tired.internal.TiredVersions
import dev.fathony.tired.smoketest.SmokeTest
import dev.fathony.tired.smoketest.SmokeTestExtension
import io.ktor.plugin.features.DockerPortMapping

plugins {
    id("dev.fathony.tired.kotlin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.ktor.plugin")
}

/**
 * The minimum to start: server, engine and logging. Features stay in the app.
 */
dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:${TiredVersions.LOGBACK}")
}

application {
    /**
     * Netty loads native libraries, which JDK 25 restricts without this flag.
     */
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC")
}

ktor {
    docker {
        jreVersion = JavaVersion.VERSION_25
        localImageName = project.name
        imageTag = "latest"
        portMappings = listOf(DockerPortMapping(3000, 3000))

        environmentVariable(
            "JAVA_TOOL_OPTIONS",
            "--enable-native-access=ALL-UNNAMED -XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport",
        )
    }
}

/**
 * Read by tired-library's `isTiredDev`.
 */
tasks.named<JavaExec>("run") {
    systemProperty("tired.dev", "true")
}

val smokeTestSettings =
    extensions.create<SmokeTestExtension>("smokeTest").apply {
        port.convention(3000)
        paths.convention(listOf("/"))
        startTimeoutSeconds.convention(30)
    }

/**
 * Runs the app as the image does: the built classes and libraries, without what `run` adds for development.
 * Not part of `check`: it needs the app's port free.
 */
tasks.register<SmokeTest>("smokeTest") {
    group = "verification"
    description = "Starts the app on the JVM and requests pages from it."
    configureFrom(smokeTestSettings)

    val java = javaToolchains.launcherFor(java.toolchain).map { it.executablePath.asFile.absolutePath }
    val classpath = sourceSets.main.map { it.runtimeClasspath }
    val jvmArgs = provider { application.applicationDefaultJvmArgs.toList() }
    val mainClass = application.mainClass
    app.from(classpath)
    command.set(provider { listOf(java.get()) + jvmArgs.get() + listOf("-cp", classpath.get().asPath, mainClass.get()) })
    log.set(layout.buildDirectory.file("reports/smokeTest/output.log"))
}

tasks.register<Exec>("setupGitHooks") {
    group = "setup"
    description = "Point git at .githooks so the pre-commit hook runs automatically."
    commandLine("git", "config", "core.hooksPath", ".githooks")
}
