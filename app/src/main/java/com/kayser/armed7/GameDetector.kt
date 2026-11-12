package com.kayser.armed7

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Detecta a instalação do Among Us usando o PackageManager.
 *
 * Não modifica arquivos nem interage com o processo do jogo.
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
                gameAbis = null,
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

    private fun getPackageInfo(
        packageName: String
    ) = if (Build.VERSION.SDK_INT >= 33) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }

    private fun getVersionCode(
        packageInfo: android.content.pm.PackageInfo
    ): Long {
        return if (Build.VERSION.SDK_INT >= 28) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}
