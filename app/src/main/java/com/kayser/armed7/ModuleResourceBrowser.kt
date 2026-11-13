
package com.kayser.armed7

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max

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
            when (resource.extension.lowercase()) {
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
                    showImage(resource.path, bytes)
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

    private fun showImage(path: String, bytes: ByteArray) {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            bounds
        )

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            showError("Não foi possível interpretar esta imagem.")
            return
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(
                bounds.outWidth,
                bounds.outHeight
            )
        }

        val bitmap = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            options
        )

        if (bitmap == null) {
            showError("Falha ao carregar a imagem.")
            return
        }

        val imageView = ImageView(context).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            maxHeight = dp(480)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val scroll = ScrollView(context).apply {
            addView(
                imageView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(path)
            .setView(scroll)
            .setPositiveButton("Fechar", null)
            .create()

        dialog.setOnDismissListener {
            imageView.setImageDrawable(null)
            bitmap.recycle()
        }

        dialog.show()
    }

    private fun calculateSampleSize(
        width: Int,
        height: Int
    ): Int {
        var sample = 1

        while (
            width / sample > 1200 ||
            height / sample > 1200
        ) {
            sample *= 2
        }

        return max(1, sample)
    }

    private fun dp(value: Int): Int {
        return (
            value * context.resources.displayMetrics.density
        ).toInt()
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
