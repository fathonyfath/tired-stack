package dev.fathony.tired

import com.google.cloud.tools.jib.gradle.JibExtension
import com.google.cloud.tools.jib.gradle.JibTask
import dev.fathony.tired.nativeimage.NativeImageJibExtension
import dev.fathony.tired.smoketest.SmokeTest
import dev.fathony.tired.smoketest.SmokeTestExtension
import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import java.nio.file.Files
import java.nio.file.Path

/**
 * Ships the app as a GraalVM native image: `nativeCompile` builds it with a GraalVM that Gradle downloads,
 * `nativeSmokeTest` checks that it serves, and the image tasks, such as `publishImage`, package it in place of the
 * JVM build. tired-library tells the image what the stack needs kept.
 */
plugins {
    id("dev.fathony.tired.ktor-app")
    id("org.graalvm.buildtools.native")
}

val graalvm =
    the<JavaToolchainService>().launcherFor {
        languageVersion = the<JavaPluginExtension>().toolchain.languageVersion
        vendor = JvmVendorSpec.GRAAL_VM
    }

configure<GraalVMExtension> {
    binaries.named("main") {
        imageName = project.name
        javaLauncher = graalvm
        /**
         * As on the JVM, for libraries that load native code.
         */
        buildArgs.add("--enable-native-access=ALL-UNNAMED")
    }
}

/**
 * Gradle unpacks a downloaded JDK without its symlinks, which leaves `bin/native-image` an empty file.
 */
val repairNativeImageLauncher =
    tasks.register("repairNativeImageLauncher") {
        val home = graalvm.map { it.metadata.installationPath.asFile }
        doLast {
            val launcher = home.get().resolve("bin/native-image").toPath()
            if (!Files.isSymbolicLink(launcher) && Files.size(launcher) == 0L) {
                Files.delete(launcher)
                Files.createSymbolicLink(launcher, Path.of("../lib/svm/bin/native-image"))
            }
        }
    }

val nativeCompile =
    tasks.named<BuildNativeImageTask>("nativeCompile") {
        dependsOn(repairNativeImageLauncher)
    }

/**
 * The executable needs glibc and zlib from the base image, in the architecture it was built for.
 * `ktor.docker.customBaseImage` in the app overrides the image.
 */
ktor {
    docker {
        customBaseImage.convention("gcr.io/distroless/java-base-debian12")
    }
}

configure<JibExtension> {
    from {
        platforms {
            platform {
                os = "linux"
                architecture = if (System.getProperty("os.arch") in setOf("aarch64", "arm64")) "arm64" else "amd64"
            }
        }
    }
    pluginExtensions {
        pluginExtension {
            implementation = NativeImageJibExtension::class.java.name
        }
    }
}

tasks.withType<JibTask>().configureEach {
    dependsOn(nativeCompile)
}

val smokeTestSettings = the<SmokeTestExtension>()

/**
 * What an image is missing only shows when the code that needs it runs, so it gets the same test as the JVM build.
 */
tasks.register<SmokeTest>("nativeSmokeTest") {
    group = "verification"
    description = "Starts the native image and requests pages from it."
    configureFrom(smokeTestSettings)

    val executable = nativeCompile.flatMap { it.outputFile }
    app.from(executable)
    command.set(executable.map { listOf(it.asFile.absolutePath) })
    log.set(layout.buildDirectory.file("reports/nativeSmokeTest/output.log"))
}
