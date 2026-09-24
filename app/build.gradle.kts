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
        localImageName = "tired-stack"
        externalRegistry =
            DockerImageRegistry.externalRegistry(
                username = providers.environmentVariable("GHCR_USERNAME"),
                password = providers.environmentVariable("GHCR_TOKEN"),
                project = provider { "tired-stack" },
                hostname = provider { "ghcr.io" },
                namespace = provider { "fathonyfath" },
            )
    }
}

webAssets {
    npm("htmx.org", "2.0.11")
    npm("htmx-ext-sse", "2.2.4")

    npm("tailwindcss", "4.3.3")
    npm("@tailwindcss/postcss", "4.3.3")
    postcss("@tailwindcss/postcss")
}

icons {
    add("chevron-down", alias = "Chevron")
    add("search")
    add("shopping-cart")
}

dependencies {
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.htmx)
    implementation(libs.ktor.server.htmx)
    implementation(libs.kotlinx.serialization.json)
}
