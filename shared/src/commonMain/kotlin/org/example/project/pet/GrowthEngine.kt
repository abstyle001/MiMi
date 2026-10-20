package org.example.project.pet

object GrowthEngine {

    const val XP_PER_LEVEL = 100
    const val STEPS_PER_XP = 400
    const val CHAT_XP_PER_MESSAGE = 2
    const val MAX_CHAT_XP_PER_DAY = 10
    const val DAILY_OPEN_XP = 1
    const val FEED_HUNGER_RESTORE = 30
    const val MAX_FEEDS_PER_DAY = 3
    const val PETTING_XP = 1
    const val MAX_PETTING_XP_PER_DAY = 5
    const val HUNGER_DECAY_PER_HOUR = 2
    const val ENERGY_DECAY_PER_HOUR = 1

    fun levelForXp(xp: Int): Int = xp / XP_PER_LEVEL + 1

    fun xpIntoCurrentLevel(xp: Int): Int = xp % XP_PER_LEVEL

    /** 8000 步 → +20 XP */
    fun xpForSteps(steps: Long): Int = (steps / STEPS_PER_XP).toInt()

    /**
     * 确定某天的计步基线（传感器返回的是开机累计值）。
     * 优先沿用上次记录的累计读数，找回上次读数之后、本次打开之前走的步数；
     * 无历史记录（-1）或累计值回退（设备重启）时退回当前读数。
     */
    fun initialStepBaseline(currentCumulative: Long, lastKnownCumulative: Long): Long =
        if (lastKnownCumulative in 0..currentCumulative) lastKnownCumulative else currentCumulative

    /** 抚摸/点击：每日前 MAX_PETTING_XP_PER_DAY 次每次 +1 */
    fun xpForPetting(usedToday: Int): Int =
        if (usedToday < MAX_PETTING_XP_PER_DAY) PETTING_XP else 0

    fun xpForSleep(hours: Double): Int = when {
        hours >= 7.0 -> 15
        hours >= 6.0 -> 8
        else -> 0
    }

    /** 睡眠不足 6 小时 → 困倦 */
    fun sleepMood(hours: Double): PetMood? = if (hours < 6.0) PetMood.SLEEPY else null

    /** 睡眠恢复精力：8 小时睡满 100 */
    fun energyForSleep(hours: Double): Int = (hours / 8.0 * 100).toInt().coerceIn(0, 100)

    /** WMO 天气码（Open-Meteo）→ 心情 */
    fun moodForWeatherCode(code: Int): PetMood = when (code) {
        0, 1 -> PetMood.HAPPY
        2, 3 -> PetMood.NORMAL
        45, 48 -> PetMood.LAZY
        in 51..67, in 80..82 -> PetMood.LAZY
        in 71..77, 85, 86 -> PetMood.EXCITED
        in 95..99 -> PetMood.SAD
        else -> PetMood.NORMAL
    }

    fun weatherDescription(code: Int): String = when (code) {
        0 -> "晴"
        1 -> "大部晴朗"
        2 -> "多云"
        3 -> "阴"
        45, 48 -> "雾"
        in 51..55 -> "毛毛雨"
        56, 57 -> "冻毛毛雨"
        61, 63, 65 -> "雨"
        66, 67 -> "冻雨"
        71, 73, 75 -> "雪"
        77 -> "雪粒"
        in 80..82 -> "阵雨"
        85, 86 -> "阵雪"
        95 -> "雷暴"
        96, 99 -> "雷暴冰雹"
        else -> "未知"
    }

    fun weatherEmoji(code: Int): String = when (code) {
        0, 1 -> "☀️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "❄️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }

    /**
     * 心情优先级：睡眠不足 > 状态过低 > 天气 > 默认平静
     */
    fun resolveMood(hunger: Int, energy: Int, weatherCode: Int?, lastSleepHours: Double?): PetMood {
        lastSleepHours?.let { hours -> sleepMood(hours)?.let { return it } }
        if (hunger <= 10 || energy <= 10) return PetMood.SAD
        if (weatherCode != null) return moodForWeatherCode(weatherCode)
        return PetMood.NORMAL
    }

    // ---- 每日任务 ----

    /** 步数任务每天必出（核心玩法），其余任务从池中轮转 2 个 */
    private val questPool = listOf(QuestType.FEED, QuestType.CHAT, QuestType.SLEEP, QuestType.PETTING)

    /** 任务进度取到该值即视为满格（目标值本身封顶展示） */
    private const val QUEST_BAR_MAX = 6

    fun questTarget(type: QuestType): Int = when (type) {
        QuestType.STEPS -> 5000
        QuestType.FEED -> 1
        QuestType.CHAT -> 3
        QuestType.SLEEP -> 1
        QuestType.PETTING -> 5
    }

    fun questXpReward(type: QuestType): Int = when (type) {
        QuestType.STEPS -> 10
        QuestType.FEED -> 6
        QuestType.CHAT -> 8
        QuestType.SLEEP -> 10
        QuestType.PETTING -> 6
    }

    /** yyyy-MM-dd → 年内天序（纯字符串算术，跨平台结果一致，不用 hashCode） */
    private fun dayOfYear(date: String): Int {
        val parts = date.split("-")
        if (parts.size != 3) return 0
        val year = parts[0].toIntOrNull() ?: 0
        val month = parts[1].toIntOrNull() ?: 1
        val day = parts[2].toIntOrNull() ?: 1
        val cum = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val febExtra = if (leap && month > 2) 1 else 0
        return cum[month.coerceIn(1, 12) - 1] + febExtra + day
    }

    /** 生成某天的 3 个每日任务：步数固定 + 池中轮转 2 个 */
    fun selectDailyQuests(date: String): List<DailyQuest> {
        val day = dayOfYear(date)
        val two = listOf(questPool[day % 4], questPool[(day + 2) % 4])
        return (listOf(QuestType.STEPS) + two).map {
            DailyQuest(id = it.name.lowercase(), type = it, target = questTarget(it), xpReward = questXpReward(it))
        }
    }

    /** 任务进度按目标值封顶 */
    fun questProgress(quest: DailyQuest, activity: QuestActivity): Int = when (quest.type) {
        QuestType.STEPS -> activity.steps
        QuestType.FEED -> activity.feedCount
        QuestType.CHAT -> activity.chatMessageCount
        QuestType.SLEEP -> if (activity.sleepChecked) 1 else 0
        QuestType.PETTING -> activity.pettingCount
    }.coerceIn(0, quest.target)

    /** 任务进度占格数：整数换算避免浮点边界（如 1/3×6 恰好等于 2），有进度未完成时至少留 1 格 */
    fun questFillCount(progress: Int, target: Int): Int {
        if (progress <= 0) return 0
        return (progress * QUEST_BAR_MAX / target).coerceIn(1, QUEST_BAR_MAX)
    }

    /** 只累加经验；lastUpdatedEpochMs 是衰减结算时钟，由 applyTimeDecay / 喂食 / 睡眠打卡维护 */
    fun addXp(state: PetState, amount: Int): PetState {
        if (amount <= 0) return state
        return state.copy(xp = state.xp + amount)
    }

    /** 按离线时长衰减：饱食度每小时 -2，精力每小时 -1 */
    fun applyTimeDecay(state: PetState, nowEpochMs: Long): PetState {
        if (state.lastUpdatedEpochMs <= 0L || nowEpochMs <= state.lastUpdatedEpochMs) {
            return state.copy(lastUpdatedEpochMs = nowEpochMs)
        }
        val elapsedHours = (nowEpochMs - state.lastUpdatedEpochMs) / 3_600_000.0
        return state.copy(
            hunger = (state.hunger - (elapsedHours * HUNGER_DECAY_PER_HOUR).toInt()).coerceIn(0, 100),
            energy = (state.energy - (elapsedHours * ENERGY_DECAY_PER_HOUR).toInt()).coerceIn(0, 100),
            lastUpdatedEpochMs = nowEpochMs,
        )
    }
}
