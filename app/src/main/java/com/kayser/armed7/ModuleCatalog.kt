package com.kayser.armed7

data class ModuleCatalogEntry(
    val module: ModuleInfo,
    val enabled: Boolean,
    val validation: ModuleValidation
)

class ModuleCatalog(
    private val manager: ModuleManager,
    private val stateManager: ModuleStateManager,
    private val validator: ModuleValidator,
    private val detector: GameDetector
) {

    fun getModules(): List<ModuleCatalogEntry> {
        val game = detector.detect()

        return manager.listModules().map { module ->
            ModuleCatalogEntry(
                module = module,
                enabled = stateManager.isEnabled(module.id),
                validation = validator.validate(module, game)
            )
        }
    }

    fun enableModule(id: String): Boolean {
        val module = manager.listModules()
            .firstOrNull { it.id == id }
            ?: return false

        val game = detector.detect()
        val result = validator.validate(module, game)

        if (!result.valid) {
            return false
        }

        return stateManager.enable(id)
    }

    fun disableModule(id: String): Boolean {
        return stateManager.disable(id)
    }

    fun uninstallModule(id: String): Boolean {
        val removed = manager.uninstallModule(id)

        if (removed) {
            stateManager.removeModule(id)
        }

        return removed
    }

    fun isModuleEnabled(id: String): Boolean {
        return stateManager.isEnabled(id)
    }
}
