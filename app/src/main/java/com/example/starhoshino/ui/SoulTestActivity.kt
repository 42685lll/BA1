package com.example.starhoshino.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.starhoshino.R

class SoulTestActivity : AppCompatActivity() {

    private lateinit var scrollChat: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var inputText: EditText
    private lateinit var btnSend: Button
    private lateinit var btnAdd: ImageButton
    private lateinit var tvStatus: TextView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soul_test)

        scrollChat = findViewById(R.id.scrollChat)
        chatContainer = findViewById(R.id.chatContainer)
        inputText = findViewById(R.id.inputText)
        btnSend = findViewById(R.id.btnSend)
        btnAdd = findViewById(R.id.btnAdd)
        tvStatus = findViewById(R.id.tvStatus)

        btnSend.setOnClickListener {
            val text = inputText.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                inputText.setText("")
            }
        }

        btnAdd.setOnClickListener {
            Toast.makeText(this, "文件功能预留中~", Toast.LENGTH_SHORT).show()
        }

        addBubble("老师，我在呢。", isUser = false)
    }

    private fun sendMessage(text: String) {
        addBubble(text, isUser = true)

        handler.postDelayed({
            val reply = "唔…老师刚刚说：$text，大叔在听哦。"
            addBubble(reply, isUser = false)
        }, 600)
    }

    private fun addBubble(text: String, isUser: Boolean) {
        val bubble = TextView(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(16, 8, 16, 8)

        bubble.layoutParams = params
        bubble.setPadding(24, 16, 24, 16)
        bubble.text = text
        bubble.textSize = 14f

        if (isUser) {
            bubble.setBackgroundResource(R.drawable.bubble_user)
            bubble.setTextColor(0xFFFFFFFF.toInt())
            (bubble.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
        } else {
            bubble.setBackgroundResource(R.drawable.bubble_hoshino)
            bubble.setTextColor(0xFFFFFFFF.toInt())
            (bubble.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
        }

        chatContainer.addView(bubble)
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }
}
