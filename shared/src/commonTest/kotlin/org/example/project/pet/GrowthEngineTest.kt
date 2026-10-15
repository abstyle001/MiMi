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
}
