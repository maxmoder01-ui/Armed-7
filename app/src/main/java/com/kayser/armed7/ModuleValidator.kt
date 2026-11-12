
package com.kayser.armed7

enum class ModuleCompatibilityStatus {
    COMPATIBLE,
    INCOMPATIBLE,
    UNKNOWN
}

data class ModuleValidation(
    val status: ModuleCompatibilityStatus,
    val errors: List<String>,
    val warnings: List<String>
) {
    val valid: Boolean
        get() = status == ModuleCompatibilityStatus.COMPATIBLE
}

class ModuleValidator {

    fun validate(
        module: ModuleInfo,
        game: GameEnvironment
    ): ModuleValidation {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (module.apiVersion != ModuleManager.SUPPORTED_API) {
            errors.add("Versão da API incompatível.")
        }

        val manifest = module.manifest

        if (manifest == null) {
            errors.add("Manifesto não disponível.")
            return result(errors, warnings)
        }

        if (manifest.targetPackage != GameCompatibility.DEFAULT_GAME_PACKAGE) {
            errors.add("Pacote de destino não reconhecido.")
        }

        if (!game.isInstalled) {
            warnings.add("Among Us não está instalado ou não foi detectado.")
        }

        if (game.packageName == null) {
            warnings.add("Pacote do jogo desconhecido.")
        } else if (game.packageName != manifest.targetPackage) {
            errors.add("O módulo foi criado para outro pacote.")
        }

        val gameVersion = game.versionName

        if (gameVersion == null) {
            warnings.add("Versão do jogo não identificada.")
        } else {
            manifest.minGameVersion?.let { minimum ->
                if (compareVersions(gameVersion, minimum) < 0) {
                    errors.add("O jogo está abaixo da versão mínima.")
                }
            }

            manifest.maxGameVersion?.let { maximum ->
                if (compareVersions(gameVersion, maximum) > 0) {
                    errors.add("O jogo ultrapassa a versão máxima.")
                }
            }
        }

        if (manifest.supportedAbis.isNotEmpty()) {
            if (game.gameAbis == null) {
                warnings.add("Arquitetura do jogo não verificada.")
            } else if (
                manifest.supportedAbis.none {
                    it in game.gameAbis
                }
            ) {
                errors.add("Arquitetura não suportada pelo módulo.")
            }
        }

        return result(errors, warnings)
    }

    private fun result(
        errors: List<String>,
        warnings: List<String>
    ): ModuleValidation {
        val status = when {
            errors.isNotEmpty() ->
                ModuleCompatibilityStatus.INCOMPATIBLE

            warnings.isNotEmpty() ->
                ModuleCompatibilityStatus.UNKNOWN

            else ->
                ModuleCompatibilityStatus.COMPATIBLE
        }

        return ModuleValidation(
            status = status,
            errors = errors,
            warnings = warnings
        )
    }

    private fun compareVersions(
        first: String,
        second: String
    ): Int {
        val a = first.split(".").map { it.toInt() }
        val b = second.split(".").map { it.toInt() }

        val size = maxOf(a.size, b.size)

        for (index in 0 until size) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }

            if (left != right) {
                return left.compareTo(right)
            }
        }

        return 0
    }
}
