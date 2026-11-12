
package com.kayser.armed7

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ModuleResourceBrowser(
    private val context: Context,
    private val reader: ModuleResourceReader
) {

    fun show(moduleId: String, moduleName: String) {
        try {
            val resources = reader.listResources(moduleId)

            if (resources.isEmpty()) {
                Toast.makeText(
                    context,
                    "Este módulo não possui recursos.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val labels = resources.map {
                "${it.path}  •  ${formatSize(it.size)}"
            }.toTypedArray()

            AlertDialog.Builder(context)
                .setTitle("Recursos: $moduleName")
                .setItems(labels) { _, index ->
                    openResource(moduleId, resources[index])
                }
                .setNegativeButton("Fechar", null)
                .show()

        } catch (exception: Exception) {
            showError(exception.message)
        }
    }

    private fun openResource(
        moduleId: String,
        resource: ModuleResourceReader.ResourceInfo
    ) {
        try {
            when (resource.extension) {
                "json", "txt" -> {
                    val content = reader.readText(
                        moduleId,
                        resource.path
                    )

                    showText(resource.path, content)
                }

                "png", "webp", "jpg", "jpeg" -> {
                    val bytes = reader.readBytes(
                        moduleId,
                        resource.path
                    )

                    AlertDialog.Builder(context)
                        .setTitle("Recurso de imagem")
                        .setMessage(
                            "Arquivo: ${resource.path}\n\n" +
                            "Tamanho: ${formatSize(bytes.size.toLong())}\n\n" +
                            "Imagem reconhecida. A visualização " +
                            "será adicionada em uma etapa posterior."
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }

                else -> showError("Formato não suportado.")
            }
        } catch (exception: Exception) {
            showError(exception.message)
        }
    }

    private fun showText(path: String, content: String) {
        val text = TextView(context).apply {
            this.text = content
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(24, 20, 24, 20)
            gravity = Gravity.START
            setTextIsSelectable(true)
        }

        val scroll = ScrollView(context).apply {
            addView(text)
        }

        AlertDialog.Builder(context)
            .setTitle(path)
            .setView(scroll)
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun showError(message: String?) {
        Toast.makeText(
            context,
            message ?: "Não foi possível abrir o recurso.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L ->
                "%.2f MB".format(bytes / (1024f * 1024f))

            bytes >= 1024L ->
                "%.1f KB".format(bytes / 1024f)

            else -> "$bytes bytes"
        }
    }
}
