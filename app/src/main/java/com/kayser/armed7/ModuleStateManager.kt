package com.kayser.armed7

import android.content.Context

/**
 * Gerencia o estado de ativação dos módulos.
 *
 * A ativação é registrada no Armed 7.
 * Isso ainda não representa execução dentro do Among Us.
 */
class ModuleStateManager(context: Context) {

    private val preferences = context.getSharedPreferences(
        "armed7_module_states",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val ENABLED_MODULES = "enabled_modules"

        private val ID_PATTERN =
            Regex("[a-z0-9._-]{2,64}")
    }

    fun isEnabled(id: String): Boolean {
        validateId(id)

        return getEnabledModules().contains(id)
    }

    fun enable(id: String): Boolean {
        validateId(id)

        val modules = getEnabledModules().toMutableSet()

        if (!modules.add(id)) {
            return true
        }

        return saveModules(modules)
    }

    fun disable(id: String): Boolean {
        validateId(id)

        val modules = getEnabledModules().toMutableSet()

        if (!modules.remove(id)) {
            return true
        }

        return saveModules(modules)
    }

    fun toggle(id: String): Boolean {
        validateId(id)

        return if (isEnabled(id)) {
            disable(id)
        } else {
            enable(id)
        }
    }

    fun getEnabledModules(): Set<String> {
        return preferences.getStringSet(
            ENABLED_MODULES,
            emptySet()
        )?.toSet() ?: emptySet()
    }

    fun removeModule(id: String): Boolean {
        return disable(id)
    }

    fun clearAll(): Boolean {
        return preferences.edit()
            .remove(ENABLED_MODULES)
            .commit()
    }

    private fun saveModules(
        modules: Set<String>
    ): Boolean {
        return preferences.edit()
            .putStringSet(
                ENABLED_MODULES,
                modules.toSet()
            )
            .commit()
    }

    private fun validateId(id: String) {
        require(ID_PATTERN.matches(id)) {
            "Invalid module ID"
        }
    }
}
