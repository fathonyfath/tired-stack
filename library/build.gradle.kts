/**
 * Added to apps by the tired plugin as `dev.fathony.tired:tired-library`.
 */
plugins {
    id("dev.fathony.tired.kotlin")
    `java-library`
    `maven-publish`
}

/**
 * For the `@Resource` routes in the tests; on the classpath through the tired plugin.
 */
apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

group = "dev.fathony.tired"
version = libs.versions.tired.get()

java {
    withSourcesJar()
}

/**
 * Credentials come from `reposiliteUsername` and `reposilitePassword`, set by the release workflow.
 */
publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
    repositories {
        maven("https://maven.fathony.dev/releases") {
            name = "reposilite"
            credentials(PasswordCredentials::class)
        }
    }
}

dependencies {
    api(platform(libs.ktor.bom))
    api(libs.ktor.server.core)
    api(libs.ktml.runtime)
    api(libs.ktml.ktor)

    /**
     * For `page` and `sendView`; apps that use them add these and install the plugins themselves.
     */
    compileOnly(libs.ktor.server.resources)
    compileOnly(libs.ktor.server.sse)

    testImplementation(libs.ktor.server.resources)
    testImplementation(libs.ktor.server.sse)
    testImplementation(libs.ktor.server.test.host)
}
