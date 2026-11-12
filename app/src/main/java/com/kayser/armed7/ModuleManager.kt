
package com.kayser.armed7

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile

data class ModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val apiVersion: Int,
    val manifest: KmodManifest? = null
)

class ModuleManager(context: Context) {

    private val modulesDir = File(context.filesDir, "modules").apply {
        mkdirs()
    }

    fun listModules(): List<ModuleInfo> =
        modulesDir.listFiles()
            ?.filter {
                it.isDirectory &&
                !it.name.endsWith(".tmp") &&
                !it.name.endsWith(".backup")
            }
            ?.mapNotNull {
                runCatching {
                    readManifest(File(it, "manifest.json"))
                }.getOrNull()
            }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun uninstallModule(id: String): Boolean {
        require(isValidId(id)) {
            "ID de módulo inválido."
        }

        val target = File(modulesDir, id)

        if (!target.exists()) return false

        require(
            target.canonicalFile.parentFile ==
                modulesDir.canonicalFile
        ) {
            "Caminho de módulo inseguro."
        }

        return target.deleteRecursively()
    }

    fun importPackage(packageFile: File): ModuleInfo {
        require(
            packageFile.isFile &&
            packageFile.extension.equals("kmod", true)
        ) {
            "Selecione um arquivo .kmod."
        }

        require(packageFile.length() in 1..MAX_ARCHIVE_BYTES) {
            "Arquivo vazio ou maior que 20 MB."
        }

        val info = ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("manifest.json")
                ?: error("Manifesto não encontrado.")

            require(!entry.isDirectory) {
                "Manifesto inválido."
            }

            if (entry.size > MAX_MANIFEST_BYTES) {
                error("Manifesto excede 64 KB.")
            }

            val content = zip.getInputStream(entry).use {
                readBounded(it, MAX_MANIFEST_BYTES)
            }

            KmodManifestParser.parse(
                content.toString(Charsets.UTF_8)
            )
        }

        require(info.apiVersion == SUPPORTED_API) {
            "Versão da API incompatível."
        }

        val target = File(modulesDir, info.id)
        val temp = File(modulesDir, "${info.id}.tmp")
        val backup = File(modulesDir, "${info.id}.backup")

        temp.deleteRecursively()
        backup.deleteRecursively()

        check(temp.mkdirs()) {
            "Não foi possível criar a pasta temporária."
        }

        try {
            extractPackage(packageFile, temp)

            val installed = readManifest(
                File(temp, "manifest.json")
            )

            require(installed.id == info.id) {
                "Identidade do módulo alterada."
            }

            if (target.exists()) {
                check(target.renameTo(backup)) {
                    "Não foi possível criar o backup."
                }
            }

            if (!temp.renameTo(target)) {
                if (backup.exists()) {
                    backup.renameTo(target)
                }

                error("Não foi possível instalar o módulo.")
            }

            backup.deleteRecursively()

            return installed

        } catch (exception: Exception) {
            temp.deleteRecursively()

            if (backup.exists() && !target.exists()) {
                backup.renameTo(target)
            }

            throw exception
        }
    }

    private fun extractPackage(
        packageFile: File,
        destination: File
    ) {
        var totalBytes = 0L
        var entryCount = 0

        val extractedPaths = mutableSetOf<String>()

        ZipFile(packageFile).use { zip ->
            val entries = zip.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                entryCount++

                require(entryCount <= MAX_ENTRIES) {
                    "O pacote possui arquivos demais."
                }

                val normalized = validateEntryName(entry.name)

                require(extractedPaths.add(normalized)) {
                    "O pacote possui caminhos duplicados."
                }

                if (entry.isDirectory) {
                    require(
                        normalized == "assets" ||
                        normalized.startsWith("assets/") ||
                        normalized == "resources" ||
                        normalized.startsWith("resources/")
                    ) {
                        "Pasta não permitida."
                    }
                } else {
                    require(isAllowedFile(normalized)) {
                        "Tipo de arquivo não permitido: $normalized"
                    }
                }

                val output = File(destination, normalized)

                require(
                    output.canonicalFile.toPath()
                        .startsWith(destination.canonicalFile.toPath())
                ) {
                    "Caminho de extração inseguro."
                }

                if (entry.isDirectory) {
                    check(output.mkdirs() || output.isDirectory) {
                        "Não foi possível criar uma pasta."
                    }

                    continue
                }

                if (entry.size > MAX_PACKAGE_BYTES) {
                    error("Arquivo interno muito grande.")
                }

                output.parentFile?.let {
                    check(it.mkdirs() || it.isDirectory) {
                        "Não foi possível criar uma pasta."
                    }
                }

                zip.getInputStream(entry).use { input ->
                    output.outputStream().use { out ->
                        val buffer = ByteArray(8192)

                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break

                            totalBytes += read

                            require(totalBytes <= MAX_PACKAGE_BYTES) {
                                "Conteúdo extraído excede 20 MB."
                            }

                            out.write(buffer, 0, read)
                        }
                    }
                }
            }
        }

        require(File(destination, "manifest.json").isFile) {
            "Manifesto ausente após extração."
        }
    }

    private fun validateEntryName(name: String): String {
        require(name.isNotBlank()) {
            "Nome de arquivo vazio."
        }

        require(
            !name.startsWith("/") &&
            !name.startsWith("\\") &&
            !name.contains("\\") &&
            !name.contains('\u0000') &&
            !name.contains(":")
        ) {
            "Caminho de arquivo inválido."
        }

        val parts = name.split("/")

        require(parts.none {
            it.isBlank() || it == "." || it == ".."
        }) {
            "O pacote contém um caminho inseguro."
        }

        return name
    }

    private fun isAllowedFile(path: String): Boolean {
        if (path == "manifest.json") return true

        val allowedDirectory =
            path.startsWith("assets/") ||
            path.startsWith("resources/")

        if (!allowedDirectory) return false

        val extension = path.substringAfterLast(
            ".",
            ""
        ).lowercase()

        return extension in ALLOWED_EXTENSIONS
    }

    private fun readManifest(file: File): ModuleInfo {
        require(file.isFile) {
            "Manifesto não encontrado."
        }

        require(file.length() <= MAX_MANIFEST_BYTES) {
            "Manifesto muito grande."
        }

        val manifest = KmodManifestParser.parse(
            file.readText(Charsets.UTF_8)
        )

        return ModuleInfo(
            id = manifest.id,
            name = manifest.name,
            version = manifest.version,
            apiVersion = manifest.apiVersion,
            manifest = manifest
        )
    }

    private fun isValidId(id: String): Boolean =
        id.matches(Regex("[a-z0-9._-]{2,64}"))

    private fun readBounded(
        input: java.io.InputStream,
        limit: Long
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var total = 0L

        while (true) {
            val read = input.read(buffer)
            if (read == -1) break

            total += read

            require(total <= limit) {
                "Conteúdo excede o limite permitido."
            }

            output.write(buffer, 0, read)
        }

        return output.toByteArray()
    }

    companion object {
        const val SUPPORTED_API = 1

        private const val MAX_ARCHIVE_BYTES =
            20L * 1024L * 1024L

        private const val MAX_PACKAGE_BYTES =
            20L * 1024L * 1024L

        private const val MAX_MANIFEST_BYTES =
            64L * 1024L

        private const val MAX_ENTRIES = 500

        private val ALLOWED_EXTENSIONS = setOf(
            "json",
            "txt",
            "png",
            "webp",
            "jpg",
            "jpeg"
        )
    }
}
