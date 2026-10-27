package org.example.project.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.project.chat.ChatMessage
import org.example.project.chat.LlmConfig
import org.example.project.pet.PetState
import org.example.project.pet.migrateEquippedAccessories

class PetRepository(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ---- 宠物状态 ----

    fun loadPet(): PetState =
        settings.getStringOrNull(KEY_PET)
            ?.let { runCatching { json.decodeFromString<PetState>(it) }.getOrNull() }
            ?.let { migrateEquippedAccessories(it) }
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

    // ---- 迷你游戏 XP ----

    fun gameXpDate(): String = settings.getString(KEY_GAME_XP_DATE, "")
    fun gameXpToday(): Int = settings.getInt(KEY_GAME_XP_TODAY, 0)
    fun setGameXp(date: String, xp: Int) {
        settings.putString(KEY_GAME_XP_DATE, date)
        settings.putInt(KEY_GAME_XP_TODAY, xp)
    }

    // ---- 金币 ----
    // 金币余额存在 PetState.coins；这里只记录按日结算的步数/聊天产出，避免重复发放

    fun stepCoinDate(): String = settings.getString(KEY_STEP_COIN_DATE, "")
    fun stepCoinToday(): Int = settings.getInt(KEY_STEP_COIN_TODAY, 0)
    fun setStepCoin(date: String, coins: Int) {
        settings.putString(KEY_STEP_COIN_DATE, date)
        settings.putInt(KEY_STEP_COIN_TODAY, coins)
    }

    fun chatCoinDate(): String = settings.getString(KEY_CHAT_COIN_DATE, "")
    fun chatCoinToday(): Int = settings.getInt(KEY_CHAT_COIN_TODAY, 0)
    fun setChatCoin(date: String, coins: Int) {
        settings.putString(KEY_CHAT_COIN_DATE, date)
        settings.putInt(KEY_CHAT_COIN_TODAY, coins)
    }

    /** 毛线球陪玩每日一次，记录日期即可 */
    fun toyPlayDate(): String = settings.getString(KEY_TOY_PLAY_DATE, "")
    fun setToyPlayDate(date: String) = settings.putString(KEY_TOY_PLAY_DATE, date)

    // ---- 每日聊天消息条数（任务进度用，与 chatXp 上限解耦） ----

    fun chatMsgDate(): String = settings.getString(KEY_CHAT_MSG_DATE, "")
    fun chatMsgCount(): Int = settings.getInt(KEY_CHAT_MSG_COUNT, 0)
    fun setChatMsg(date: String, count: Int) {
        settings.putString(KEY_CHAT_MSG_DATE, date)
        settings.putInt(KEY_CHAT_MSG_COUNT, count)
    }

    // ---- 每日任务 ----

    fun questDate(): String = settings.getString(KEY_QUEST_DATE, "")

    /** 当天已领取奖励的任务 id；跨天时自动视为空 */
    fun claimedQuestIds(date: String): Set<String> =
        if (questDate() != date) emptySet()
        else settings.getString(KEY_CLAIMED_QUEST_IDS, "").split(',').filter { it.isNotBlank() }.toSet()

    fun addClaimedQuestIds(date: String, ids: Set<String>) {
        settings.putString(KEY_QUEST_DATE, date)
        val merged = claimedQuestIds(date) + ids
        settings.putString(KEY_CLAIMED_QUEST_IDS, merged.joinToString(","))
    }

    // ---- 睡眠 ----

    fun sleepSettledDate(): String = settings.getString(KEY_SLEEP_SETTLED_DATE, "")
    fun setSleepSettledDate(date: String) = settings.putString(KEY_SLEEP_SETTLED_DATE, date)
    fun lastSleepHours(): Double? =
        if (settings.hasKey(KEY_LAST_SLEEP_HOURS)) settings.getDouble(KEY_LAST_SLEEP_HOURS, 0.0) else null
    fun setLastSleepHours(hours: Double) = settings.putDouble(KEY_LAST_SLEEP_HOURS, hours)

    // ---- 成就终身计数 ----

    fun lifetimeSteps(): Long = settings.getLong(KEY_LIFETIME_STEPS, 0L)

    fun addLifetimeSteps(amount: Long) {
        if (amount > 0) settings.putLong(KEY_LIFETIME_STEPS, lifetimeSteps() + amount)
    }

    fun stepsCountedDate(): String = settings.getString(KEY_STEPS_COUNTED_DATE, "")
    fun stepsCountedToday(): Long = settings.getLong(KEY_STEPS_COUNTED_TODAY, 0L)

    fun setStepsCounted(date: String, steps: Long) {
        settings.putString(KEY_STEPS_COUNTED_DATE, date)
        settings.putLong(KEY_STEPS_COUNTED_TODAY, steps)
    }

    fun lifetimeFeeds(): Int = settings.getInt(KEY_LIFETIME_FEEDS, 0)
    fun incrementLifetimeFeeds() = settings.putInt(KEY_LIFETIME_FEEDS, lifetimeFeeds() + 1)

    fun lifetimeChats(): Int = settings.getInt(KEY_LIFETIME_CHATS, 0)
    fun incrementLifetimeChats() = settings.putInt(KEY_LIFETIME_CHATS, lifetimeChats() + 1)

    fun lifetimePettings(): Int = settings.getInt(KEY_LIFETIME_PETTINGS, 0)
    fun incrementLifetimePettings() = settings.putInt(KEY_LIFETIME_PETTINGS, lifetimePettings() + 1)

    fun lifetimeSleepCheckIns(): Int = settings.getInt(KEY_LIFETIME_SLEEP_CHECKINS, 0)
    fun incrementLifetimeSleepCheckIns() =
        settings.putInt(KEY_LIFETIME_SLEEP_CHECKINS, lifetimeSleepCheckIns() + 1)

    fun sleepStreak(): Int = settings.getInt(KEY_SLEEP_STREAK, 0)
    fun sleepStreakDate(): String = settings.getString(KEY_SLEEP_STREAK_DATE, "")

    fun setSleepStreak(date: String, streak: Int) {
        settings.putString(KEY_SLEEP_STREAK_DATE, date)
        settings.putInt(KEY_SLEEP_STREAK, streak)
    }

    // ---- 已解锁成就 ----

    fun unlockedAchievements(): Set<String> =
        settings.getString(KEY_UNLOCKED_ACHIEVEMENTS, "")
            .split(',')
            .filter { it.isNotBlank() }
            .toSet()

    fun addUnlockedAchievements(ids: Set<String>) {
        if (ids.isEmpty()) return
        val merged = (unlockedAchievements() + ids).sorted()
        settings.putString(KEY_UNLOCKED_ACHIEVEMENTS, merged.joinToString(","))
    }

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
        const val KEY_CHAT_MSG_DATE = "chat_msg_date"
        const val KEY_CHAT_MSG_COUNT = "chat_msg_count"
        const val KEY_GAME_XP_DATE = "game_xp_date"
        const val KEY_GAME_XP_TODAY = "game_xp_today"
        const val KEY_STEP_COIN_DATE = "step_coin_date"
        const val KEY_STEP_COIN_TODAY = "step_coin_today"
        const val KEY_CHAT_COIN_DATE = "chat_coin_date"
        const val KEY_CHAT_COIN_TODAY = "chat_coin_today"
        const val KEY_TOY_PLAY_DATE = "toy_play_date"
        const val KEY_QUEST_DATE = "quest_date"
        const val KEY_CLAIMED_QUEST_IDS = "claimed_quest_ids"
        const val KEY_SLEEP_SETTLED_DATE = "sleep_settled_date"
        const val KEY_LAST_SLEEP_HOURS = "last_sleep_hours"
        const val KEY_LIFETIME_STEPS = "lifetime_steps"
        const val KEY_STEPS_COUNTED_DATE = "steps_counted_date"
        const val KEY_STEPS_COUNTED_TODAY = "steps_counted_today"
        const val KEY_LIFETIME_FEEDS = "lifetime_feeds"
        const val KEY_LIFETIME_CHATS = "lifetime_chats"
        const val KEY_LIFETIME_PETTINGS = "lifetime_pettings"
        const val KEY_LIFETIME_SLEEP_CHECKINS = "lifetime_sleep_checkins"
        const val KEY_SLEEP_STREAK = "sleep_streak"
        const val KEY_SLEEP_STREAK_DATE = "sleep_streak_date"
        const val KEY_UNLOCKED_ACHIEVEMENTS = "unlocked_achievements"
        const val KEY_CHAT_MESSAGES = "chat_messages"
        const val MAX_CHAT_HISTORY = 50
        const val KEY_LLM_BASE_URL = "llm_base_url"
        const val KEY_LLM_API_KEY = "llm_api_key"
        const val KEY_LLM_MODEL = "llm_model"
        const val KEY_CITY_NAME = "city_name"
    }
}
