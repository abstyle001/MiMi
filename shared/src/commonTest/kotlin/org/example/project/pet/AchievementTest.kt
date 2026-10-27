package org.example.project.pet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementTest {

    @Test
    fun nothingUnlockedOnFreshStats() {
        assertTrue(checkAchievements(AchievementStats()).isEmpty())
    }

    @Test
    fun firstLevelUnlocksAtLevelTwo() {
        assertFalse(Achievement.FIRST_LEVEL_UP in checkAchievements(AchievementStats(level = 1)))
        assertTrue(Achievement.FIRST_LEVEL_UP in checkAchievements(AchievementStats(level = 2)))
    }

    @Test
    fun stepsMilestonesUnlockAtThresholds() {
        val almost = checkAchievements(AchievementStats(totalSteps = 99_999L))
        assertTrue(Achievement.STEPS_10K in almost)
        assertFalse(Achievement.STEPS_100K in almost)
        assertTrue(Achievement.STEPS_100K in checkAchievements(AchievementStats(totalSteps = 100_000L)))
    }

    @Test
    fun chatFeedAndPettingMilestones() {
        val unlocked = checkAchievements(
            AchievementStats(chatMessages = 100, feeds = 50, pettings = 100)
        )
        assertTrue(Achievement.CHAT_10 in unlocked)
        assertTrue(Achievement.CHAT_100 in unlocked)
        assertTrue(Achievement.FEED_10 in unlocked)
        assertTrue(Achievement.FEED_50 in unlocked)
        assertTrue(Achievement.PET_100 in unlocked)
    }

    @Test
    fun sleepAndStreakMilestones() {
        val unlocked = checkAchievements(AchievementStats(sleepCheckIns = 10, sleepStreak = 7))
        assertTrue(Achievement.SLEEP_10 in unlocked)
        assertTrue(Achievement.STREAK_7 in unlocked)
    }

    @Test
    fun progressClampsBetweenZeroAndTarget() {
        assertEquals(100_000, achievementProgress(Achievement.STEPS_100K, AchievementStats(totalSteps = 250_000L)))
        assertEquals(3, achievementProgress(Achievement.FEED_10, AchievementStats(feeds = 3)))
        assertEquals(0, achievementProgress(Achievement.FEED_10, AchievementStats(feeds = -5)))
    }
}
