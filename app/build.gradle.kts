import io.ktor.plugin.features.DockerImageRegistry

plugins {
    id("dev.fathony.tired")
    id("dev.fathony.tired.icons")
    id("dev.fathony.tired.native")
    alias(libs.plugins.sqldelight)
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
 * In CI, also tags the image with its commit next to Ktor's `latest`, or `jvm` for the JVM image. The labels replace
 * the base image's on the GHCR package page, and the source label links the package to this repo.
 */
val commit = providers.environmentVariable("GITHUB_SHA").orNull
val jvmImage = providers.gradleProperty("tired.image").orNull == "jvm"

jib {
    to {
        tags = setOfNotNull(commit?.let { (if (jvmImage) "jvm-" else "") + "sha-${it.take(7)}" })
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
 * One page of every feature, so each template and query is reached in both builds.
 */
smokeTest {
    paths = listOf("/", "/htmx-test", "/htmx", "/contacts", "/contacts?q=an", "/bank", "/tickets", "/sse-demo")
}

/**
 * The migrations are the schema: every query in a `.sq` file is checked against what they add up to.
 */
sqldelight {
    databases {
        create("Database") {
            packageName = "dev.fathony.tired.data"
            dialect(libs.sqldelight.sqlite.dialect)
            deriveSchemaFromMigrations = true
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
    implementation(libs.sqlite.jdbc)
    implementation(libs.sqldelight.sqlite.driver)
    implementation(libs.hikari)
}
