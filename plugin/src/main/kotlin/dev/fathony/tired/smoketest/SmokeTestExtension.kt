package dev.fathony.tired.smoketest

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Shared by `smokeTest` and, with `dev.fathony.tired.native`, `nativeSmokeTest`.
 */
abstract class SmokeTestExtension {
    abstract val port: Property<Int>

    abstract val paths: ListProperty<String>

    abstract val startTimeoutSeconds: Property<Int>
}
