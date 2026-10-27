package org.example.project.pet

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FishGameTest {

    private val engine = FishGameEngine(Random(7))

    @Test
    fun gameStopsAfterThirtySeconds() {
        val after = engine.update(FishGameState(), FishGameConfig.DURATION_MS)
        assertFalse(after.running)
        assertEquals(FishGameConfig.DURATION_MS, after.elapsedMs)
        assertEquals(0L, after.remainingMs)
    }

    @Test
    fun fishInsideBasketIsCaught() {
        val state = FishGameState(
            playerX = 120f,
            items = listOf(FishGameItem(1, 120f, catchY(), isBomb = false)),
        )
        val after = engine.update(state, 100L)
        assertEquals(1, after.score)
        assertTrue(after.items.none { it.id == 1 })
    }

    @Test
    fun fishOutsideBasketFallsThrough() {
        val state = FishGameState(
            playerX = 120f,
            items = listOf(FishGameItem(1, 30f, catchY(), isBomb = false)),
        )
        val after = engine.update(state, 100L)
        assertEquals(0, after.score)
        assertTrue(after.items.any { it.id == 1 })
    }

    @Test
    fun bombCostsOneLifeAndThreeBombsEndGame() {
        var state = FishGameState()
        repeat(FishGameConfig.MAX_LIVES) { index ->
            state = engine.update(
                state.copy(
                    playerX = 120f,
                    items = listOf(FishGameItem(100 + index, 120f, catchY(), isBomb = true)),
                ),
                100L,
            )
        }
        assertEquals(0, state.lives)
        assertFalse(state.running)
    }

    @Test
    fun missedFishLeavesScreenWithoutPenalty() {
        val state = FishGameState(
            playerX = 120f,
            items = listOf(FishGameItem(1, 30f, FishGameConfig.HEIGHT + 10f, isBomb = false)),
        )
        val after = engine.update(state, 100L)
        assertEquals(0, after.score)
        assertEquals(FishGameConfig.MAX_LIVES, after.lives)
        assertTrue(after.items.none { it.id == 1 })
    }

    @Test
    fun difficultyRampsUpOverTime() {
        assertTrue(FishGameConfig.fallSpeed(30_000L) > FishGameConfig.fallSpeed(0L))
        assertTrue(FishGameConfig.spawnIntervalMs(30_000L) < FishGameConfig.spawnIntervalMs(0L))
    }

    /** 物品中心落在接取带（篮子 y 区间 214..230）上方，下一步更新后即可被接住 */
    private fun catchY(): Float = FishGameConfig.PLAYER_Y - FishGameConfig.ITEM_RADIUS - 1f
}
