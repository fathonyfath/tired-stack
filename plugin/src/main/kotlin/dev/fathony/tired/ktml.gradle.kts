package dev.fathony.tired

import dev.fathony.tired.internal.TiredVersions

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.ktml.gradle")
}

dependencies {
    "implementation"("dev.ktml:ktml-runtime:${TiredVersions.KTML}")
    "implementation"("dev.ktml:ktml-ktor:${TiredVersions.KTML}")
    /**
     * Substituted by the repo's `library/` build when it's included.
     */
    "implementation"("dev.fathony.tired:tired-library:${TiredVersions.TIRED}")
}

val ktmlDevMode =
    configurations.create("ktmlDevMode") {
        /**
         * Leaked by `dev.ktool:kotlin-gen`: a second SLF4J provider and a test library.
         */
        exclude(group = "org.slf4j", module = "slf4j-simple")
        exclude(group = "dev.ktool", module = "kotest-bdd")
    }

dependencies {
    ktmlDevMode("dev.ktml:ktml-dev-mode:${TiredVersions.KTML}")
}

/**
 * Dev mode turns on whenever it's on the classpath, so only `run` gets it.
 * The KTML plugin's `developmentOnly` would put it in the jar.
 */
pluginManager.withPlugin("application") {
    tasks.named<JavaExec>("run") {
        classpath += ktmlDevMode
        /**
         * Its embedded Kotlin compiler uses `sun.misc.Unsafe`.
         */
        jvmArgumentProviders.add { listOf("--sun-misc-unsafe-memory-access=allow") }
    }
}

/**
 * The KTML plugin doesn't order its output before ktlint.
 */
pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
    tasks
        .matching { it.name.startsWith("runKtlint") || it.name.contains("KtlintCheck") || it.name.contains("KtlintFormat") }
        .configureEach { mustRunAfter(tasks.named("generateKtml")) }
}
