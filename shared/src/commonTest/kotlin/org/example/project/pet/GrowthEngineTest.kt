package org.example.project.pet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GrowthEngineTest {

    @Test
    fun stepsConvertToXp() {
        assertEquals(20, GrowthEngine.xpForSteps(8000))
        assertEquals(2, GrowthEngine.xpForSteps(800))
        assertEquals(0, GrowthEngine.xpForSteps(399))
        assertEquals(0, GrowthEngine.xpForSteps(0))
    }

    @Test
    fun stepBaselineCarriesOverLastReading() {
        // 昨天关 App 时累计 8000，今天打开时 9500 → 基线 8000，找回 1500 步
        assertEquals(8000, GrowthEngine.initialStepBaseline(currentCumulative = 9500, lastKnownCumulative = 8000))
    }

    @Test
    fun stepBaselineFallsBackOnRebootOrNoRecord() {
        // 设备重启后累计值回退 → 退回当前读数
        assertEquals(120, GrowthEngine.initialStepBaseline(currentCumulative = 120, lastKnownCumulative = 8000))
        // 没有任何历史记录 → 退回当前读数
        assertEquals(500, GrowthEngine.initialStepBaseline(currentCumulative = 500, lastKnownCumulative = -1))
    }

    @Test
    fun sleepXpTiers() {
        assertEquals(15, GrowthEngine.xpForSleep(7.0))
        assertEquals(15, GrowthEngine.xpForSleep(8.5))
        assertEquals(8, GrowthEngine.xpForSleep(6.0))
        assertEquals(8, GrowthEngine.xpForSleep(6.9))
        assertEquals(0, GrowthEngine.xpForSleep(5.9))
        assertEquals(0, GrowthEngine.xpForSleep(0.0))
    }

    @Test
    fun shortSleepMakesPetSleepy() {
        assertEquals(PetMood.SLEEPY, GrowthEngine.sleepMood(5.9))
        assertNull(GrowthEngine.sleepMood(6.0))
    }

    @Test
    fun sleepRestoresEnergy() {
        assertEquals(100, GrowthEngine.energyForSleep(8.0))
        assertEquals(100, GrowthEngine.energyForSleep(10.0))
        assertEquals(50, GrowthEngine.energyForSleep(4.0))
        assertEquals(0, GrowthEngine.energyForSleep(0.0))
    }

    @Test
    fun levelBoundaries() {
        assertEquals(1, GrowthEngine.levelForXp(0))
        assertEquals(1, GrowthEngine.levelForXp(99))
        assertEquals(2, GrowthEngine.levelForXp(100))
        assertEquals(3, GrowthEngine.levelForXp(250))
        assertEquals(0, GrowthEngine.xpIntoCurrentLevel(100))
        assertEquals(50, GrowthEngine.xpIntoCurrentLevel(250))
    }

    @Test
    fun weatherMoodMapping() {
        assertEquals(PetMood.HAPPY, GrowthEngine.moodForWeatherCode(0))
        assertEquals(PetMood.NORMAL, GrowthEngine.moodForWeatherCode(2))
        assertEquals(PetMood.LAZY, GrowthEngine.moodForWeatherCode(61))
        assertEquals(PetMood.EXCITED, GrowthEngine.moodForWeatherCode(73))
        assertEquals(PetMood.SAD, GrowthEngine.moodForWeatherCode(96))
        assertEquals(PetMood.NORMAL, GrowthEngine.moodForWeatherCode(999))
    }

    @Test
    fun moodPrioritySleepOverWeather() {
        assertEquals(PetMood.SLEEPY, GrowthEngine.resolveMood(80, 80, weatherCode = 0, lastSleepHours = 5.0))
        assertEquals(PetMood.SAD, GrowthEngine.resolveMood(5, 80, weatherCode = 0, lastSleepHours = 7.0))
        assertEquals(PetMood.HAPPY, GrowthEngine.resolveMood(80, 80, weatherCode = 0, lastSleepHours = 7.0))
        assertEquals(PetMood.NORMAL, GrowthEngine.resolveMood(80, 80, weatherCode = null, lastSleepHours = null))
    }

    @Test
    fun timeDecayDropsStats() {
        val start = PetState(hunger = 50, energy = 50, lastUpdatedEpochMs = 1_000_000L)
        val after = GrowthEngine.applyTimeDecay(start, 1_000_000L + 10L * 3_600_000L)
        assertEquals(30, after.hunger)
        assertEquals(40, after.energy)
    }

    @Test
    fun timeDecayClampsAtZero() {
        val start = PetState(hunger = 3, energy = 2, lastUpdatedEpochMs = 1_000_000L)
        val after = GrowthEngine.applyTimeDecay(start, 1_000_000L + 10L * 3_600_000L)
        assertEquals(0, after.hunger)
        assertEquals(0, after.energy)
    }

    @Test
    fun firstLaunchOnlyStampsTime() {
        val after = GrowthEngine.applyTimeDecay(PetState(hunger = 80), nowEpochMs = 12345L)
        assertEquals(80, after.hunger)
        assertEquals(12345L, after.lastUpdatedEpochMs)
    }

    @Test
    fun pettingXpDailyCap() {
        assertEquals(1, GrowthEngine.xpForPetting(0))
        assertEquals(1, GrowthEngine.xpForPetting(4))
        assertEquals(0, GrowthEngine.xpForPetting(5))
        assertEquals(0, GrowthEngine.xpForPetting(100))
    }

    @Test
    fun addXpCrossesLevel() {
        val pet = GrowthEngine.addXp(PetState(xp = 99), 5)
        assertEquals(104, pet.xp)
        assertEquals(2, GrowthEngine.levelForXp(pet.xp))
    }

    @Test
    fun addXpKeepsDecayClockUntouched() {
        val start = PetState(xp = 0, lastUpdatedEpochMs = 1_000_000L)
        val pet = GrowthEngine.addXp(start, 10)
        assertEquals(1_000_000L, pet.lastUpdatedEpochMs)
    }

    // ---- 每日任务 ----

    @Test
    fun dailyQuestsAreStableForSameDate() {
        val a = GrowthEngine.selectDailyQuests("2026-10-20")
        val b = GrowthEngine.selectDailyQuests("2026-10-20")
        assertEquals(a, b)
    }

    @Test
    fun dailyQuestsAlwaysThreeWithStepsNoDuplicates() {
        for (day in 1..31) {
            val quests = GrowthEngine.selectDailyQuests(String.format("2026-10-%02d", day))
            assertEquals(3, quests.size)
            assertEquals(3, quests.map { it.type }.distinct().size)
            assertEquals(true, quests.any { it.type == QuestType.STEPS })
        }
    }

    @Test
    fun dailyQuestsRotateAcrossDays() {
        // 池轮转：连续 4 天里非步数任务应覆盖到池中不同任务
        val secondary = (20..24).flatMap { GrowthEngine.selectDailyQuests("2026-10-$it").map { q -> q.type } }
            .filter { it != QuestType.STEPS }
            .distinct()
        assertEquals(true, secondary.size >= 2)
    }

    @Test
    fun questProgressCapsAtTarget() {
        val steps = GrowthEngine.selectDailyQuests("2026-10-20").first { it.type == QuestType.STEPS }
        assertEquals(4200, GrowthEngine.questProgress(steps, QuestActivity(steps = 4200)))
        assertEquals(5000, GrowthEngine.questProgress(steps, QuestActivity(steps = 99999)))
        assertEquals(0, GrowthEngine.questProgress(steps, QuestActivity(steps = -5)))

        val sleep = DailyQuest("sleep", QuestType.SLEEP, 1, 10)
        assertEquals(1, GrowthEngine.questProgress(sleep, QuestActivity(sleepChecked = true)))
        assertEquals(0, GrowthEngine.questProgress(sleep, QuestActivity(sleepChecked = false)))
    }

    @Test
    fun questFillCountScaling() {
        // 统一换算到 6 格：3 句聊天任务
        assertEquals(0, GrowthEngine.questFillCount(0, 3))
        assertEquals(2, GrowthEngine.questFillCount(1, 3))
        assertEquals(4, GrowthEngine.questFillCount(2, 3))
        assertEquals(6, GrowthEngine.questFillCount(3, 3))
        // 大目标：5000 步，有进度未完成时至少留 1 格
        assertEquals(0, GrowthEngine.questFillCount(0, 5000))
        assertEquals(1, GrowthEngine.questFillCount(800, 5000))
        assertEquals(3, GrowthEngine.questFillCount(2500, 5000))
        assertEquals(6, GrowthEngine.questFillCount(5000, 5000))
    }
}
