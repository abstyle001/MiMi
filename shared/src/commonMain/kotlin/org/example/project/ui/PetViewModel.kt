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
import org.example.project.pet.GrowthEngine
import org.example.project.pet.PetState
import org.example.project.pet.level
import org.example.project.pet.xpIntoCurrentLevel

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
        )
    )
    val ui: StateFlow<PetUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { bootstrap() }
        viewModelScope.launch { pollStepsLoop() }
    }

    private suspend fun bootstrap() {
        val now = nowEpochMs()
        var pet = GrowthEngine.applyTimeDecay(repository.loadPet(), now)

        val today = todayString()
        if (repository.lastOpenDate() != today) {
            repository.setLastOpenDate(today)
            pet = addXp(pet, GrowthEngine.DAILY_OPEN_XP, now)
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
            )
        }
        refreshStepsInternal()
    }

    private suspend fun pollStepsLoop() {
        while (viewModelScope.isActive) {
            delay(STEP_POLL_INTERVAL_MS)
            refreshStepsInternal()
        }
    }

    // ---- 步数 ----

    fun refreshSteps() {
        viewModelScope.launch { refreshStepsInternal() }
    }

    private suspend fun refreshStepsInternal() {
        val cumulative = stepDataSource.cumulativeSteps()
        if (cumulative == null) {
            _ui.update { it.copy(stepsSupported = false) }
            return
        }
        val today = todayString()
        var baseline = if (repository.stepBaselineDate() == today) repository.stepBaseline() else -1L
        if (baseline < 0 || cumulative < baseline) {
            baseline = cumulative
            repository.setStepBaseline(today, baseline)
        }
        val stepsToday = (cumulative - baseline).coerceAtLeast(0L)
        val earnedXp = GrowthEngine.xpForSteps(stepsToday)
        val givenXp = if (repository.stepXpDate() == today) repository.stepXpToday() else 0
        var pet = _ui.value.pet
        if (earnedXp > givenXp) {
            pet = addXp(pet, earnedXp - givenXp, nowEpochMs())
            repository.setStepXp(today, earnedXp)
            repository.savePet(pet)
            notify("今日已走 $stepsToday 步，经验 +${earnedXp - givenXp}")
        }
        _ui.update { it.copy(pet = pet, todaySteps = stepsToday, todayStepXp = earnedXp, stepsSupported = true) }
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
        notify("${_ui.value.pet.name}吃得很开心！饱食度 +${GrowthEngine.FEED_HUNGER_RESTORE}")
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
        val now = nowEpochMs()
        var pet = _ui.value.pet.copy(
            energy = GrowthEngine.energyForSleep(hours),
            lastUpdatedEpochMs = now,
        )
        val xp = GrowthEngine.xpForSleep(hours)
        pet = addXp(pet, xp, now)
        pet = pet.copy(
            mood = GrowthEngine.resolveMood(pet.hunger, pet.energy, _ui.value.weather?.weatherCode, hours)
        )
        repository.savePet(pet)
        _ui.update { it.copy(pet = pet, sleepCheckedToday = true, lastSleepHours = hours) }
        notify(
            if (xp > 0) "睡眠打卡成功，经验 +$xp，精力恢复到 ${pet.energy}"
            else "睡得太少啦，${_ui.value.pet.name}好困……"
        )
    }

    // ---- 抚摸 ----

    fun petThePet() {
        val today = todayString()
        val used = if (repository.pettingDate() == today) repository.pettingCount() else 0
        val xp = GrowthEngine.xpForPetting(used)
        if (xp <= 0) {
            notify("${_ui.value.pet.name}蹭了蹭你喵～（今日抚摸经验已用完）")
            return
        }
        repository.setPetting(today, used + 1)
        val pet = addXp(_ui.value.pet, xp, nowEpochMs())
        repository.savePet(pet)
        _ui.update { it.copy(pet = pet, pettingsUsedToday = used + 1) }
        notify("摸摸头～经验 +$xp")
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
                pet = addXp(pet, GrowthEngine.CHAT_XP_PER_MESSAGE, now)
                chatXp = (chatXp + GrowthEngine.CHAT_XP_PER_MESSAGE).coerceAtMost(GrowthEngine.MAX_CHAT_XP_PER_DAY)
                repository.setChatXp(today, chatXp)
                repository.savePet(pet)
            }

            val userMsg = ChatMessage(ChatMessage.Role.USER, trimmed, now)
            _ui.update {
                it.copy(pet = pet, chatMessages = it.chatMessages + userMsg, chatSending = true, chatXpToday = chatXp)
            }

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
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage(
                        ChatMessage.Role.PET, reply.text, nowEpochMs(), fromLlm = reply.fromLlm,
                    ),
                    chatSending = false,
                )
            }
        }
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

    private fun addXp(pet: PetState, amount: Int, now: Long): PetState {
        if (amount <= 0) return pet
        val levelBefore = pet.level
        val updated = GrowthEngine.addXp(pet, amount, now)
        if (updated.level > levelBefore) {
            notify("🎉 升级啦！${pet.name} 现在是 Lv.${updated.level}")
        }
        return updated
    }

    private fun notify(message: String) {
        _ui.update { it.copy(notice = message) }
    }

    private companion object {
        const val STEP_POLL_INTERVAL_MS = 30_000L
    }
}
