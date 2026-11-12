
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

class ModuleManager(context: Context) {

    private val modulesDir = File(context.filesDir, "modules").apply {
        mkdirs()
    }

    fun listModules(): List<ModuleInfo> =
        modulesDir.listFiles()
            ?.filter { it.isDirectory && !it.name.endsWith(".tmp") }
            ?.mapNotNull {
                runCatching {
                    readManifest(File(it, "manifest.json"))
                }.getOrNull()
            }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun uninstallModule(id: String): Boolean {
        require(isValidId(id)) {
            "Invalid module ID"
        }

        val target = File(modulesDir, id)

        if (!target.exists()) return false

        require(
            target.canonicalFile.parentFile ==
                modulesDir.canonicalFile
        ) {
            "Unsafe module path"
        }

        return target.deleteRecursively()
    }

    fun importPackage(packageFile: File): ModuleInfo {
        require(
            packageFile.isFile &&
            packageFile.extension.equals("kmod", true)
        ) {
            "Expected a .kmod package"
        }

        require(packageFile.length() <= MAX_ARCHIVE_BYTES) {
            "Archive exceeds 20 MB"
        }

        val info = ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("manifest.json")
                ?: error("Missing manifest.json")

            require(!entry.isDirectory) {
                "Invalid manifest"
            }

            val manifest = zip.getInputStream(entry)
                .bufferedReader()
                .use { it.readText() }

            parseManifest(manifest)
        }

        require(info.apiVersion == SUPPORTED_API) {
            "Unsupported Armed 7 API version"
        }

        val target = File(modulesDir, info.id)
        val temp = File(modulesDir, "${info.id}.tmp")
        val backup = File(modulesDir, "${info.id}.backup")

        temp.deleteRecursively()
        backup.deleteRecursively()

        check(temp.mkdirs()) {
            "Could not create temporary directory"
        }

        try {
            var total = 0L
            var count = 0

            ZipFile(packageFile).use { zip ->
                val entries = zip.entries()

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    count++

                    require(count <= MAX_ENTRIES) {
                        "Too many package entries"
                    }

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
                        output.parentFile?.mkdirs()

                        zip.getInputStream(entry).use { input ->
                            output.outputStream().use { out ->
                                val buffer = ByteArray(8192)

                                while (true) {
                                    val read = input.read(buffer)
                                    if (read == -1) break

                                    total += read

                                    require(total <= MAX_PACKAGE_BYTES) {
                                        "Package exceeds 20 MB"
                                    }

                                    out.write(buffer, 0, read)
                                }
                            }
                        }
                    }
                }
            }

            val installed = readManifest(
                File(temp, "manifest.json")
            )

            require(installed.id == info.id) {
                "Manifest changed during extraction"
            }

            if (target.exists()) {
                check(target.renameTo(backup)) {
                    "Could not back up existing module"
                }
            }

            if (!temp.renameTo(target)) {
                if (backup.exists()) {
                    backup.renameTo(target)
                }
                error("Could not install module")
            }

            backup.deleteRecursively()

            return installed

        } catch (e: Exception) {
            temp.deleteRecursively()

            if (backup.exists() && !target.exists()) {
                backup.renameTo(target)
            }

            throw e
        }
    }

    private fun readManifest(file: File): ModuleInfo {
        require(file.isFile) {
            "Missing manifest.json"
        }

        require(file.length() <= MAX_MANIFEST_BYTES) {
            "Manifest too large"
        }

        return parseManifest(file.readText())
    }

    private fun parseManifest(text: String): ModuleInfo {
        val json = JSONObject(text)

        val id = json.getString("id")
        val name = json.getString("name")
        val version = json.getString("version")
        val apiVersion = json.getInt("apiVersion")

        require(isValidId(id)) {
            "Invalid module ID"
        }

        require(name.isNotBlank() && name.length <= 80) {
            "Invalid module name"
        }

        require(version.isNotBlank() && version.length <= 32) {
            "Invalid module version"
        }

        return ModuleInfo(
            id = id,
            name = name,
            version = version,
            apiVersion = apiVersion
        )
    }

    private fun isValidId(id: String): Boolean =
        id.matches(Regex("[a-z0-9._-]{2,64}"))

    companion object {
        const val SUPPORTED_API = 1

        private const val MAX_ARCHIVE_BYTES =
            20L * 1024L * 1024L

        private const val MAX_PACKAGE_BYTES =
            20L * 1024L * 1024L

        private const val MAX_MANIFEST_BYTES =
            64L * 1024L

        private const val MAX_ENTRIES = 500
    }
}
