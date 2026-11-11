package com.kayser.armed7

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

data class ModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val apiVersion: Int
)

/**
 * Alpha 0.1: gerencia apenas os metadados dos módulos.
 * Não executa código dos pacotes.
 */
class ModuleManager(context: Context) {

    private val modulesDir = File(context.filesDir, "modules").apply {
        mkdirs()
    }

    fun listModules(): List<ModuleInfo> =
        modulesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull {
                runCatching {
                    readManifest(File(it, "manifest.json"))
                }.getOrNull()
            } ?: emptyList()

    fun importPackage(packageFile: File): ModuleInfo {
        require(
            packageFile.isFile &&
            packageFile.extension.equals("kmod", true)
        ) {
            "Expected a .kmod package"
        }

        val info = ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("manifest.json")
                ?: error("Missing manifest.json")

            parseManifest(
                zip.getInputStream(entry)
                    .bufferedReader()
                    .use { it.readText() }
            )
        }

        require(info.apiVersion == SUPPORTED_API) {
            "Unsupported Armed 7 API version"
        }

        val target = File(modulesDir, info.id)
        val temp = File(modulesDir, "${info.id}.tmp")

        temp.deleteRecursively()
        temp.mkdirs()

        var total = 0L

        ZipFile(packageFile).use { zip ->
            val entries = zip.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val output = File(temp, entry.name)

                require(
                    output.canonicalFile.toPath()
                        .startsWith(temp.canonicalFile.toPath())
                ) {
                    "Unsafe package path"
                }

                if (entry.isDirectory) {
                    output.mkdirs()
                } else {
                    total += entry.size.coerceAtLeast(0)

                    require(total <= MAX_PACKAGE_BYTES) {
                        "Package exceeds 20 MB"
                    }

                    output.parentFile?.mkdirs()

                    zip.getInputStream(entry).use { input ->
                        output.outputStream().use { out ->
                            input.copyTo(out)
                        }
                    }
                }
            }
        }

        val installed = readManifest(
            File(temp, "manifest.json")
        )

        target.deleteRecursively()

        check(temp.renameTo(target)) {
            "Could not finalize module installation"
        }

        return installed
    }

    private fun readManifest(file: File): ModuleInfo {
        require(file.isFile) {
            "Missing manifest.json"
        }

        return parseManifest(file.readText())
    }

    private fun parseManifest(text: String): ModuleInfo {
        val json = JSONObject(text)
        val id = json.getString("id")

        require(id.matches(Regex("[a-z0-9._-]{2,64}"))) {
            "Invalid module ID"
        }

        return ModuleInfo(
            id,
            json.getString("name"),
            json.getString("version"),
            json.getInt("apiVersion")
        )
    }

    companion object {
        const val SUPPORTED_API = 1
        private const val MAX_PACKAGE_BYTES =
            20L * 1024L * 1024L
    }
}
