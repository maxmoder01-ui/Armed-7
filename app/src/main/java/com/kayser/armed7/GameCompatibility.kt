
package com.kayser.armed7

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.zip.ZipFile

/**
 * Detecta informações públicas da instalação do Among Us.
 *
 * Inspeciona os APKs apenas para leitura.
 * Não altera arquivos nem interage com o processo do jogo.
 */
class GameDetector(context: Context) {

    private val packageManager = context.packageManager

    fun detect(
        packageName: String = GameCompatibility.DEFAULT_GAME_PACKAGE
    ): GameEnvironment {
        return try {
            val packageInfo = getPackageInfo(packageName)

            GameEnvironment(
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName,
                versionCode = getVersionCode(packageInfo),
                gameAbis = inspectGameAbis(packageInfo),
                isInstalled = true
            )
        } catch (exception: PackageManager.NameNotFoundException) {
            GameEnvironment(
                packageName = null,
                versionName = null,
                versionCode = null,
                gameAbis = null,
                isInstalled = false
            )
        }
    }

    private fun inspectGameAbis(
        packageInfo: PackageInfo
    ): List<String>? {
        val applicationInfo = packageInfo.applicationInfo
            ?: return null

        val apkPaths = mutableListOf<String>()

        applicationInfo.sourceDir?.let {
            apkPaths.add(it)
        }

        if (Build.VERSION.SDK_INT >= 21) {
            applicationInfo.splitSourceDirs?.let {
                apkPaths.addAll(it)
            }
        }

        if (apkPaths.isEmpty()) return null

        val detectedAbis = linkedSetOf<String>()

        return try {
            for (apkPath in apkPaths) {
                ZipFile(apkPath).use { zip ->
                    val entries = zip.entries()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()

                        if (entry.isDirectory) continue

                        val parts = entry.name.split("/")

                        if (parts.size >= 3 && parts[0] == "lib") {
                            val abi = parts[1]

                            if (abi in SUPPORTED_ABIS) {
                                detectedAbis.add(abi)
                            }
                        }
                    }
                }
            }

            detectedAbis.takeIf { it.isNotEmpty() }?.toList()
        } catch (exception: Exception) {
            // Falha na leitura significa arquitetura desconhecida.
            null
        }
    }

    private fun getPackageInfo(
        packageName: String
    ): PackageInfo {
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

    private fun getVersionCode(
        packageInfo: PackageInfo
    ): Long {
        return if (Build.VERSION.SDK_INT >= 28) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
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
