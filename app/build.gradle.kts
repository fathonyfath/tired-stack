import io.ktor.plugin.features.DockerImageRegistry

plugins {
    id("dev.fathony.tired")
    id("dev.fathony.tired.icons")
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
}
