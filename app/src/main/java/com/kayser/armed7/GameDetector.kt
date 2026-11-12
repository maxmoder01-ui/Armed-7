
package com.kayser.armed7

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.zip.ZipFile

/**
 * Detecta informações públicas da instalação do Among Us.
 * Inspeciona os APKs somente para leitura.
 */
class GameDetector(context: Context) {

    private val packageManager = context.packageManager

    fun detect(
        packageName: String = GameCompatibility.DEFAULT_GAME_PACKAGE
    ): GameEnvironment {
        return try {
            val info = getPackageInfo(packageName)

            GameEnvironment(
                packageName = info.packageName,
                versionName = info.versionName,
                versionCode = getVersionCode(info),
                gameAbis = inspectGameAbis(info),
                isInstalled = true
            )
        } catch (exception: PackageManager.NameNotFoundException) {
            GameEnvironment(isInstalled = false)
        }
    }

    private fun inspectGameAbis(info: PackageInfo): List<String>? {
        val appInfo = info.applicationInfo ?: return null
        val paths = mutableListOf<String>()

        appInfo.sourceDir?.let { paths.add(it) }

        if (Build.VERSION.SDK_INT >= 21) {
            appInfo.splitSourceDirs?.let { paths.addAll(it) }
        }

        if (paths.isEmpty()) return null

        val found = linkedSetOf<String>()

        return try {
            for (path in paths) {
                ZipFile(path).use { zip ->
                    val entries = zip.entries()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue

                        val parts = entry.name.split("/")

                        if (
                            parts.size >= 3 &&
                            parts[0] == "lib" &&
                            parts[1] in SUPPORTED_ABIS
                        ) {
                            found.add(parts[1])
                        }
                    }
                }
            }

            found.takeIf { it.isNotEmpty() }?.toList()
        } catch (exception: Exception) {
            null
        }
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }

    private fun getVersionCode(info: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= 28) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    companion object {
        private val SUPPORTED_ABIS = setOf(
            "armeabi-v7a",
            "arm64-v8a",
            "x86",
            "x86_64"
        )
    }
}
