package com.example.starhoshino.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

// ========== 数据类 ==========

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: String = "neutral"
)

data class KnowledgeEntry(
    val key: String,
    val learned: String,
    val confidence: Float,
    val gaps: MutableList<String>,
    val source: String,
    val mentionCount: Int,
    val lastMentioned: Long
)

data class MemorySummary(
    val date: String,
    val summary: String,
    val emotion: String,
    val keywords: List<String>
)

data class ReplyStrategy(
    val style: String,
    val length: String,
    val delayMs: Long,
    val contentDirection: String,
    val emotion: String,
    val shouldAskBack: Boolean,
    val askContent: String
)

data class ChatContext(
    val sessionHistory: MutableList<ChatMessage>,
    var lastUserInput: String
) {
    fun addMessage(msg: ChatMessage) { sessionHistory.add(msg) }
    fun getRecent(n: Int): List<ChatMessage> {
        return if (sessionHistory.size <= n) sessionHistory else sessionHistory.takeLast(n)
    }
}

// ========== 文件传输接口（预留） ==========

interface FileTransfer {
    fun sendFileToUser(filePath: String): Boolean
    fun receiveFileFromUser(filePath: String): Boolean
}

// ========== 小红书桥接接口（预留） ==========

interface XiaohongshuBridge {
    fun sendPost(content: String, mediaUrl: String? = null): Boolean
    fun checkMessages(): List<String>
    fun isOnline(): Boolean
}

// ========== 知识库 ==========

object KnowledgeBase {
    private const val FILE_NAME = "knowledge.json"
    private val entries = mutableMapOf<String, KnowledgeEntry>()
    private val pendingQuestions = mutableListOf<String>()
    private var file: File? = null

    fun init(baseDir: File) {
        file = File(baseDir, FILE_NAME)
        if (file!!.exists()) load()
    }

    private fun load() {
        try {
            val json = JSONObject(file!!.readText())
            val arr = json.optJSONArray("entries") ?: return
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                entries[obj.getString("key")] = KnowledgeEntry(
                    key = obj.getString("key"),
                    learned = obj.getString("learned"),
                    confidence = obj.optDouble("confidence", 0.5).toFloat(),
                    gaps = mutableListOf<String>().apply {
                        val g = obj.optJSONArray("gaps")
                        if (g != null) for (j in 0 until g.length()) add(g.getString(j))
                    },
                    source = obj.optString("source", "chat"),
                    mentionCount = obj.optInt("mentionCount", 1),
                    lastMentioned = obj.optLong("lastMentioned", System.currentTimeMillis())
                )
            }
            val pq = json.optJSONArray("pendingQuestions")
            if (pq != null) for (i in 0 until pq.length()) pendingQuestions.add(pq.getString(i))
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun save() {
        try {
            val json = JSONObject()
            val arr = JSONArray()
            for (e in entries.values) {
                val obj = JSONObject()
                obj.put("key", e.key)
                obj.put("learned", e.learned)
                obj.put("confidence", e.confidence)
                val g = JSONArray()
                for (gap in e.gaps) g.put(gap)
                obj.put("gaps", g)
                obj.put("source", e.source)
                obj.put("mentionCount", e.mentionCount)
                obj.put("lastMentioned", e.lastMentioned)
                arr.put(obj)
            }
            json.put("entries", arr)
            val pq = JSONArray()
            for (q in pendingQuestions) pq.put(q)
            json.put("pendingQuestions", pq)
            file?.writeText(json.toString(2))
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun learn(key: String, content: String, confidence: Float = 0.6f): Boolean {
        if (entries.containsKey(key)) {
            val e = entries[key]!!
            entries[key] = e.copy(mentionCount = e.mentionCount + 1, lastMentioned = System.currentTimeMillis())
            return false
        }
        entries[key] = KnowledgeEntry(key, content, confidence, mutableListOf(), "chat", 1, System.currentTimeMillis())
        return true
    }

    fun get(key: String): KnowledgeEntry? = entries[key]
    fun searchByKeyword(keyword: String): List<KnowledgeEntry> = entries.values.filter { it.learned.contains(keyword) || it.key.contains(keyword) }
    fun getLowConfidence(threshold: Float = 0.3f): List<KnowledgeEntry> = entries.values.filter { it.confidence < threshold }
    fun addPendingQuestion(q: String) { if (!pendingQuestions.contains(q)) pendingQuestions.add(q) }
    fun getPendingQuestions(): List<String> = pendingQuestions.toList()
    fun removePendingQuestion(q: String) { pendingQuestions.remove(q) }
    fun getAllEntries(): Map<String, KnowledgeEntry> = entries.toMap()
}

// ========== 情绪引擎 ==========

object EmotionEngine {
    private val positiveWords = listOf("开心", "哈哈", "喜欢", "好耶", "棒", "爱", "舒服", "爽", "nice", "赞", "嘻嘻", "嘿嘿")
    private val negativeWords = listOf("烦", "生气", "累", "想死", "难受", "痛", "恶心", "讨厌", "崩溃", "痛苦", "绝望")
    private val angryWords = listOf("骂", "气死", "滚", "去死")
    private var currentUserEmotion = "neutral"
    private var currentHoshinoEmotion = "neutral"

    fun detectUserEmotion(text: String): String {
        val lower = text.lowercase()
        var score = 0
        positiveWords.forEach { if (lower.contains(it)) score++ }
        negativeWords.forEach { if (lower.contains(it)) score-- }
        currentUserEmotion = when {
            score >= 2 -> "positive"
            score <= -2 -> if (angryWords.any { lower.contains(it) }) "angry" else "negative"
            score == -1 -> "negative"
            score == 1 -> "positive"
            else -> "neutral"
        }
        return currentUserEmotion
    }

    fun getHoshinoEmotion(): String = currentHoshinoEmotion
    fun setHoshinoEmotion(e: String) { currentHoshinoEmotion = e }
    fun getUserEmotion(): String = currentUserEmotion
}

// ========== 亲密度系统 ==========

object BondSystem {
    private var intimacy: Float = 0.15f
    private var lastOpenTime: Long = 0
    private var sessionStart: Long = 0
    private var totalMessagesThisSession: Int = 0
    private var selfAwareCooldownUntil: Long = 0

    fun init(saved: Float? = null) { if (saved != null) intimacy = saved }

    fun onSessionStart() {
        sessionStart = System.currentTimeMillis()
        totalMessagesThisSession = 0
    }

    fun onMessageExchanged() {
        totalMessagesThisSession++
        if (totalMessagesThisSession > 0 && totalMessagesThisSession % 10 == 0) {
            intimacy = min(1.0f, intimacy + 0.02f)
        }
    }

    fun getIntimacy(): Float = intimacy
    fun getStage(): String = when {
        intimacy < 0.25f -> "陌生人"
        intimacy < 0.45f -> "熟人"
        intimacy < 0.65f -> "朋友"
        intimacy < 0.85f -> "亲密"
        else -> "灵魂伴侣"
    }

    fun addIntimacy(v: Float) { intimacy = min(1.0f, intimacy + v) }
    fun subIntimacy(v: Float) { intimacy = maxOf(0.0f, intimacy - v) }

    fun shouldSelfAware(): Boolean {
        val now = System.currentTimeMillis()
        if (now < selfAwareCooldownUntil) return false
        selfAwareCooldownUntil = now + 3 * 24 * 60 * 60 * 1000L
        return true
    }

    fun getReturnGreeting(): String {
        val away = System.currentTimeMillis() - lastOpenTime
        lastOpenTime = System.currentTimeMillis()
        return when {
            away < 5 * 60 * 1000 -> "呼啊~前辈又来啦。刚睡醒…才怪。"
            away < 30 * 60 * 1000 -> "哼，去哪了啊。大叔我等了好久…才没有。"
            away < 2 * 3600 * 1000 -> "前辈…你知不知道我有多…算了，反正你也不在乎。"
            away < 24 * 3600 * 1000 -> "笨蛋！一天都不来找我！（小声）…欢迎回来。"
            else -> "前辈…你还记得大叔我吗。（眼眶红红）"
        }
    }

    fun saveState(): JSONObject {
        val j = JSONObject()
        j.put("intimacy", intimacy)
        j.put("lastOpenTime", lastOpenTime)
        return j
    }

    fun loadState(j: JSONObject) {
        intimacy = j.optDouble("intimacy", 0.15).toFloat()
        lastOpenTime = j.optLong("lastOpenTime", 0L)
    }
}

// ========== Recall引擎 ==========

object RecallEngine {
    private const val FILE_NAME = "memory_index.json"
    private val memories = mutableListOf<MemorySummary>()
    private var file: File? = null

    fun init(baseDir: File) {
        file = File(baseDir, FILE_NAME)
        if (file!!.exists()) load()
    }

    private fun load() {
        try {
            val arr = JSONArray(file!!.readText())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                memories.add(MemorySummary(
                    date = obj.getString("date"),
                    summary = obj.getString("summary"),
                    emotion = obj.optString("emotion", "neutral"),
                    keywords = obj.optJSONArray("keywords")?.let { k ->
                        (0 until k.length()).map { k.getString(it) }
                    } ?: emptyList()
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun save() {
        try {
            val arr = JSONArray()
            for (m in memories) {
                val obj = JSONObject()
                obj.put("date", m.date)
                obj.put("summary", m.summary)
                obj.put("emotion", m.emotion)
                val k = JSONArray()
                for (kw in m.keywords) k.put(kw)
                obj.put("keywords", k)
                arr.put(obj)
            }
            file?.writeText(arr.toString(2))
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun addMemory(summary: String, emotion: String, keywords: List<String>) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        memories.add(MemorySummary(date, summary, emotion, keywords))
    }

    fun recall(context: String, emotion: String, topN: Int = 3): List<MemorySummary> {
        val contextWords = context.lowercase().split(" ", "，", "。", "？", "！", "\n")
        val scored = memories.map { mem ->
            var score = 0
            for (kw in mem.keywords) if (context.contains(kw.lowercase())) score += 3
            for (w in contextWords) if (mem.summary.contains(w)) score += 1
            if (mem.emotion == emotion) score += 1
            score to mem
        }
        return scored.filter { it.first > 0 }.sortedByDescending { it.first }.take(topN).map { it.second }
    }
}

// ========== 模板池 ==========

object TemplatePool {
    private val templates = mapOf(
        "POLITE" to listOf("前辈好呀~", "嗯，我知道了。", "呼啊~前辈在说什么呢。", "这样啊…大叔我明白了。"),
        "CASUAL" to listOf("哦？然后呢？", "哈哈哈前辈真是的~", "嗯嗯，然后？", "大叔我也觉得啦~"),
        "PLAYFUL" to listOf("前辈笨蛋~", "诶嘿~被我猜到了吧！", "大叔我才不会告诉你呢~", "哼哼~前辈你完蛋了！"),
        "TSUNDERE" to listOf("哼！才不是在乎你呢！", "笨蛋前辈…", "谁、谁担心你了啊！大叔我只是刚好醒着！", "哼，随便你啦。"),
        "CUTE" to listOf("前辈前辈~陪我嘛~", "呼啊…好困…前辈不要走…", "大叔我呀，最喜欢前辈了…（小声）", "前辈在就好~"),
        "SUPPORTIVE" to listOf("前辈…大叔我在这里哦。", "呼…没事，我在呢。", "不想说就不说，陪着你就好。", "前辈辛苦了，休息一下吧。"),
        "JEALOUS" to listOf("哦…那个人啊。真好呢。", "前辈你什么时候交的女朋友？！…啊没有。", "哼，又提别人。大叔我什么都没听到。"),
        "SILENT" to listOf("…", "（安静地看着前辈）", "………", "（轻轻靠过来）")
    )

    fun getTemplate(style: String): String {
        val list = templates[style] ?: templates["CASUAL"]!!
        return list.random()
    }
}

// ========== 思考引擎 ==========

object ThinkEngine {
    private var messageCount = 0
    private var consecutiveAsks = 0
    private var lastAskAt = 0

    fun decideStrategy(context: ChatContext): ReplyStrategy {
        messageCount++
        val userEmo = EmotionEngine.getUserEmotion()
        val recalled = RecallEngine.recall(context.lastUserInput, userEmo, 3)
        val stage = BondSystem.getIntimacy()

        val style = when {
            userEmo == "negative" || userEmo == "angry" -> { EmotionEngine.setHoshinoEmotion("supportive"); "SUPPORTIVE" }
            stage >= 0.65f && userEmo == "positive" -> "CUTE"
            stage >= 0.45f && recalled.isNotEmpty() -> "CASUAL"
            stage >= 0.45f -> "PLAYFUL"
            stage >= 0.25f -> "CASUAL"
            else -> "POLITE"
        }

        val length = when (style) {
            "SILENT" -> "SHORT"
            "SUPPORTIVE" -> "MEDIUM"
            "CUTE" -> "MEDIUM"
            else -> "NORMAL"
        }

        var shouldAsk = false
        var askContent = ""
        val lowConf = KnowledgeBase.getLowConfidence(0.3f)
        if (lowConf.isNotEmpty() && consecutiveAsks < 3 && messageCount - lastAskAt >= 6) {
            val target = lowConf.first()
            if (target.gaps.isNotEmpty()) {
                shouldAsk = true
                askContent = target.gaps.first()
                lastAskAt = messageCount
                consecutiveAsks++
            }
        }
        if (context.lastUserInput.contains("？") || context.lastUserInput.contains("?")) {
            consecutiveAsks = 0
        }

        val delayMs = when (style) {
            "TSUNDERE" -> 800L + (Math.random() * 500).toLong()
            "CUTE" -> 300L + (Math.random() * 300).toLong()
            "SILENT" -> 1500L
            else -> 400L + (Math.random() * 400).toLong()
        }

        return ReplyStrategy(
            style = style,
            length = length,
            delayMs = delayMs,
            contentDirection = recalled.joinToString(" ") { it.summary },
            emotion = EmotionEngine.getHoshinoEmotion(),
            shouldAskBack = shouldAsk,
            askContent = askContent
        )
    }

    fun generateReply(strategy: ReplyStrategy, context: ChatContext): String {
        val sb = StringBuilder()
        val skeleton = TemplatePool.getTemplate(strategy.style)

        if (strategy.contentDirection.isNotBlank()) {
            val recallSnippet = strategy.contentDirection.take(30)
            sb.append("啊，对了…之前前辈说过$recallSnippet 大叔我记得呢。")
        }

        if (strategy.shouldAskBack) {
            sb.append(" 话说，").append(strategy.askContent).append(" 前辈？")
            return sb.toString().take(50)
        }

        if (sb.isEmpty()) sb.append(skeleton)

        val intimacy = BondSystem.getIntimacy()
        if (intimacy > 0.65f && Math.random() < 0.3) {
            sb.append(" 前辈最近都来找我呢…大叔我好开心哦。（小声）")
        }

        return sb.toString().take(
            when (strategy.length) {
                "SHORT" -> 15
                "MEDIUM" -> 25
                "NORMAL" -> 40
                "LONG" -> 50
                else -> 30
            }
        )
    }

    fun resetSession() {
        messageCount = 0
        consecutiveAsks = 0
        lastAskAt = 0
    }
}
