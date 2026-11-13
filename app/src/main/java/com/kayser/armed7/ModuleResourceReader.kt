
package com.kayser.armed7

import android.content.Context
import java.io.File
import java.io.ByteArrayOutputStream

class ModuleResourceReader(context: Context) {

    private val modulesDir = File(context.filesDir, "modules")

    data class ResourceInfo(
        val path: String,
        val extension: String,
        val size: Long
    )

    fun listResources(moduleId: String): List<ResourceInfo> {
        val moduleDir = getModuleDirectory(moduleId)
        val manifestFile = File(moduleDir, "manifest.json")

        require(manifestFile.isFile) {
            "Manifesto não encontrado."
        }

        val manifest = KmodManifestParser.parse(
            manifestFile.readText(Charsets.UTF_8)
        )

        require(manifest.id == moduleId) {
            "Identidade do módulo inválida."
        }

        val result = mutableListOf<ResourceInfo>()

        for (directoryName in listOf("assets", "resources")) {
            val directory = File(moduleDir, directoryName)

            if (!directory.exists()) continue

            require(directory.isDirectory) {
                "Diretório de recursos inválido."
            }

            directory.walkTopDown().forEach { file ->
                if (!file.isFile) return@forEach

                require(isInside(moduleDir, file)) {
                    "Caminho de recurso inseguro."
                }

                val extension = file.extension.lowercase()

                if (extension !in ALLOWED_EXTENSIONS) {
                    return@forEach
                }

                val relativePath = file.relativeTo(moduleDir)
                    .invariantSeparatorsPath

                result.add(
                    ResourceInfo(
                        path = relativePath,
                        extension = extension,
                        size = file.length()
                    )
                )

                require(result.size <= MAX_RESOURCES) {
                    "O módulo possui recursos demais."
                }
            }
        }

        return result.sortedBy { it.path }
    }

    fun readText(
        moduleId: String,
        resourcePath: String
    ): String {
        val file = getResourceFile(moduleId, resourcePath)

        require(file.extension.lowercase() in TEXT_EXTENSIONS) {
            "Formato de texto não permitido."
        }

        val bytes = readBounded(file, MAX_TEXT_BYTES)

        return bytes.toString(Charsets.UTF_8)
    }

    fun readBytes(
        moduleId: String,
        resourcePath: String
    ): ByteArray {
        val file = getResourceFile(moduleId, resourcePath)

        require(file.extension.lowercase() in IMAGE_EXTENSIONS) {
            "Formato de imagem não permitido."
        }

        return readBounded(file, MAX_IMAGE_BYTES)
    }

    private fun getModuleDirectory(moduleId: String): File {
        require(isValidId(moduleId)) {
            "ID de módulo inválido."
        }

        val directory = File(modulesDir, moduleId)

        require(directory.isDirectory) {
            "Módulo não encontrado."
        }

        require(
            directory.canonicalFile.parentFile ==
                modulesDir.canonicalFile
        ) {
            "Caminho de módulo inseguro."
        }

        return directory
    }

    private fun getResourceFile(
        moduleId: String,
        resourcePath: String
    ): File {
        require(resourcePath.isNotBlank()) {
            "Caminho vazio."
        }

        require(
            !resourcePath.startsWith("/") &&
            !resourcePath.contains("\\") &&
            !resourcePath.contains('\u0000') &&
            !resourcePath.contains(":")
        ) {
            "Caminho inválido."
        }

        val parts = resourcePath.split("/")

        require(parts.none {
            it.isBlank() || it == "." || it == ".."
        }) {
            "Caminho inseguro."
        }

        require(
            parts.firstOrNull() == "assets" ||
            parts.firstOrNull() == "resources"
        ) {
            "Recurso fora dos diretórios permitidos."
        }

        val moduleDir = getModuleDirectory(moduleId)
        val file = File(moduleDir, resourcePath)

        require(isInside(moduleDir, file)) {
            "Caminho de recurso inseguro."
        }

        require(file.isFile) {
            "Recurso não encontrado."
        }

        return file
    }

    private fun isInside(
        parent: File,
        child: File
    ): Boolean {
        val parentPath = parent.canonicalPath +
            File.separator

        return child.canonicalPath.startsWith(parentPath)
    }

    private fun readBounded(
        file: File,
        limit: Long
    ): ByteArray {
        require(file.length() <= limit) {
            "Arquivo excede o limite permitido."
        }

        val output = ByteArrayOutputStream()
        var total = 0L

        file.inputStream().use { input ->
            val buffer = ByteArray(8192)

            while (true) {
                val count = input.read(buffer)

                if (count == -1) break

                total += count

                require(total <= limit) {
                    "Arquivo excede o limite permitido."
                }

                output.write(buffer, 0, count)
            }
        }

        return output.toByteArray()
    }

    private fun isValidId(id: String): Boolean {
        return id.matches(Regex("[a-z0-9._-]{2,64}"))
    }

    companion object {
        private const val MAX_RESOURCES = 500
        private const val MAX_TEXT_BYTES = 1024L * 1024L
        private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L

        private val TEXT_EXTENSIONS = setOf("json", "txt")

        private val IMAGE_EXTENSIONS = setOf(
            "png",
            "webp",
            "jpg",
            "jpeg"
        )

        private val ALLOWED_EXTENSIONS =
            TEXT_EXTENSIONS + IMAGE_EXTENSIONS
    }
}
