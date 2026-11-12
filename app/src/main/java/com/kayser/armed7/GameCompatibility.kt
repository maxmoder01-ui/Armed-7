package com.kayser.armed7

/**
 * Verifica se um módulo declara compatibilidade com o ambiente
 * detectado do Among Us e com a API do Armed 7.
 *
 * Este componente apenas valida metadados.
 * Ele não modifica nem executa código dentro do jogo.
 */

enum class CompatibilityStatus {
    COMPATIBLE,
    INCOMPATIBLE,
    UNKNOWN
}

data class GameEnvironment(
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val gameAbis: List<String>? = null,
    val isInstalled: Boolean = false
)

data class ModuleCompatibilitySpec(
    val targetPackage: String = "com.innersloth.spacemafia",
    val minGameVersion: String? = null,
    val maxGameVersion: String? = null,
    val supportedAbis: List<String>? = null,
    val requiredApiVersion: Int = 1
)

data class CompatibilityResult(
    val status: CompatibilityStatus,
    val reasons: List<String>,
    val warnings: List<String>
)

object GameCompatibility {

    const val DEFAULT_GAME_PACKAGE =
        "com.innersloth.spacemafia"

    fun evaluate(
        game: GameEnvironment,
        module: ModuleCompatibilitySpec,
        armed7ApiVersion: Int = 1
    ): CompatibilityResult {

        val reasons = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (!game.isInstalled) {
            return CompatibilityResult(
                CompatibilityStatus.UNKNOWN,
                listOf("Among Us não foi detectado."),
                emptyList()
            )
        }

        if (game.packageName == null) {
            warnings.add("Não foi possível identificar o pacote do jogo.")
        } else if (game.packageName != module.targetPackage) {
            reasons.add("O módulo foi criado para outro pacote.")
        }

        if (module.requiredApiVersion > armed7ApiVersion) {
            reasons.add(
                "O módulo exige uma API mais recente do Armed 7."
            )
        }

        if (game.versionName == null) {
            warnings.add("Versão do Among Us não identificada.")
        } else {
            module.minGameVersion?.let { minimum ->
                val comparison = compareVersions(
                    game.versionName,
                    minimum
                )

                if (comparison == null) {
                    warnings.add(
                        "Não foi possível comparar a versão mínima."
                    )
                } else if (comparison < 0) {
                    reasons.add(
                        "A versão do jogo é inferior à exigida pelo módulo."
                    )
                }
            }

            module.maxGameVersion?.let { maximum ->
                val comparison = compareVersions(
                    game.versionName,
                    maximum
                )

                if (comparison == null) {
                    warnings.add(
                        "Não foi possível comparar a versão máxima."
                    )
                } else if (comparison > 0) {
                    reasons.add(
                        "A versão do jogo é superior à suportada pelo módulo."
                    )
                }
            }
        }

        val requiredAbis = module.supportedAbis
        val detectedAbis = game.gameAbis

        if (requiredAbis != null) {
            if (detectedAbis == null || detectedAbis.isEmpty()) {
                warnings.add(
                    "Arquitetura do jogo ainda não foi identificada."
                )
            } else if (
                requiredAbis.none { required ->
                    detectedAbis.any { detected ->
                        required.equals(detected, ignoreCase = true)
                    }
                }
            ) {
                reasons.add(
                    "A arquitetura do jogo não é suportada pelo módulo."
                )
            }
        }

        val status = when {
            reasons.isNotEmpty() ->
                CompatibilityStatus.INCOMPATIBLE

            warnings.isNotEmpty() ->
                CompatibilityStatus.UNKNOWN

            else ->
                CompatibilityStatus.COMPATIBLE
        }

        return CompatibilityResult(
            status = status,
            reasons = reasons,
            warnings = warnings
        )
    }

    private fun compareVersions(
        first: String,
        second: String
    ): Int? {
        val firstParts = parseVersion(first) ?: return null
        val secondParts = parseVersion(second) ?: return null

        val size = maxOf(firstParts.size, secondParts.size)

        for (index in 0 until size) {
            val a = firstParts.getOrElse(index) { 0 }
            val b = secondParts.getOrElse(index) { 0 }

            if (a != b) {
                return a.compareTo(b)
            }
        }

        return 0
    }

    private fun parseVersion(version: String): List<Int>? {
        val cleanVersion = version.trim()

        if (cleanVersion.isEmpty()) return null

        val parts = cleanVersion.split(".")

        if (parts.any { part -> part.toIntOrNull() == null }) {
            return null
        }

        return parts.map { it.toInt() }
    }
}
