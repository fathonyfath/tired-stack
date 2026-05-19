plugins {
    id("tired.ktor-app")
    id("tired.web-assets")
}

tasks.register<Exec>("setupGitHooks") {
    group = "setup"
    description = "Point git at .githooks so the pre-commit hook runs automatically."
    commandLine("git", "config", "core.hooksPath", ".githooks")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.htmx)
    implementation(libs.ktor.htmx.html)
    implementation(libs.ktor.server.htmx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)
    testImplementation(kotlin("test"))
}
