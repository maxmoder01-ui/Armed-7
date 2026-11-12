
package com.kayser.armed7

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.database.Cursor
import android.provider.OpenableColumns
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File

class MainActivity : Activity() {

    private lateinit var manager: ModuleManager
    private lateinit var statusText: TextView
    private lateinit var moduleList: LinearLayout

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                importModule(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(16, 21, 31)
        window.navigationBarColor = Color.rgb(16, 21, 31)

        manager = ModuleManager(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(Color.rgb(16, 21, 31))
        }

        val scroll = ScrollView(this).apply {
            addView(root)
            isFillViewport = true
        }

        val title = text(
            "ARMED 7",
            32f,
            Color.WHITE,
            true
        )

        root.addView(title)

        root.addView(text(
            "Central de módulos • Alpha 0.2",
            15f,
            Color.LTGRAY
        ).apply {
            setPadding(0, dp(6), 0, dp(28))
        })

        statusText = text(
            "",
            17f,
            Color.WHITE,
            true
        ).apply {
            setPadding(dp(10), dp(18), dp(10), dp(22))
        }

        root.addView(statusText)

        root.addView(makeButton("Importar módulo") {
            filePicker.launch(arrayOf("*/*"))
        })

        root.addView(makeButton("Atualizar lista") {
            refreshModules()
            toast("Lista atualizada.")
        })

        root.addView(text(
            "MÓDULOS INSTALADOS",
            14f,
            Color.rgb(150, 180, 220),
            true
        ).apply {
            gravity = Gravity.START
            setPadding(0, dp(30), 0, dp(12))
        })

        moduleList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(moduleList)

        root.addView(text(
            "SOBRE O ARMED 7",
            14f,
            Color.rgb(150, 180, 220),
            true
        ).apply {
            gravity = Gravity.START
            setPadding(0, dp(30), 0, dp(10))
        })

        root.addView(text(
            "O Armed 7 é uma estrutura para organizar módulos Android. " +
            "Nesta versão, os pacotes são armazenados como dados. " +
            "O aplicativo ainda não modifica o Among Us.",
            13f,
            Color.LTGRAY
        ))

        setContentView(scroll)
        refreshModules()
    }

    private fun refreshModules() {
        val modules = manager.listModules()

        statusText.text =
            "Armed 7 está funcionando\nMódulos instalados: ${modules.size}"

        moduleList.removeAllViews()

        if (modules.isEmpty()) {
            moduleList.addView(text(
                "Nenhum módulo instalado.\nImporte um arquivo .kmod para começar.",
                14f,
                Color.LTGRAY
            ))
            return
        }

        modules.forEach { module ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundColor(Color.rgb(29, 38, 53))
            }

            item.addView(text(
                module.name,
                17f,
                Color.WHITE,
                true
            ))

            item.addView(text(
                "ID: ${module.id}\nVersão: ${module.version}",
                13f,
                Color.LTGRAY
            ))

            moduleList.addView(item, matchWrap())
        }
    }

    private fun importModule(uri: Uri) {
        val name = getFileName(uri)

        if (!name.endsWith(".kmod", ignoreCase = true)) {
            toast("Selecione um arquivo .kmod.")
            return
        }

        val tempFile = File(
            cacheDir,
            "armed7-import.kmod"
        )

        try {
            val input = contentResolver.openInputStream(uri)
                ?: error("Não foi possível abrir o arquivo.")

            input.use { source ->
                tempFile.outputStream().use { output ->
                    source.copyTo(output)
                }
            }

            val result = manager.importPackage(tempFile)

            refreshModules()
            toast("Módulo ${result.name} instalado!")

        } catch (e: Exception) {
            toast("Falha ao importar módulo. Verifique o pacote.")
        } finally {
            tempFile.delete()
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "arquivo"

        val cursor: Cursor? = contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) {
                    name = it.getString(index) ?: name
                }
            }
        }

        return name
    }

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setOnClickListener { action() }

            val params = matchWrap()
            params.bottomMargin = dp(10)
            layoutParams = params
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}
