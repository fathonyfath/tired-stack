/**
 * Added to apps by the tired plugin as `dev.fathony.tired:tired-library`.
 */
plugins {
    id("dev.fathony.tired.kotlin")
    `java-library`
}

/**
 * For the `@Resource` routes in the tests; on the classpath through the tired plugin.
 */
apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

group = "dev.fathony.tired"
version = libs.versions.tired.get()

dependencies {
    api(platform(libs.ktor.bom))
    api(libs.ktor.server.core)
    api(libs.ktor.server.resources)
    api(libs.ktor.server.sse)
    api(libs.ktml.runtime)
    api(libs.ktml.ktor)

    testImplementation(libs.ktor.server.test.host)
}
