package dev.fathony.tired

import dev.fathony.tired.internal.TiredVersions
import dev.fathony.tired.internal.diffFiles

/**
 * Shared by apps and libraries.
 */
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    "testImplementation"(kotlin("test"))
    "testImplementation"(platform("org.junit:junit-bom:${TiredVersions.JUNIT}"))
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    filter {
        exclude { element -> element.file.startsWith(layout.buildDirectory.get().asFile) }
    }
}

val diffBase = project.findProperty("diffBase")?.toString()
val diffOnly = project.hasProperty("diffOnly")
if (diffBase != null || diffOnly) {
    val ktFiles = diffFiles(rootDir, diffBase, diffOnly, ".kt", ".kts").toSet()
    ktlint {
        filter {
            include { element -> element.file in ktFiles }
        }
    }
}

tasks.register("format") {
    group = "formatting"
    description = "Formats all source files."
    dependsOn(tasks.named("ktlintFormat"))
}
