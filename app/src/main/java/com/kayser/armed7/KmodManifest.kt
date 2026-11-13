
package com.kayser.armed7

import org.json.JSONObject

data class KmodManifest(
    val id: String,
    val name: String,
    val version: String,
    val apiVersion: Int,
    val author: String,
    val description: String,
    val targetPackage: String,
    val minGameVersion: String?,
    val maxGameVersion: String?,
    val supportedAbis: List<String>,
    val entry: String? = null
)

object KmodManifestParser {

    private val ID_PATTERN =
        Regex("[a-z0-9._-]{2,64}")

    private val VERSION_PATTERN =
        Regex("[0-9]+(\\.[0-9]+){0,3}")

    private val ALLOWED_ABIS = setOf(
        "armeabi-v7a",
        "arm64-v8a",
        "x86",
        "x86_64"
    )

    fun parse(content: String): KmodManifest {
        require(content.toByteArray(Charsets.UTF_8).size <= 65536) {
            "Manifesto excede 64 KB."
        }

        val json = JSONObject(content)

        val id = json.getString("id")
        val name = json.getString("name")
        val version = json.getString("version")
        val apiVersion = json.getInt("apiVersion")

        val author = json.optString("author", "Desconhecido")
        val description = json.optString("description", "")

        val targetPackage = json.optString(
            "targetPackage",
            GameCompatibility.DEFAULT_GAME_PACKAGE
        )

        val minVersion = json
            .optString("minGameVersion")
            .takeIf { it.isNotBlank() }

        val maxVersion = json
            .optString("maxGameVersion")
            .takeIf { it.isNotBlank() }

        val entry = json
            .optString("entry")
            .takeIf { it.isNotBlank() }

        if (entry != null) {
            validateEntryPath(entry)
        }

        val abis = mutableListOf<String>()
        val abiArray = json.optJSONArray("supportedAbis")

        if (abiArray != null) {
            for (i in 0 until abiArray.length()) {
                val abi = abiArray.getString(i)

                require(abi in ALLOWED_ABIS) {
                    "Arquitetura não reconhecida: $abi"
                }

                if (abi !in abis) {
                    abis.add(abi)
                }
            }
        }

        require(ID_PATTERN.matches(id)) {
            "ID de módulo inválido."
        }

        require(name.isNotBlank() && name.length <= 80) {
            "Nome inválido."
        }

        require(VERSION_PATTERN.matches(version)) {
            "Versão inválida."
        }

        require(apiVersion > 0) {
            "Versão da API inválida."
        }

        require(author.length <= 80) {
            "Nome do autor muito longo."
        }

        require(description.length <= 1000) {
            "Descrição muito longa."
        }

        require(
            targetPackage == GameCompatibility.DEFAULT_GAME_PACKAGE
        ) {
            "Pacote de destino não reconhecido."
        }

        if (minVersion != null) {
            require(VERSION_PATTERN.matches(minVersion)) {
                "Versão mínima inválida."
            }
        }

        if (maxVersion != null) {
            require(VERSION_PATTERN.matches(maxVersion)) {
                "Versão máxima inválida."
            }
        }

        return KmodManifest(
            id = id,
            name = name,
            version = version,
            apiVersion = apiVersion,
            author = author,
            description = description,
            targetPackage = targetPackage,
            minGameVersion = minVersion,
            maxGameVersion = maxVersion,
            supportedAbis = abis,
            entry = entry
        )
    }

    private fun validateEntryPath(path: String) {
        require(path.length <= 240) {
            "Caminho de entrada muito longo."
        }

        require(
            !path.startsWith("/") &&
            !path.contains("\\") &&
            !path.contains('\u0000') &&
            !path.contains(":")
        ) {
            "Caminho de entrada inválido."
        }

        val parts = path.split("/")

        require(parts.size >= 2) {
            "Caminho de entrada incompleto."
        }

        require(
            parts.first() == "assets" ||
            parts.first() == "resources"
        ) {
            "A entrada deve ficar em assets/ ou resources/."
        }

        require(parts.none {
            it.isBlank() || it == "." || it == ".."
        }) {
            "Caminho de entrada inseguro."
        }

        require(path.endsWith(".json", ignoreCase = true)) {
            "A entrada do módulo precisa ser um arquivo JSON."
        }
    }
}
