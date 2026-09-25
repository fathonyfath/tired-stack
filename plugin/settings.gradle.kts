dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
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

rootProject.name = "tired-gradle-plugin"
