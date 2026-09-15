package com.example.starhoshino.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.starhoshino.R
import com.example.starhoshino.core.KnowledgeBase
import com.example.starhoshino.core.RecallEngine
import java.io.File

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.btnChangeMainWallpaper).setOnClickListener { Toast.makeText(this, "壁纸功能预留~", Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btnChangeDiscWallpaper).setOnClickListener { Toast.makeText(this, "圆盘壁纸功能预留~", Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btnChangeAppName).setOnClickListener { Toast.makeText(this, "改名功能预留~", Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btnChangeAppIcon).setOnClickListener { Toast.makeText(this, "改图标功能预留~", Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btnViewHistory).setOnClickListener { Toast.makeText(this, "历史记录功能预留~", Toast.LENGTH_SHORT).show() }

        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            Toast.makeText(this, "用户档案：\n当前知识条目数 ${KnowledgeBase.getAllEntries().size}", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnPermissions).setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnExportChat).setOnClickListener { exportChat() }
        findViewById<Button>(R.id.btnImportChat).setOnClickListener { Toast.makeText(this, "导入功能预留~", Toast.LENGTH_SHORT).show() }

        findViewById<Button>(R.id.btnToggleOrientation).setOnClickListener {
            requestedOrientation = if (requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    private fun exportChat() {
        try {
            val exportDir = File(filesDir, "starhoshino_soul")
            val chatJson = File(exportDir, "chat.json")
            val knowledge = KnowledgeBase.getAllEntries()
            val json = org.json.JSONObject()
            val arr = org.json.JSONArray()
            for (entry in knowledge.values) {
                val obj = org.json.JSONObject()
                obj.put("key", entry.key)
                obj.put("learned", entry.learned)
                obj.put("confidence", entry.confidence)
                obj.put("mentionCount", entry.mentionCount)
                arr.put(obj)
            }
            json.put("knowledge", arr)
            json.put("exportTime", System.currentTimeMillis())
            chatJson.writeText(json.toString(2))
            Toast.makeText(this, "导出成功：${chatJson.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        KnowledgeBase.save()
        RecallEngine.save()
    }
}
