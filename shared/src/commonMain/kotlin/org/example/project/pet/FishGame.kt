package org.example.project.pet

import kotlin.random.Random

/** 接小鱼干：沿用 CatPet 的 240×240 虚拟坐标，Canvas 按边长等比缩放 */
object FishGameConfig {
    const val WIDTH = 240f
    const val HEIGHT = 240f
    const val DURATION_MS = 30_000L
    const val MAX_LIVES = 3

    const val PLAYER_WIDTH = 54f
    const val PLAYER_HEIGHT = 16f
    const val PLAYER_Y = 214f

    const val ITEM_RADIUS = 7f
    const val ITEM_MARGIN = 14f
    const val BOMB_CHANCE = 0.28f

    private const val START_SPAWN_MS = 720L
    private const val MIN_SPAWN_MS = 360L
    private const val START_FALL_SPEED = 58f
    private const val MAX_FALL_SPEED = 120f

    /** 随时间加快掉落：30 秒内从 720ms 缩到约 360ms */
    fun spawnIntervalMs(elapsedMs: Long): Long =
        (START_SPAWN_MS - elapsedMs / 80).coerceAtLeast(MIN_SPAWN_MS)

    /** 随时间加快下落：30 秒内从 58 线性升到 120（虚拟像素/秒） */
    fun fallSpeed(elapsedMs: Long): Float {
        val progress = elapsedMs.toFloat() / DURATION_MS
        return (START_FALL_SPEED + (MAX_FALL_SPEED - START_FALL_SPEED) * progress)
            .coerceAtMost(MAX_FALL_SPEED)
    }
}

data class FishGameItem(
    val id: Int,
    val x: Float,
    val y: Float,
    val isBomb: Boolean,
)

data class FishGameState(
    val elapsedMs: Long = 0L,
    val playerX: Float = FishGameConfig.WIDTH / 2,
    val items: List<FishGameItem> = emptyList(),
    val score: Int = 0,
    val lives: Int = FishGameConfig.MAX_LIVES,
    val spawnAccumulatorMs: Long = 0L,
    val running: Boolean = true,
) {
    val remainingMs: Long get() = (FishGameConfig.DURATION_MS - elapsedMs).coerceAtLeast(0L)
}

/**
 * 游戏状态机：每帧调用 [update] 推进时间、掉落和接取判定。
 * 随机源可注入，便于单测复现；渲染与输入完全在 Compose 层。
 */
class FishGameEngine(private val random: Random = Random.Default) {

    private var nextId = 0

    fun update(state: FishGameState, deltaMs: Long): FishGameState {
        if (!state.running || deltaMs <= 0L) return state

        val remaining = FishGameConfig.DURATION_MS - state.elapsedMs
        val stepMs = deltaMs.coerceAtMost(remaining).coerceAtLeast(0L)
        val elapsed = state.elapsedMs + stepMs
        val speed = FishGameConfig.fallSpeed(elapsed)
        val playerLeft = state.playerX - FishGameConfig.PLAYER_WIDTH / 2
        val playerRight = state.playerX + FishGameConfig.PLAYER_WIDTH / 2

        var score = state.score
        var lives = state.lives
        val kept = ArrayList<FishGameItem>(state.items.size)
        state.items.forEach { item ->
            val nextY = item.y + speed * stepMs / 1000f
            val overlapsX = item.x + FishGameConfig.ITEM_RADIUS >= playerLeft &&
                item.x - FishGameConfig.ITEM_RADIUS <= playerRight
            val reachesPlayer = nextY + FishGameConfig.ITEM_RADIUS >= FishGameConfig.PLAYER_Y &&
                nextY - FishGameConfig.ITEM_RADIUS <= FishGameConfig.PLAYER_Y + FishGameConfig.PLAYER_HEIGHT
            when {
                overlapsX && reachesPlayer -> if (item.isBomb) lives-- else score++
                nextY - FishGameConfig.ITEM_RADIUS > FishGameConfig.HEIGHT -> Unit
                else -> kept += item.copy(y = nextY)
            }
        }

        var spawnAccumulator = state.spawnAccumulatorMs + stepMs
        val interval = FishGameConfig.spawnIntervalMs(elapsed)
        while (spawnAccumulator >= interval) {
            spawnAccumulator -= interval
            kept += spawnItem()
        }

        return state.copy(
            elapsedMs = elapsed,
            items = kept,
            score = score,
            lives = lives.coerceAtLeast(0),
            spawnAccumulatorMs = spawnAccumulator,
            running = elapsed < FishGameConfig.DURATION_MS && lives > 0,
        )
    }

    private fun spawnItem(): FishGameItem = FishGameItem(
        id = nextId++,
        x = FishGameConfig.ITEM_MARGIN +
            random.nextFloat() * (FishGameConfig.WIDTH - FishGameConfig.ITEM_MARGIN * 2),
        y = -FishGameConfig.ITEM_RADIUS * 2,
        isBomb = random.nextFloat() < FishGameConfig.BOMB_CHANCE,
    )
}
