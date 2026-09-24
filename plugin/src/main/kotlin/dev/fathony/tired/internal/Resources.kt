package dev.fathony.tired.internal

internal fun readResource(path: String): ByteArray =
    requireNotNull(object {}.javaClass.getResourceAsStream("/dev/fathony/tired/$path")) {
        "Missing plugin resource: $path"
    }.use { it.readBytes() }
