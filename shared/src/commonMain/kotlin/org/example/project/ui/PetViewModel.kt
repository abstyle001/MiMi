package org.example.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.example.project.chat.ChatMessage
import org.example.project.chat.LlmChatService
import org.example.project.chat.LlmConfig
import org.example.project.chat.PetPersona
import org.example.project.data.CityPreset
import org.example.project.data.PetRepository
import org.example.project.data.StepDataSource
import org.example.project.data.WeatherDataSource
import org.example.project.data.WeatherInfo
import org.example.project.data.nowEpochMs
import org.example.project.data.todayString
import org.example.project.pet.Achievement
import org.example.project.pet.AchievementState
import org.example.project.pet.AchievementStats
import org.example.project.pet.DailyQuestState
import org.example.project.pet.GrowthEngine
import org.example.project.pet.PetState
import org.example.project.pet.QuestActivity
import org.example.project.pet.MAX_PET_NAME_LENGTH
import org.example.project.pet.achievementProgress
import org.example.project.pet.checkAchievements
import org.example.project.pet.level
import org.example.project.pet.sanitizePetName

data class PetUiState(
    val pet: PetState = PetState(),
    val todaySteps: Long = 0,
    val todayStepXp: Int = 0,
    val stepsSupported: Boolean = true,
    val weather: WeatherInfo? = null,
    val weatherDesc: String = "加载中…",
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatSending: Boolean = false,
    val chatXpToday: Int = 0,
    val feedsUsedToday: Int = 0,
    val pettingsUsedToday: Int = 0,
    val sleepCheckedToday: Boolean = false,
    val lastSleepHours: Double? = null,
    val dailyQuests: List<DailyQuestState> = emptyList(),
    val achievements: List<AchievementState> = emptyList(),
    val llmConfig: LlmConfig = LlmConfig(),
    val city: CityPreset = CityPreset.DEFAULT,
    /** 一次性提示条（升级 / 结算等），展示几秒后清除 */
    val notice: String? = null,
)

class PetViewModel(
    private val repository: PetRepository,
    private val stepDataSource: StepDataSource,
    private val weatherDataSource: WeatherDataSource,
    private val chatService: LlmChatService,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        PetUiState(
            llmConfig = repository.loadLlmConfig(),
            city = repository.loadCity(),
            lastSleepHours = repository.lastSleepHours(),
            chatMessages = repository.loadChatMessages(),
        )
    )
    val ui: StateFlow<PetUiState> = _ui.asStateFlow()

    private var lastTickDate: String = todayString()

    init {
        viewModelScope.launch { bootstrap() }
        viewModelScope.launch { tickLoop() }
    }

    private suspend fun bootstrap() {
        val now = nowEpochMs()
        var pet = GrowthEngine.applyTimeDecay(repository.loadPet(), now)

        val today = todayString()
        if (repository.lastOpenDate() != today) {
            repository.setLastOpenDate(today)
            pet = addXp(pet, GrowthEngine.DAILY_OPEN_XP)
        }

        val weather = weatherDataSource.currentWeather(_ui.value.city.lat, _ui.value.city.lon)
        pet = pet.copy(
            mood = GrowthEngine.resolveMood(pet.hunger, pet.energy, weather?.weatherCode, repository.lastSleepHours())
        )
        repository.savePet(pet)
        _ui.update {
            it.copy(
                pet = pet,
                weather = weather,
                weatherDesc = weather?.let { w -> GrowthEngine.weatherDescription(w.weatherCode) } ?: "获取失败",
                sleepCheckedToday = repository.sleepSettledDate() == today,
                chatXpToday = if (repository.chatXpDate() == today) repository.chatXpToday() else 0,
                feedsUsedToday = if (repository.feedDate() == today) repository.feedCount() else 0,
                pettingsUsedToday = if (repository.pettingDate() == today) repository.pettingCount() else 0,
                dailyQuests = buildQuests(today),
            )
        }
        refreshStepsInternal()
    }

    /** 周期任务：跨天重置、在线衰减结算、步数刷新 */
    private suspend fun tickLoop() {
        while (viewModelScope.isActive) {
            delay(TICK_INTERVAL_MS)
            checkDateRollover()
            applyLiveDecay()
            refreshStepsInternal()
        }
    }

    /** 挂过 0 点后，把 UI 缓存的「今日」计数按新日期重新推导（仓库层本身按日存取，无需清理） */
    private fun checkDateRollover() {
        val today = todayString()
        if (today == lastTickDate) return
        lastTickDate = today
        _ui.update {
            it.copy(
                sleepCheckedToday = repository.sleepSettledDate() == today,
                chatXpToday = if (repository.chatXpDate() == today) repository.chatXpToday() else 0,
                feedsUsedToday = if (repository.feedDate() == today) repository.feedCount() else 0,
                pettingsUsedToday = if (repository.pettingDate() == today) repository.pettingCount() else 0,
                dailyQuests = buildQuests(today),
            )
        }
    }

    /** 会话期间的实时衰减：每满 1 小时结算一次，不足 1 小时的部分留给下次结算或启动时的离线结算 */
    private fun applyLiveDecay() {
        val now = nowEpochMs()
        val current = _ui.value.pet
        if (current.lastUpdatedEpochMs <= 0L) return
        if (now - current.lastUpdatedEpochMs < DECAY_SETTLE_INTERVAL_MS) return
        val decayed = GrowthEngine.applyTimeDecay(current, now)
        val pet = decayed.copy(
            mood = GrowthEngine.resolveMood(
                decayed.hunger, decayed.energy, _ui.value.weather?.weatherCode, _ui.value.lastSleepHours
            )
        )
        repository.savePet(pet)
        _ui.update { it.copy(pet = pet) }
    }

    // ---- 步数 ----

    fun refreshSteps() {
        viewModelScope.launch { refreshStepsInternal() }
    }

    private suspend fun refreshStepsInternal() {
        val cumulative = stepDataSource.cumulativeSteps()
        if (cumulative == null) {
            _ui.update { it.copy(stepsSupported = false) }
            refreshAchievements()
            return
        }
        val today = todayString()
        var baseline = if (repository.stepBaselineDate() == today) repository.stepBaseline() else -1L
        if (baseline < 0 || cumulative < baseline) {
            // 新的一天或设备重启：优先用上次记录的累计读数做基线，找回打开 App 之前走的步数
            baseline = GrowthEngine.initialStepBaseline(cumulative, repository.lastStepCumulative())
            repository.setStepBaseline(today, baseline)
        }
        repository.setLastStepCumulative(cumulative)
        val stepsToday = (cumulative - baseline).coerceAtLeast(0L)
        recordLifetimeSteps(today, stepsToday)
        val earnedXp = GrowthEngine.xpForSteps(stepsToday)
        val givenXp = if (repository.stepXpDate() == today) repository.stepXpToday() else 0
        var pet = _ui.value.pet
        if (earnedXp > givenXp) {
            // 静默累计，不弹提示，避免走路时每 30 秒刷一次 Snackbar；升级提示由 addXp 负责
            pet = addXp(pet, earnedXp - givenXp)
            repository.setStepXp(today, earnedXp)
            repository.savePet(pet)
        }
        _ui.update { it.copy(pet = pet, todaySteps = stepsToday, todayStepXp = earnedXp, stepsSupported = true) }
        refreshQuests()
        refreshAchievements()
    }

    /** 终身步数按当天增量累计：同日刷新不重复加，设备重启读数回退时只保留已计入的部分 */
    private fun recordLifetimeSteps(today: String, stepsToday: Long) {
        val alreadyCounted = if (repository.stepsCountedDate() == today) repository.stepsCountedToday() else 0L
        val delta = GrowthEngine.lifetimeStepDelta(stepsToday, alreadyCounted)
        if (delta > 0) repository.addLifetimeSteps(delta)
        val counted = maxOf(alreadyCounted, stepsToday)
        if (counted != alreadyCounted || repository.stepsCountedDate() != today) {
            repository.setStepsCounted(today, counted)
        }
    }

    // ---- 天气 ----

    fun refreshWeather() {
        viewModelScope.launch {
            val weather = weatherDataSource.currentWeather(_ui.value.city.lat, _ui.value.city.lon)
            val pet = _ui.value.pet.copy(
                mood = GrowthEngine.resolveMood(
                    _ui.value.pet.hunger, _ui.value.pet.energy, weather?.weatherCode, _ui.value.lastSleepHours
                )
            )
            repository.savePet(pet)
            _ui.update {
                it.copy(
                    pet = pet,
                    weather = weather,
                    weatherDesc = weather?.let { w -> GrowthEngine.weatherDescription(w.weatherCode) } ?: "获取失败",
                )
            }
        }
    }

    // ---- 喂食 ----

    fun feed() {
        val today = todayString()
        var used = if (repository.feedDate() == today) repository.feedCount() else 0
        if (used >= GrowthEngine.MAX_FEEDS_PER_DAY) {
            notify("今天已经喂过 ${GrowthEngine.MAX_FEEDS_PER_DAY} 次啦，明天再来吧")
            return
        }
        used++
        repository.setFeed(today, used)
        repository.incrementLifetimeFeeds()
        val now = nowEpochMs()
        val pet = _ui.value.pet.copy(
            hunger = (_ui.value.pet.hunger + GrowthEngine.FEED_HUNGER_RESTORE).coerceAtMost(100),
            lastUpdatedEpochMs = now,
            mood = GrowthEngine.resolveMood(
                (_ui.value.pet.hunger + GrowthEngine.FEED_HUNGER_RESTORE).coerceAtMost(100),
                _ui.value.pet.energy, _ui.value.weather?.weatherCode, _ui.value.lastSleepHours
            ),
        )
        repository.savePet(pet)
        _ui.update { it.copy(pet = pet, feedsUsedToday = used) }
        refreshQuests()
        notify("${_ui.value.pet.name}吃得很开心！饱食度 +${GrowthEngine.FEED_HUNGER_RESTORE}")
        refreshAchievements()
    }

    // ---- 睡眠打卡 ----

    fun checkInSleep(hours: Double) {
        val today = todayString()
        if (repository.sleepSettledDate() == today) {
            notify("今天已经打过卡啦，明天再来记录吧")
            return
        }
        repository.setSleepSettledDate(today)
        repository.setLastSleepHours(hours)
        repository.incrementLifetimeSleepCheckIns()
        val streak = GrowthEngine.nextSleepStreak(today, repository.sleepStreakDate(), repository.sleepStreak())
        repository.setSleepStreak(today, streak)
        val now = nowEpochMs()
        var pet = _ui.value.pet.copy(
            energy = GrowthEngine.energyForSleep(hours),
            lastUpdatedEpochMs = now,
        )
        val xp = GrowthEngine.xpForSleep(hours)
        pet = addXp(pet, xp)
        pet = pet.copy(
            mood = GrowthEngine.resolveMood(pet.hunger, pet.energy, _ui.value.weather?.weatherCode, hours)
        )
        repository.savePet(pet)
        _ui.update { it.copy(pet = pet, sleepCheckedToday = true, lastSleepHours = hours) }
        refreshQuests()
        notify(
            if (xp > 0) "睡眠打卡成功，经验 +$xp，精力恢复到 ${pet.energy}"
            else "睡得太少啦，${_ui.value.pet.name}好困……"
        )
        refreshAchievements()
    }

    // ---- 抚摸 ----

    fun petThePet() {
        val today = todayString()
        val used = if (repository.pettingDate() == today) repository.pettingCount() else 0
        repository.incrementLifetimePettings()
        val xp = GrowthEngine.xpForPetting(used)
        if (xp <= 0) {
            notify("${_ui.value.pet.name}蹭了蹭你喵～（今日抚摸经验已用完）")
            refreshAchievements()
            return
        }
        repository.setPetting(today, used + 1)
        val pet = addXp(_ui.value.pet, xp)
        repository.savePet(pet)
        _ui.update { it.copy(pet = pet, pettingsUsedToday = used + 1) }
        refreshQuests()
        notify("摸摸头～经验 +$xp")
        refreshAchievements()
    }

    // ---- 聊天 ----

    fun sendChat(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _ui.value.chatSending) return
        viewModelScope.launch {
            val now = nowEpochMs()
            var pet = _ui.value.pet

            val today = todayString()
            var chatXp = if (repository.chatXpDate() == today) repository.chatXpToday() else 0
            if (chatXp < GrowthEngine.MAX_CHAT_XP_PER_DAY) {
                pet = addXp(pet, GrowthEngine.CHAT_XP_PER_MESSAGE)
                chatXp = (chatXp + GrowthEngine.CHAT_XP_PER_MESSAGE).coerceAtMost(GrowthEngine.MAX_CHAT_XP_PER_DAY)
                repository.setChatXp(today, chatXp)
                repository.savePet(pet)
            }
            // 每日消息条数独立计数（XP 封顶后仍要继续累计，供聊天任务进度使用）
            val chatMsgs = if (repository.chatMsgDate() == today) repository.chatMsgCount() else 0
            repository.setChatMsg(today, chatMsgs + 1)
            repository.incrementLifetimeChats()

            val userMsg = ChatMessage(ChatMessage.Role.USER, trimmed, now)
            _ui.update {
                val messages = it.chatMessages + userMsg
                repository.saveChatMessages(messages)
                it.copy(pet = pet, chatMessages = messages, chatSending = true, chatXpToday = chatXp)
            }
            refreshQuests()
            refreshAchievements()

            val persona = PetPersona(
                name = pet.name,
                level = pet.level,
                mood = pet.mood,
                hunger = pet.hunger,
                energy = pet.energy,
                weatherDesc = _ui.value.weatherDesc,
                temperatureC = _ui.value.weather?.temperatureC,
                todaySteps = _ui.value.todaySteps,
            )
            val reply = chatService.reply(_ui.value.llmConfig, persona, _ui.value.chatMessages, trimmed)
            _ui.update {
                val messages = it.chatMessages + ChatMessage(
                    ChatMessage.Role.PET, reply.text, nowEpochMs(), fromLlm = reply.fromLlm,
                )
                repository.saveChatMessages(messages)
                it.copy(chatMessages = messages, chatSending = false)
            }
        }
    }

    // ---- 重命名 ----

    /** 改宠物名：非法输入弹提示，合法则随宠物状态一起持久化（name 是 PetState 的既有字段，无需额外 KV） */
    fun renamePet(raw: String) {
        val name = sanitizePetName(raw)
        if (name == null) {
            notify("名字不能为空，最多 ${MAX_PET_NAME_LENGTH} 个字符")
            return
        }
        if (name == _ui.value.pet.name) return
        val pet = _ui.value.pet.copy(name = name)
        repository.savePet(pet)
        _ui.update { it.copy(pet = pet) }
        notify("已改名为「$name」")
    }

    // ---- 设置 ----

    fun saveSettings(config: LlmConfig, city: CityPreset) {
        repository.saveLlmConfig(config)
        repository.saveCity(city)
        _ui.update { it.copy(llmConfig = config, city = city) }
        notify("设置已保存")
        refreshWeather()
    }

    fun clearNotice() {
        _ui.update { it.copy(notice = null) }
    }

    private fun addXp(pet: PetState, amount: Int): PetState {
        if (amount <= 0) return pet
        val levelBefore = pet.level
        val updated = GrowthEngine.addXp(pet, amount)
        if (updated.level > levelBefore) {
            notify("🎉 升级啦！${pet.name} 现在是 Lv.${updated.level}")
        }
        return updated
    }

    // ---- 每日任务 ----

    /** 从仓库层按日计数重建当天任务快照（进度评估全走 GrowthEngine 纯函数） */
    private fun buildQuests(today: String = todayString()): List<DailyQuestState> {
        val activity = QuestActivity(
            steps = _ui.value.todaySteps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            feedCount = if (repository.feedDate() == today) repository.feedCount() else 0,
            chatMessageCount = if (repository.chatMsgDate() == today) repository.chatMsgCount() else 0,
            sleepChecked = repository.sleepSettledDate() == today,
            pettingCount = if (repository.pettingDate() == today) repository.pettingCount() else 0,
        )
        val claimed = repository.claimedQuestIds(today)
        return GrowthEngine.selectDailyQuests(today).map { quest ->
            DailyQuestState(quest, GrowthEngine.questProgress(quest, activity), quest.id in claimed)
        }
    }

    /** 行为后刷新任务进度（在 UI/仓库更新完成后调用） */
    private fun refreshQuests() {
        _ui.update { it.copy(dailyQuests = buildQuests()) }
    }

    fun claimDailyQuests() {
        val today = todayString()
        val claimable = _ui.value.dailyQuests.filter { it.completed && !it.claimed }
        if (claimable.isEmpty()) return
        val xp = claimable.sumOf { it.quest.xpReward }
        repository.addClaimedQuestIds(today, claimable.map { it.quest.id }.toSet())
        val pet = addXp(_ui.value.pet, xp)
        repository.savePet(pet)
        _ui.update { it.copy(pet = pet, dailyQuests = buildQuests(today)) }
        notify("任务完成！经验 +$xp")
        refreshAchievements()
    }

    // ---- 成就 ----

    /** 用终身计数重算成就墙；新解锁立即持久化（成就永不回收，进度回退不影响已解锁状态） */
    private fun refreshAchievements() {
        val stats = AchievementStats(
            level = _ui.value.pet.level,
            totalSteps = repository.lifetimeSteps(),
            chatMessages = repository.lifetimeChats(),
            feeds = repository.lifetimeFeeds(),
            pettings = repository.lifetimePettings(),
            sleepCheckIns = repository.lifetimeSleepCheckIns(),
            sleepStreak = repository.sleepStreak(),
        )
        val previouslyUnlocked = repository.unlockedAchievements()
        val newlyUnlocked = checkAchievements(stats).filter { it.name !in previouslyUnlocked }
        if (newlyUnlocked.isNotEmpty()) {
            repository.addUnlockedAchievements(newlyUnlocked.map { it.name }.toSet())
        }
        val unlocked = previouslyUnlocked + newlyUnlocked.map { it.name }
        _ui.update {
            it.copy(
                achievements = Achievement.entries.map { achievement ->
                    AchievementState(
                        achievement = achievement,
                        progress = achievementProgress(achievement, stats),
                        unlocked = achievement.name in unlocked,
                    )
                }
            )
        }
        if (newlyUnlocked.isNotEmpty()) {
            notify("🏆 解锁成就：" + newlyUnlocked.joinToString("、") { it.title })
        }
    }

    private fun notify(message: String) {
        _ui.update { it.copy(notice = message) }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 30_000L
        const val DECAY_SETTLE_INTERVAL_MS = 3_600_000L
    }
}
