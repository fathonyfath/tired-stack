pluginManagement {
    includeBuild("../plugin")
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
            /**
             * Releases pass `-Ptired.version=<tag>`; everything else uses the catalog's SNAPSHOT.
             */
            providers.gradleProperty("tired.version").orNull?.let { version("tired", it) }
        }
    }
}

rootProject.name = "tired-library"
