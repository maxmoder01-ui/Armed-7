
package com.kayser.armed7

import org.json.JSONObject

data class LoadedModuleDefinition(
    val manifest: KmodManifest,
    val schemaVersion: Int,
    val content: JSONObject
)

class ModuleDefinitionLoader(
    private val resourceReader: ModuleResourceReader
) {

    fun load(moduleId: String): LoadedModuleDefinition? {
        val manifest = resourceReader.readManifest(moduleId)

        val entryPath = manifest.entry ?: return null

        val jsonText = resourceReader.readText(
            moduleId,
            entryPath
        )

        val definition = JSONObject(jsonText)

        val definitionId = definition.optString("module_id")

        require(definitionId == manifest.id) {
            "O ID da definição não corresponde ao manifesto."
        }

        val schemaVersion = definition.optInt(
            "schema_version",
            1
        )

        require(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Versão de definição não suportada: $schemaVersion"
        }

        return LoadedModuleDefinition(
            manifest = manifest,
            schemaVersion = schemaVersion,
            content = definition
        )
    }

    companion object {
        private const val SUPPORTED_SCHEMA_VERSION = 1
    }
}
