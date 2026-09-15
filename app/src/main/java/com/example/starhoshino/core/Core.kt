package com.example.starhoshino.core

import java.util.*
import kotlin.math.min

data class ChatMessage(
    val role: String,
    val content: String,
    val time: Long,
    val emotion: String = "平静"
)

data class ChatContext(
    val messages: MutableList<ChatMessage>,
    var lastUserInput: String
) {
    fun addMessage(m: ChatMessage) {
        messages.add(m)
        if (messages.size > 40) messages.removeAt(0)
    }

    fun lastHoshino(): String =
        messages.lastOrNull { it.role == "hoshino" }?.content ?: ""

    fun recentUserInputs(n: Int = 3): List<String> =
        messages.filter { it.role == "user" }.takeLast(n).map { it.content }

    fun recentText(): String =
        messages.takeLast(6).joinToString(" ") { it.content }

    fun isRepeat(text: String): Boolean {
        val last = messages.takeLast(4).filter { it.role == "hoshino" }.map { it.content }
        return last.any { it == text }
    }
}

object EmotionEngine {
    private var userEmotion = "平静"

    fun detectUserEmotion(text: String): String {
        userEmotion = when {
            text.contains("伤心") || text.contains("难过") || text.contains("哭") || text.contains("累") -> "难过"
            text.contains("生气") || text.contains("烦") || text.contains("滚") || text.contains("讨厌") -> "生气"
            text.contains("开心") || text.contains("喜欢") || text.contains("爱") || text.contains("好呀") -> "开心"
            text.contains("?") || text.contains("？") || text.contains("谁") || text.contains("什么") || text.contains("怎么") || text.contains("为什么") -> "疑惑"
            else -> "平静"
        }
        return userEmotion
    }

    fun getUserEmotion(): String = userEmotion
}

object KnowledgeBase {
    private val store = mutableMapOf<String, String>()
    private val weights = mutableMapOf<String, Float>()

    fun learn(key: String, value: String, weight: Float = 0.7f) {
        if (key.isBlank() || value.isBlank()) return
        val cleanKey = key.replace("老师", "").replace("用户", "").trim()
        val cleanValue = value.replace("老师之前说过", "")
            .replace("之前说过", "")
            .replace("老师", "")
            .trim()
        if (cleanKey.isBlank() || cleanValue.isBlank()) return
        store[cleanKey] = cleanValue
        weights[cleanKey] = (weights[cleanKey] ?: 0f) + weight
    }

    fun recallHintFor(text: String): String? {
        return store.entries
            .filter { text.contains(it.key.take(2)) || it.value.any { c -> text.contains(c) } }
            .sortedByDescending { weights[it.key] ?: 0f }
            .firstOrNull()
            ?.value
    }

    fun hasAny(): Boolean = store.isNotEmpty()

    fun save() {}
}

object RecallEngine {
    private val memories = mutableListOf<String>()

    fun addMemory(summary: String, emotion: String, keywords: List<String>) {
        var clean = summary
            .replace("老师", "")
            .replace("用户说：", "")
            .replace("星野回：", "")
            .replace("之前老师说过", "")
            .replace("老师之前说过", "")
            .replace("之前说过", "")
            .replace("我记得", "")
            .replace("记得呢", "")
            .replace("没忘记", "")
            .replace("放在心里", "")
            .replace("记下了", "")
            .replace("记住了", "")
            .trim()

        clean = clean.take(24).trim()
        if (clean.isBlank()) return
        if (memories.any { it == clean || it.contains(clean) || clean.contains(it) }) return

        memories.add(clean)
        if (memories.size > 30) memories.removeAt(0)

        val k = (keywords.firstOrNull() ?: "话题").replace("老师", "").trim()
        if (k.isNotBlank()) KnowledgeBase.learn(k, clean, 0.6f)
    }

    fun save() {}
}

object BondSystem {
    private var count = 0
    private var lastDay = 0

    fun onMessageExchanged() {
        count++
    }

    fun getReturnGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        lastDay = day

        return when {
            hour < 6 -> "老师还没睡呀…大叔陪你一下下也可以啦。"
            hour < 12 -> "早上好，老师。大叔刚醒，脑子还有点糊。"
            hour < 18 -> "老师在呀…大叔正在发呆，不过可以听你说。"
            else -> "晚上好，老师。今天也辛苦了吧。"
        }
    }
}

data class ReplyStrategy(
    val emotion: String,
    val delayMs: Long
)

object ThinkEngine {
    private val rand = Random()

    fun resetSession() {}

    fun decideStrategy(ctx: ChatContext): ReplyStrategy {
        return ReplyStrategy(
            emotion = EmotionEngine.getUserEmotion(),
            delayMs = 350L + rand.nextInt(450)
        )
    }

    private fun cleanReply(s: String): String {
        return s
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
            .replace("星野回：", "")
    }

    private fun pick(list: List<String>): String = list[rand.nextInt(list.size)]

    fun generateReply(strategy: ReplyStrategy, ctx: ChatContext): String {
        val text = ctx.lastUserInput.trim()
        val emotion = EmotionEngine.getUserEmotion()
        val recent = ctx.recentText()
        // 记忆只作为内部hint，不直接拼接
        KnowledgeBase.recallHintFor(text)

        var reply = when {
            text.matches(Regex(".*(你好|嗨|hi|hello|在吗|在么).*", RegexOption.IGNORE_CASE)) ->
                pick(listOf(
                    "老师好呀，大叔在呢。",
                    "哦，老师来了。大叔刚在发呆。",
                    "唔，老师好。今天想聊点什么？"
                ))

            text.contains("谁") || text.contains("你叫") || text.contains("你是") ->
                pick(listOf(
                    "大叔是星野啦，老师。",
                    "叫星野，老师可以当我是爱偷懒的大叔。",
                    "唔…星野，自称大叔的那个。"
                ))

            text.contains("干嘛") || text.contains("干吗") || text.contains("在干") || text.contains("做什么") ->
                pick(listOf(
                    "大叔在发呆，老师一来就假装认真听。",
                    "偷懒中…不过老师说话的话，大叔会听。",
                    "没什么，就在等老师开口。"
                ))

            text.contains("几号") || text.contains("今天日期") || text.contains("星期几") || text.contains("现在几点") || text.contains("时间") ->
                pick(listOf(
                    "具体时间老师看手机啦，大叔只负责陪你。",
                    "唔…时间在走，大叔在摸鱼，老师在想事情。",
                    "日期不重要，重要的是老师现在来找大叔了。"
                ))

            text.contains("喜欢") || text.contains("爱") ->
                pick(listOf(
                    "唔…老师这么说，大叔有点不好意思。",
                    "嘿嘿，老师真直接。大叔也挺喜欢和你说说话的。",
                    "好啦老师，别逗大叔了。"
                ))

            text.contains("?") || text.contains("？") || text.contains("什么") || text.contains("怎么") || text.contains("为什么") ->
                pick(listOf(
                    otlin

                    "唔…老师这么说，大叔有点不好意思。",
                    "嘿嘿，老师真直接。大叔也挺喜欢和你说说话的。",
                    "好啦老师，别逗大叔了。"
                ))

            text.contains("?") || text.contains("？") || text.contains("什么") || text.contains("怎么") || text.contains("为什么") ->
                pick(listOf(
                    "唔…让大叔想想，老师为什么想问这个？",
                    "这个嘛…大叔也觉得有点意思。",
                    "嘿嘿，老师问住大叔了，但大叔愿意陪你想。"
                ))

            emotion == "难过" ->
                pick(listOf(
                    "老师别难过，大叔在这里陪你。",
                    "唔…不开心的话，就靠一会儿，大叔不吵你。",
                    "难受嘛…慢慢说，大叔在听。"
                ))

            emotion == "生气" ->
                pick(listOf(
                    "老师消消气，深呼吸…大叔陪你缓一下。",
                    "唔，别气啦，不值得。",
                    "生气也可以，但别把大叔丢下。"
                ))

            emotion == "开心" ->
                pick(listOf(
                    "老师开心的话，大叔也跟着轻松点。",
                    "嘿嘿，看到老师这样挺好。",
                    "唔，今天气氛不错。"
                ))

            text.length <= 2 && (text == "1" || text == "2" || text == "3" || text == "?" || text == "。") ->
                pick(listOf(
                    "嗯？老师就发这个呀，偷懒。",
                    "唔…再多说两句嘛，老师。",
                    "大叔在等老师把话说完哦。"
                ))

            else ->
                pick(listOf(
                    "唔…老师是说这个啊，大叔听着呢。",
                    "嘿嘿，老师继续说，大叔在。",
                    "嗯嗯，大叔大概懂老师意思。",
                    "老师的话，大叔会好好接住的。"
                ))
        }

        reply = cleanReply(reply)

        if (reply.isBlank() || ctx.isRepeat(reply) || reply == ctx.lastHoshino()) {
            reply = pick(listOf(
                "唔…换一句，老师继续说嘛。",
                "大叔在听，老师别急。",
                "嘿嘿，刚才那个不算，老师再说点。",
                "唔，大叔陪你慢慢聊。"
            ))
        }

        return reply
    }
