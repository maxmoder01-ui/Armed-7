
package com.kayser.armed7

import android.app.Activity
import android.app.AlertDialog
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
import java.io.File

class MainActivity : Activity() {

    private lateinit var manager: ModuleManager
    private lateinit var detector: GameDetector
    private lateinit var stateManager: ModuleStateManager
    private lateinit var catalog: ModuleCatalog

    private lateinit var statusText: TextView
    private lateinit var gameStatusText: TextView
    private lateinit var moduleList: LinearLayout

    companion object {
        private const val PICK_MODULE_REQUEST = 1001
        private const val MAX_IMPORT_BYTES = 20L * 1024L * 1024L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(16, 21, 31)
        window.navigationBarColor = Color.rgb(16, 21, 31)

        manager = ModuleManager(this)
        detector = GameDetector(this)
        stateManager = ModuleStateManager(this)

        catalog = ModuleCatalog(
            manager,
            stateManager,
            ModuleValidator(),
            detector
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(Color.rgb(16, 21, 31))
        }

        val scroll = ScrollView(this).apply {
            addView(root)
            isFillViewport = true
        }

        root.addView(text("ARMED 7", 32f, Color.WHITE, true))

        root.addView(text(
            "Central independente de módulos",
            15f,
            Color.LTGRAY
        ).apply {
            setPadding(0, dp(6), 0, dp(28))
        })

        statusText = text("", 17f, Color.WHITE, true).apply {
            setPadding(dp(10), dp(18), dp(10), dp(12))
        }
        root.addView(statusText)

        root.addView(sectionTitle("DETECÇÃO DO JOGO"))

        gameStatusText = text("", 14f, Color.LTGRAY).apply {
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setBackgroundColor(Color.rgb(29, 38, 53))
        }
        root.addView(gameStatusText)

        root.addView(makeButton("Verificar Among Us") {
            refreshGameStatus()
            refreshModules()
        })

        root.addView(makeButton("Importar módulo .kmod") {
            openModulePicker()
        })

        root.addView(makeButton("Atualizar catálogo") {
            refreshModules()
            toast("Catálogo atualizado.")
        })

        root.addView(sectionTitle("MÓDULOS INSTALADOS"))

        moduleList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(moduleList)

        root.addView(sectionTitle("SOBRE O ARMED 7"))

        root.addView(text(
            "O Armed 7 importa, organiza e valida pacotes " +
            ".kmod com recursos declarativos. A ativação " +
            "controla o estado dentro do próprio aplicativo. " +
            "Ela não injeta código nem altera o processo " +
            "do Among Us. Uma integração oficial ou " +
            "compatível com o jogo ainda é necessária.",
            13f,
            Color.LTGRAY
        ))

        setContentView(scroll)

        refreshModules()
        refreshGameStatus()
    }

    private fun sectionTitle(value: String): TextView =
        text(
            value,
            14f,
            Color.rgb(150, 180, 220),
            true
        ).apply {
            gravity = Gravity.START
            setPadding(0, dp(24), 0, dp(10))
        }

    private fun refreshGameStatus() {
        val game = detector.detect()

        gameStatusText.text = if (game.isInstalled) {
            "Among Us detectado\n\n" +
            "Pacote: ${game.packageName}\n" +
            "Versão: ${game.versionName ?: "Desconhecida"}\n" +
            "Código: ${game.versionCode ?: "Desconhecido"}\n" +
            "Arquitetura: não verificada"
        } else {
            "Among Us não foi encontrado.\n" +
            "Verifique se o jogo está instalado."
        }

        gameStatusText.setTextColor(
            if (game.isInstalled) {
                Color.rgb(130, 220, 150)
            } else {
                Color.LTGRAY
            }
        )
    }

    private fun refreshModules() {
        val entries = catalog.getModules()

        val enabledCount = entries.count { it.enabled }

        statusText.text =
            "Armed 7 operacional\n" +
            "Módulos: ${entries.size} • Ativados: $enabledCount"

        moduleList.removeAllViews()

        if (entries.isEmpty()) {
            moduleList.addView(text(
                "Nenhum módulo instalado.\n" +
                "Importe um arquivo .kmod para começar.",
                14f,
                Color.LTGRAY
            ))
            return
        }

        entries.forEach { entry ->
            val module = entry.module

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                setBackgroundColor(Color.rgb(29, 38, 53))
            }

            item.addView(text(
                module.name,
                18f,
                Color.WHITE,
                true
            ))

            val manifest = module.manifest

            item.addView(text(
                "ID: ${module.id}\n" +
                "Versão: ${module.version}\n" +
                "API: ${module.apiVersion}\n" +
                "Autor: ${manifest?.author ?: "Desconhecido"}",
                13f,
                Color.LTGRAY
            ))

            if (!manifest?.description.isNullOrBlank()) {
                item.addView(text(
                    manifest?.description ?: "",
                    13f,
                    Color.LTGRAY
                ).apply {
                    setPadding(0, dp(8), 0, dp(8))
                })
            }

            val compatibilityText = when {
                entry.validation.errors.isNotEmpty() ->
                    "Incompatível: ${entry.validation.errors.joinToString()}"

                entry.validation.warnings.isNotEmpty() ->
                    "Atenção: ${entry.validation.warnings.joinToString()}"

                else ->
                    "Requisitos conhecidos: aprovados"
            }

            item.addView(text(
                compatibilityText,
                12f,
                if (entry.validation.errors.isNotEmpty()) {
                    Color.rgb(255, 130, 130)
                } else {
                    Color.rgb(220, 190, 120)
                }
            ).apply {
                setPadding(0, dp(8), 0, dp(8))
            })

            item.addView(makeButton(
                if (entry.enabled) "Desativar módulo"
                else "Ativar módulo"
            ) {
                toggleModule(module.id)
            })

            item.addView(makeButton("Desinstalar") {
                confirmUninstall(module.id, module.name)
            })

            val params = matchWrap()
            params.bottomMargin = dp(12)
            moduleList.addView(item, params)
        }
    }

    private fun toggleModule(id: String) {
        try {
            val enabled = catalog.isModuleEnabled(id)

            val success = if (enabled) {
                catalog.disableModule(id)
            } else {
                catalog.enableModule(id)
            }

            if (success) {
                toast(
                    if (enabled) "Módulo desativado."
                    else "Módulo ativado no Armed 7."
                )
            } else {
                toast("Não foi possível ativar o módulo.")
            }

            refreshModules()

        } catch (exception: Exception) {
            toast("Falha ao alterar o estado do módulo.")
        }
    }

    private fun confirmUninstall(id: String, name: String) {
        AlertDialog.Builder(this)
            .setTitle("Desinstalar módulo")
            .setMessage(
                "Deseja remover \"$name\" do Armed 7?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Desinstalar") { _, _ ->
                try {
                    val removed = catalog.uninstallModule(id)

                    toast(
                        if (removed) "Módulo desinstalado."
                        else "Módulo não encontrado."
                    )

                    refreshModules()

                } catch (exception: Exception) {
                    toast("Não foi possível desinstalar.")
                }
            }
            .show()
    }

    private fun openModulePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, PICK_MODULE_REQUEST)
    }

    @Deprecated("Usado para compatibilidade com o seletor nativo")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_MODULE_REQUEST &&
            resultCode == RESULT_OK
        ) {
            data?.data?.let { importModule(it) }
        }
    }

    private fun importModule(uri: Uri) {
        val name = getFileName(uri)

        if (!name.endsWith(".kmod", ignoreCase = true)) {
            toast("Selecione um arquivo .kmod.")
            return
        }

        val tempFile = File(cacheDir, "armed7-import.kmod")

        try {
            val input = contentResolver.openInputStream(uri)
                ?: error("Não foi possível abrir o arquivo.")

            input.use { source ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L

                    while (true) {
                        val count = source.read(buffer)
                        if (count == -1) break

                        total += count

                        require(total <= MAX_IMPORT_BYTES) {
                            "Arquivo maior que 20 MB."
                        }

                        output.write(buffer, 0, count)
                    }
                }
            }

            val result = manager.importPackage(tempFile)

            refreshModules()
            toast("Módulo ${result.name} instalado!")

        } catch (exception: Exception) {
            toast(
                exception.message
                    ?: "Falha ao importar o módulo."
            )
        } finally {
            tempFile.delete()
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "arquivo"

        val cursor: Cursor? = contentResolver.query(
            uri, null, null, null, null
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
            textSize = 15f
            setOnClickListener { action() }

            val params = matchWrap()
            params.bottomMargin = dp(8)
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
                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
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
