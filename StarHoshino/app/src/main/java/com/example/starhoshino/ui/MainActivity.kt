package com.example.starhoshino.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.starhoshino.R
import com.example.starhoshino.core.BondSystem
import com.example.starhoshino.core.KnowledgeBase
import com.example.starhoshino.core.RecallEngine
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var discContainer: FrameLayout
    private lateinit var waveCanvas: WaveCanvas
    private lateinit var btnSoulTest: Button
    private var doubleClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val soulDir = File(filesDir, "starhoshino_soul")
        if (!soulDir.exists()) soulDir.mkdirs()
        KnowledgeBase.init(soulDir)
        RecallEngine.init(soulDir)
        BondSystem.init()

        discContainer = findViewById(R.id.discContainer)
        waveCanvas = findViewById(R.id.waveCanvas)
        btnSoulTest = findViewById(R.id.btnSoulTest)

        discContainer.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - doubleClickTime < 300) {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            doubleClickTime = now
        }

        btnSoulTest.setOnClickListener {
            startActivity(Intent(this, SoulTestActivity::class.java))
        }

        requestPermissionsIfNeeded()

        val greeting = BondSystem.getReturnGreeting()
        Toast.makeText(this, greeting, Toast.LENGTH_SHORT).show()

        BondSystem.onSessionStart()
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("前辈~需要几个小权限哦")
                .setMessage("不会乱用的啦，放心点允许嘛🥺")
                .setPositiveButton("允许") { _, _ -> ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100) }
                .setNegativeButton("拒绝") { _, _ -> Toast.makeText(this, "没有麦克风星野听不到你说话哦~要去设置里改吗？", Toast.LENGTH_LONG).show() }
                .show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        KnowledgeBase.save()
        RecallEngine.save()
        waveCanvas.release()
    }

    override fun onPause() {
        super.onPause()
        waveCanvas.stopSpeaking()
    }
}
