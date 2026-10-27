package org.example.project.pet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GrowthStageTest {

    @Test
    fun levelMapsToStageAtBoundaries() {
        assertEquals(GrowthStage.KITTEN, growthStageForLevel(1))
        assertEquals(GrowthStage.KITTEN, growthStageForLevel(4))
        assertEquals(GrowthStage.YOUNG, growthStageForLevel(5))
        assertEquals(GrowthStage.YOUNG, growthStageForLevel(9))
        assertEquals(GrowthStage.ADULT, growthStageForLevel(10))
        assertEquals(GrowthStage.ADULT, growthStageForLevel(99))
        // 非法等级按最低阶段兜底
        assertEquals(GrowthStage.KITTEN, growthStageForLevel(0))
        assertEquals(GrowthStage.KITTEN, growthStageForLevel(-3))
    }

    @Test
    fun xpMapsToStageThroughLevelCurve() {
        assertEquals(GrowthStage.KITTEN, growthStageForXp(0))
        assertEquals(GrowthStage.KITTEN, growthStageForXp(GrowthEngine.XP_PER_LEVEL * 4 - 1))
        assertEquals(GrowthStage.YOUNG, growthStageForXp(GrowthEngine.XP_PER_LEVEL * 4))
        assertEquals(GrowthStage.ADULT, growthStageForXp(GrowthEngine.XP_PER_LEVEL * 9))
    }

    @Test
    fun petStateExposesDerivedStage() {
        assertEquals(GrowthStage.KITTEN, PetState(xp = 399).growthStage)
        assertEquals(GrowthStage.YOUNG, PetState(xp = 400).growthStage)
        assertEquals(GrowthStage.ADULT, PetState(xp = 1000).growthStage)
    }

    @Test
    fun nextStageFollowsCatalogOrder() {
        assertEquals(GrowthStage.YOUNG, GrowthStage.KITTEN.next)
        assertEquals(GrowthStage.ADULT, GrowthStage.YOUNG.next)
        assertNull(GrowthStage.ADULT.next)
    }

    @Test
    fun levelsToNextStageCountsRemainingLevels() {
        assertEquals(4, levelsToNextStage(1))
        assertEquals(1, levelsToNextStage(4))
        assertEquals(5, levelsToNextStage(5))
        assertEquals(1, levelsToNextStage(9))
        assertNull(levelsToNextStage(10))
        assertNull(levelsToNextStage(30))
    }

    @Test
    fun stageProgressGoesFromZeroToOne() {
        assertEquals(0f, stageProgress(1))
        assertEquals(0.75f, stageProgress(4))
        assertEquals(0f, stageProgress(5))
        assertEquals(0.8f, stageProgress(9))
        assertEquals(1f, stageProgress(10))
        assertEquals(1f, stageProgress(50))
    }

    @Test
    fun levelRangeLabelsMatchThresholds() {
        assertEquals("Lv.1-4", GrowthStage.KITTEN.levelRangeLabel)
        assertEquals("Lv.5-9", GrowthStage.YOUNG.levelRangeLabel)
        assertEquals("Lv.10+", GrowthStage.ADULT.levelRangeLabel)
    }
}
