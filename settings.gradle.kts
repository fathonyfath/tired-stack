pluginManagement {
    includeBuild("plugin")
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "tired-stack"

/**
 * Substituted for `dev.fathony.tired:tired-library`, which the tired plugin adds to apps.
 */
includeBuild("library")

include(":app")
