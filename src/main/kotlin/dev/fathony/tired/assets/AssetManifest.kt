package dev.fathony.tired.assets

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object AssetManifest {
    private val entries: Map<String, String> by lazy {
        val json = AssetManifest::class.java.getResource("/manifest.json")?.readText()
        if (json != null) {
            val obj = Json.parseToJsonElement(json) as JsonObject
            obj.mapValues { it.value.jsonPrimitive.content }
        } else {
            emptyMap()
        }
    }

    fun resolve(logicalName: String): String = entries[logicalName] ?: logicalName
}
