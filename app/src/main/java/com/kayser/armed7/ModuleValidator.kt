package com.kayser.armed7

data class ModuleValidation(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

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

        if (!game.isInstalled) {
            warnings.add("Among Us não foi detectado.")
        }

        if (game.packageName == null) {
            warnings.add("Pacote do jogo desconhecido.")
        }

        return ModuleValidation(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
