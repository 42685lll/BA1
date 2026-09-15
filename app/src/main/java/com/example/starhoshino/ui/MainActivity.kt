package com.example.starhoshino.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.starhoshino.R
import com.example.starhoshino.core.KnowledgeBase

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSoul = findViewById<Button>(R.id.btnSoul)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnSoul.setOnClickListener {
            KnowledgeBase.remember("进入灵魂测试")
            startActivity(Intent(this, SoulTestActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
