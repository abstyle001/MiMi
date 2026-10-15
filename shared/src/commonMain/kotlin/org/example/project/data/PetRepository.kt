package org.example.project.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.project.chat.ChatMessage
import org.example.project.chat.LlmConfig
import org.example.project.pet.PetState

class PetRepository(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ---- 宠物状态 ----

    fun loadPet(): PetState =
        settings.getStringOrNull(KEY_PET)
            ?.let { runCatching { json.decodeFromString<PetState>(it) }.getOrNull() }
            ?: PetState()

    fun savePet(pet: PetState) {
        settings.putString(KEY_PET, json.encodeToString(PetState.serializer(), pet))
    }

    // ---- 步数结算 ----

    fun stepBaselineDate(): String = settings.getString(KEY_STEP_BASELINE_DATE, "")
    fun stepBaseline(): Long = settings.getLong(KEY_STEP_BASELINE, -1L)
    fun setStepBaseline(date: String, baseline: Long) {
        settings.putString(KEY_STEP_BASELINE_DATE, date)
        settings.putLong(KEY_STEP_BASELINE, baseline)
    }

    fun stepXpDate(): String = settings.getString(KEY_STEP_XP_DATE, "")
    fun stepXpToday(): Int = settings.getInt(KEY_STEP_XP_TODAY, 0)
    fun setStepXp(date: String, xp: Int) {
        settings.putString(KEY_STEP_XP_DATE, date)
        settings.putInt(KEY_STEP_XP_TODAY, xp)
    }

    /** 最近一次成功读到的传感器累计步数，用作新一天的基线种子 */
    fun lastStepCumulative(): Long = settings.getLong(KEY_STEP_LAST_CUMULATIVE, -1L)
    fun setLastStepCumulative(cumulative: Long) = settings.putLong(KEY_STEP_LAST_CUMULATIVE, cumulative)

    // ---- 每日首次打开 ----

    fun lastOpenDate(): String = settings.getString(KEY_LAST_OPEN_DATE, "")
    fun setLastOpenDate(date: String) = settings.putString(KEY_LAST_OPEN_DATE, date)

    // ---- 聊天 XP ----

    fun chatXpDate(): String = settings.getString(KEY_CHAT_XP_DATE, "")
    fun chatXpToday(): Int = settings.getInt(KEY_CHAT_XP_TODAY, 0)
    fun setChatXp(date: String, xp: Int) {
        settings.putString(KEY_CHAT_XP_DATE, date)
        settings.putInt(KEY_CHAT_XP_TODAY, xp)
    }

    // ---- 喂食 ----

    fun feedDate(): String = settings.getString(KEY_FEED_DATE, "")
    fun feedCount(): Int = settings.getInt(KEY_FEED_COUNT, 0)
    fun setFeed(date: String, count: Int) {
        settings.putString(KEY_FEED_DATE, date)
        settings.putInt(KEY_FEED_COUNT, count)
    }

    // ---- 抚摸 ----

    fun pettingDate(): String = settings.getString(KEY_PETTING_DATE, "")
    fun pettingCount(): Int = settings.getInt(KEY_PETTING_COUNT, 0)
    fun setPetting(date: String, count: Int) {
        settings.putString(KEY_PETTING_DATE, date)
        settings.putInt(KEY_PETTING_COUNT, count)
    }

    // ---- 睡眠 ----

    fun sleepSettledDate(): String = settings.getString(KEY_SLEEP_SETTLED_DATE, "")
    fun setSleepSettledDate(date: String) = settings.putString(KEY_SLEEP_SETTLED_DATE, date)
    fun lastSleepHours(): Double? =
        if (settings.hasKey(KEY_LAST_SLEEP_HOURS)) settings.getDouble(KEY_LAST_SLEEP_HOURS, 0.0) else null
    fun setLastSleepHours(hours: Double) = settings.putDouble(KEY_LAST_SLEEP_HOURS, hours)

    // ---- 聊天记录 ----

    fun loadChatMessages(): List<ChatMessage> =
        settings.getStringOrNull(KEY_CHAT_MESSAGES)
            ?.let { runCatching { json.decodeFromString(ListSerializer(ChatMessage.serializer()), it) }.getOrNull() }
            ?: emptyList()

    fun saveChatMessages(messages: List<ChatMessage>) {
        val trimmed = messages.takeLast(MAX_CHAT_HISTORY)
        settings.putString(KEY_CHAT_MESSAGES, json.encodeToString(ListSerializer(ChatMessage.serializer()), trimmed))
    }

    // ---- LLM 配置 ----

    fun loadLlmConfig(): LlmConfig = LlmConfig(
        baseUrl = settings.getString(KEY_LLM_BASE_URL, LlmConfig.DEFAULT_BASE_URL),
        apiKey = settings.getString(KEY_LLM_API_KEY, ""),
        model = settings.getString(KEY_LLM_MODEL, LlmConfig.DEFAULT_MODEL),
    )

    fun saveLlmConfig(config: LlmConfig) {
        settings.putString(KEY_LLM_BASE_URL, config.baseUrl)
        settings.putString(KEY_LLM_API_KEY, config.apiKey)
        settings.putString(KEY_LLM_MODEL, config.model)
    }

    // ---- 城市 ----

    fun loadCity(): CityPreset {
        val name = settings.getString(KEY_CITY_NAME, CityPreset.DEFAULT.name)
        return CityPreset.PRESETS.firstOrNull { it.name == name } ?: CityPreset.DEFAULT
    }

    fun saveCity(city: CityPreset) = settings.putString(KEY_CITY_NAME, city.name)

    private companion object {
        const val KEY_PET = "pet_state"
        const val KEY_STEP_BASELINE = "step_baseline"
        const val KEY_STEP_BASELINE_DATE = "step_baseline_date"
        const val KEY_STEP_XP_DATE = "step_xp_date"
        const val KEY_STEP_XP_TODAY = "step_xp_today"
        const val KEY_STEP_LAST_CUMULATIVE = "step_last_cumulative"
        const val KEY_LAST_OPEN_DATE = "last_open_date"
        const val KEY_CHAT_XP_DATE = "chat_xp_date"
        const val KEY_CHAT_XP_TODAY = "chat_xp_today"
        const val KEY_FEED_DATE = "feed_date"
        const val KEY_FEED_COUNT = "feed_count"
        const val KEY_PETTING_DATE = "petting_date"
        const val KEY_PETTING_COUNT = "petting_count"
        const val KEY_SLEEP_SETTLED_DATE = "sleep_settled_date"
        const val KEY_LAST_SLEEP_HOURS = "last_sleep_hours"
        const val KEY_CHAT_MESSAGES = "chat_messages"
        const val MAX_CHAT_HISTORY = 50
        const val KEY_LLM_BASE_URL = "llm_base_url"
        const val KEY_LLM_API_KEY = "llm_api_key"
        const val KEY_LLM_MODEL = "llm_model"
        const val KEY_CITY_NAME = "city_name"
    }
}
