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
import com.example.starhoshino.core.*

class SoulTestActivity : AppCompatActivity() {

    private lateinit var scrollChat: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var inputText: EditText
    private lateinit var btnSend: Button
    private lateinit var btnAdd: ImageButton
    private lateinit var tvStatus: TextView

    private lateinit var chatContext: ChatContext
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

        chatContext = ChatContext(mutableListOf(), "")

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

        addBubble(BondSystem.getReturnGreeting(), isUser = false)

        ThinkEngine.resetSession()
    }

    private fun sendMessage(text: String) {
        addBubble(text, isUser = true)

        EmotionEngine.detectUserEmotion(text)
        extractKnowledge(text)

        chatContext.lastUserInput = text
        chatContext.addMessage(ChatMessage("user", text, System.currentTimeMillis(), EmotionEngine.getUserEmotion()))

        val strategy = ThinkEngine.decideStrategy(chatContext)

        handler.postDelayed({
            var reply = ThinkEngine.generateReply(strategy, chatContext)

            reply = reply
                .replace("老师之前说过", "")
                .replace("之前老师说过", "")
                .replace("之前说过", "")
                .replace("我记得呢", "")
                .replace("记得呢", "")
                .replace("没忘记", "")
                .replace("放在心里", "")
                .replace("记下了", "")
                .replace("记住了", "")
                .replace("用户说：", "")
                .replace("星野回：", "")
                .trimEnd('~', '。', '，', '.', ',', ' ')
                .trim()

            if (reply.isBlank() || reply == chatContext.lastHoshino()) {
                reply = "唔…老师继续说嘛，大叔在听。"
            }

            addBubble(reply, isUser = false)
            chatContext.addMessage(ChatMessage("hoshino", reply, System.currentTimeMillis(), strategy.emotion))
            BondSystem.onMessageExchanged()

            RecallEngine.addMemory(
                summary = reply,
                emotion = strategy.emotion,
                keywords = text.split(" ").take(5)
            )
        }, strategy.delayMs)
    }

    private fun extractKnowledge(text: String) {
        val patterns = mapOf(
            "我喜欢" to "喜好",
            "我叫" to "名字",
            "我今年" to "年龄",
            "我是" to "身份",
            "我最" to "偏好"
        )
        for ((trigger, category) in patterns) {
            if (text.contains(trigger)) {
                val value = text.substringAfter(trigger).take(20).trim()
                if (value.isNotBlank()) {
                    KnowledgeBase.learn("${category}_${value.take(10)}", value, 0.7f)
                }
            }
        }
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
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN)
        }

    override fun onDestroy() {
        super.onDestroy()
        KnowledgeBase.save()
        RecallEngine.save()
    }
}
