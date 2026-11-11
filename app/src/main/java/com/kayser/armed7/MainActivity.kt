package com.kayser.armed7

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var manager: ModuleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = ModuleManager(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 24)
            setBackgroundColor(0xFF10151F.toInt())
        }

        root.addView(TextView(this).apply {
            text = "ARMED 7"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }, matchWrap())

        root.addView(TextView(this).apply {
            text = "Android module framework • Alpha 0.1"
            textSize = 14f
            setTextColor(0xFFB8C4D8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        }, matchWrap())

        statusText = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFFE8EDF5.toInt())
            gravity = Gravity.CENTER
            setPadding(12, 18, 12, 24)
        }

        root.addView(statusText, matchWrap())
        refreshStatus()

        root.addView(Button(this).apply {
            text = "Import .kmod package"
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Package picker will be added in the next update.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "Refresh module list"
            setOnClickListener {
                refreshStatus()
            }
        }, matchWrap())

        root.addView(TextView(this).apply {
            text = "Alpha 0.1 stores declarative package data only. It does not inject code into Among Us or modify the game process."
            textSize = 12f
            setTextColor(0xFF9AA8BC.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 0)
        }, matchWrap())

        setContentView(root)
    }

    private fun refreshStatus() {
        statusText.text =
            "Framework ready\nInstalled modules: ${manager.listModules().size}"
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}
