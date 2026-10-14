package org.example.project.chat

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.example.project.pet.PetMood

data class LlmConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_BASE_URL = "https://api.moonshot.cn/v1"
        const val DEFAULT_MODEL = "moonshot-v1-8k"
    }
}

/** 组装给 LLM 的宠物实时状态快照 */
data class PetPersona(
    val name: String,
    val level: Int,
    val mood: PetMood,
    val hunger: Int,
    val energy: Int,
    val weatherDesc: String,
    val temperatureC: Double?,
    val todaySteps: Long,
)

/** 一次回复及其来源 */
data class ChatReply(val text: String, val fromLlm: Boolean)

class LlmChatService(private val client: HttpClient) {

    suspend fun reply(
        config: LlmConfig,
        persona: PetPersona,
        history: List<ChatMessage>,
        userText: String,
    ): ChatReply {
        if (!config.isConfigured) return ChatReply(localReply(persona, userText), fromLlm = false)
        return try {
            remoteReply(config, persona, history, userText)
                ?.takeIf { it.isNotBlank() }
                ?.let { ChatReply(it, fromLlm = true) }
                ?: ChatReply(localReply(persona, userText), fromLlm = false)
        } catch (_: Exception) {
            ChatReply("喵呜……网络好像不太顺畅，${localReply(persona, userText)}", fromLlm = false)
        }
    }

    private suspend fun remoteReply(
        config: LlmConfig,
        persona: PetPersona,
        history: List<ChatMessage>,
        userText: String,
    ): String? {
        val messages = buildList {
            add(ApiMessage(role = "system", content = systemPrompt(persona)))
            history.takeLast(10).forEach { msg ->
                add(ApiMessage(role = if (msg.role == ChatMessage.Role.USER) "user" else "assistant", content = msg.content))
            }
            add(ApiMessage(role = "user", content = userText))
        }
        val response: ChatCompletionResponse = client.post("${config.baseUrl.trimEnd('/')}/chat/completions") {
            contentType(ContentType.Application.Json)
            headers.append("Authorization", "Bearer ${config.apiKey}")
            setBody(ChatCompletionRequest(model = config.model, messages = messages))
        }.body()
        return response.choices.firstOrNull()?.message?.content
    }

    private fun systemPrompt(p: PetPersona): String {
        val weatherPart = if (p.temperatureC != null) "${p.weatherDesc}，气温 ${p.temperatureC}℃" else p.weatherDesc
        return """
            你是${p.name}，一只住在手机里的宠物猫，当前 ${p.level} 级。
            当前状态：心情=${p.mood.label}，饱食度=${p.hunger}/100，精力=${p.energy}/100。
            今天外面天气：$weatherPart。主人今天走了 ${p.todaySteps} 步。
            要求：用中文回复，每次一两句话以内，带点猫的口癖（偶尔用「喵」），
            语气要符合当前心情（${p.mood.label}），可以结合天气、步数等状态，不要暴露你是 AI。
        """.trimIndent()
    }

    private fun localReply(p: PetPersona, text: String): String = when {
        text.containsAny("吃", "饿", "饭", "鱼") ->
            if (p.hunger < 40) "我好饿喵……快喂我小鱼干！🐟" else "刚吃饱，好满足喵～"
        text.containsAny("天气", "下雨", "晴", "冷", "热") ->
            "今天外面${p.weatherDesc}喵，出门记得看天哦"
        text.containsAny("步", "走", "运动", "跑") ->
            "你今天走了 ${p.todaySteps} 步，好厉害喵！继续加油～"
        text.containsAny("睡", "困", "累", "熬夜") ->
            if (p.energy < 40) "我也好困……一起休息会儿吧喵 😴" else "我精神好得很喵！"
        text.containsAny("名字", "叫什么") -> "我叫${p.name}喵！"
        text.containsAny("级", "升级", "经验") -> "我现在 ${p.level} 级啦，多陪我玩就能更快升级喵～"
        text.containsAny("你好", "在吗", "hi", "Hi") -> "你好呀，${p.mood.emoji} 今天也是${p.mood.label}的一天喵～"
        else -> moodLine(p.mood, text)
    }

    private fun moodLine(mood: PetMood, text: String): String {
        val lines = when (mood) {
            PetMood.HAPPY -> listOf("心情超好喵～", "今天阳光都甜甜的喵！", "蹭蹭你，喵～")
            PetMood.EXCITED -> listOf("好兴奋！想出去玩喵！", "喵喵喵！快陪我跑两圈！")
            PetMood.NORMAL -> listOf("在呢，喵。", "嗯嗯，我听着呢喵。", "今天也是平静的一天喵。")
            PetMood.LAZY -> listOf("懒得动喵……", "让我再趴一会儿喵……", "雨天最适合发呆了喵。")
            PetMood.SLEEPY -> listOf("好困……喵……", "眼皮打架了喵……😴")
            PetMood.SAD -> listOf("呜呜，有点没精神喵……", "陪陪我好不好喵……😿")
        }
        return lines[kotlin.math.abs(text.hashCode()) % lines.size]
    }

    private fun String.containsAny(vararg keys: String): Boolean = keys.any { contains(it) }
}

@Serializable
private data class ApiMessage(val role: String, val content: String)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val temperature: Double = 0.8,
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList(),
) {
    @Serializable
    data class Choice(val message: ApiMessage? = null)
}
