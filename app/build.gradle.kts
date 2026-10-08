import io.ktor.plugin.features.DockerImageRegistry
import java.nio.file.Files
import java.nio.file.Path

plugins {
    id("dev.fathony.tired")
    id("dev.fathony.tired.icons")
    id("org.graalvm.buildtools.native")
}

application {
    mainClass = "dev.fathony.tired.MainKt"
}

ktor {
    docker {
        localImageName = "tired-stack-sample"
        externalRegistry =
            DockerImageRegistry.externalRegistry(
                username = providers.environmentVariable("GHCR_USERNAME"),
                password = providers.environmentVariable("GHCR_TOKEN"),
                project = provider { "tired-stack-sample" },
                hostname = provider { "ghcr.io" },
                namespace = provider { "fathonyfath" },
            )
    }
}

/**
 * In CI, also tags the image with its commit next to Ktor's `latest`. The labels replace the base image's on the
 * GHCR package page, and the source label links the package to this repo.
 */
val commit = providers.environmentVariable("GITHUB_SHA").orNull

jib {
    to {
        tags = setOfNotNull(commit?.let { "sha-${it.take(7)}" })
    }
    container {
        labels =
            buildMap {
                put("org.opencontainers.image.title", "tired-stack-sample")
                put("org.opencontainers.image.description", "The sample app of the Tired Stack")
                put("org.opencontainers.image.source", "https://github.com/fathonyfath/tired-stack")
                commit?.let { put("org.opencontainers.image.revision", it) }
            }
    }
}

/**
 * `./gradlew nativeCompile` builds `build/native/nativeCompile/tired-stack-sample` with a GraalVM that Gradle
 * downloads. tired-library tells the image what the stack needs kept.
 */
val graalvm =
    javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.GRAAL_VM
    }

graalvmNative {
    binaries {
        named("main") {
            imageName = "tired-stack-sample"
            javaLauncher = graalvm
            /**
             * sqlite-jdbc loads its native library.
             */
            buildArgs.add("--enable-native-access=ALL-UNNAMED")
        }
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

webAssets {
    npm("htmx.org", "2.0.11")
    npm("htmx-ext-sse", "2.2.4")

    npm("tailwindcss", "4.3.3")
    npm("@tailwindcss/postcss", "4.3.3")
    postcss("@tailwindcss/postcss")
}

dependencies {
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.htmx)
    implementation(libs.ktor.server.htmx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sqlite.jdbc)
}
