package com.example.starhoshino.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.starhoshino.R
import com.example.starhoshino.core.KnowledgeBase

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnShowMemory = findViewById<Button>(R.id.btnShowMemory)

        btnBack.setOnClickListener { finish() }

        btnShowMemory.setOnClickListener {
            val lines = KnowledgeBase.recent()
            if (lines.isEmpty()) {
                Toast.makeText(this, "记忆是空的~", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, lines.joinToString("\n").take(400), Toast.LENGTH_LONG).show()
            }
        }
    }
}
