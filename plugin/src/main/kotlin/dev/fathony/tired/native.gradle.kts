package dev.fathony.tired

import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import java.nio.file.Files
import java.nio.file.Path

/**
 * `nativeCompile` builds the app as a GraalVM native image, with a GraalVM that Gradle downloads.
 * tired-library tells the image what the stack needs kept.
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

tasks.named("nativeCompile") {
    dependsOn(repairNativeImageLauncher)
}
