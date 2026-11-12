
package com.kayser.armed7

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

    const val DEFAULT_GAME_PACKAGE = "com.innersloth.spacemafia"

    fun evaluate(
        game: GameEnvironment,
        module: ModuleCompatibilitySpec,
        armed7ApiVersion: Int = 1
    ): CompatibilityResult {
        val reasons = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (!game.isInstalled) {
            warnings.add("Among Us não foi detectado.")
        }

        if (game.packageName == null) {
            warnings.add("Pacote do jogo desconhecido.")
        } else if (game.packageName != module.targetPackage) {
            reasons.add("Pacote do jogo incompatível.")
        }

        if (armed7ApiVersion != module.requiredApiVersion) {
            reasons.add("Versão da API do Armed 7 incompatível.")
        }

        if (game.versionName == null) {
            warnings.add("Versão do jogo desconhecida.")
        } else {
            module.minGameVersion?.let { minimum ->
                val comparison = compareVersions(game.versionName, minimum)

                if (comparison == null) {
                    warnings.add("Não foi possível interpretar a versão do jogo.")
                } else if (comparison < 0) {
                    reasons.add("Versão do jogo abaixo do mínimo exigido.")
                }
            }

            module.maxGameVersion?.let { maximum ->
                val comparison = compareVersions(game.versionName, maximum)

                if (comparison == null) {
                    warnings.add("Não foi possível interpretar a versão do jogo.")
                } else if (comparison > 0) {
                    reasons.add("Versão do jogo acima do máximo permitido.")
                }
            }
        }

        if (!module.supportedAbis.isNullOrEmpty()) {
            if (game.gameAbis == null) {
                warnings.add("Arquitetura do jogo não verificada.")
            } else if (
                module.supportedAbis.none { it in game.gameAbis }
            ) {
                reasons.add("Nenhuma arquitetura compatível foi encontrada.")
            }
        }

        val status = when {
            reasons.isNotEmpty() -> CompatibilityStatus.INCOMPATIBLE
            warnings.isNotEmpty() -> CompatibilityStatus.UNKNOWN
            else -> CompatibilityStatus.COMPATIBLE
        }

        return CompatibilityResult(status, reasons, warnings)
    }

    fun compareVersions(first: String, second: String): Int? {
        val a = parseVersion(first) ?: return null
        val b = parseVersion(second) ?: return null

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

    private fun parseVersion(version: String): List<Int>? {
        val match = Regex(
            """^\s*(\d+(?:\.\d+){0,3})(?:[-+].*)?\s*$"""
        ).matchEntire(version) ?: return null

        return match.groupValues[1]
            .split(".")
            .mapNotNull { it.toIntOrNull() }
            .takeIf { it.isNotEmpty() }
            ?.takeIf { it.size == match.groupValues[1].count { c -> c == '.' } + 1 }
    }
}
