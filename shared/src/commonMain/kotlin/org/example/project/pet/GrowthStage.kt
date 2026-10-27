package org.example.project.pet

/**
 * 宠物成长阶段：由等级派生，不单独持久化，xp 是唯一事实来源。
 * 达到 [minLevel] 自动进化；新增阶段时同步调整 CatPet 的外观参数，并补 GrowthStageTest / 渲染快照。
 */
enum class GrowthStage(
    val id: String,
    val label: String,
    val minLevel: Int,
    val description: String,
) {
    KITTEN("kitten", "幼猫", 1, "奶声奶气的小团子，最黏人"),
    YOUNG("young", "青年猫", 5, "好奇心旺盛，开始有自己的小脾气"),
    ADULT("adult", "成年猫", 10, "沉稳可靠，但依然最爱蹭你"),
}

val GrowthStage.next: GrowthStage?
    get() = GrowthStage.entries.getOrNull(ordinal + 1)

/** 等级 → 阶段；等级非法（0 / 负数）按幼猫处理 */
fun growthStageForLevel(level: Int): GrowthStage {
    val safeLevel = level.coerceAtLeast(1)
    return GrowthStage.entries.last { safeLevel >= it.minLevel }
}

fun growthStageForXp(xp: Int): GrowthStage = growthStageForLevel(GrowthEngine.levelForXp(xp))

val PetState.growthStage: GrowthStage
    get() = growthStageForLevel(level)

val PetState.nextGrowthStage: GrowthStage?
    get() = growthStage.next

/** 距离下一阶段还差几级；成年后返回 null */
fun levelsToNextStage(level: Int): Int? {
    val next = growthStageForLevel(level).next ?: return null
    return (next.minLevel - level).coerceAtLeast(0)
}

/** 当前阶段内的等级进度（0f = 刚进化，1f = 即将进化）；成年后恒为 1f */
fun stageProgress(level: Int): Float {
    val current = growthStageForLevel(level)
    val next = current.next ?: return 1f
    val span = (next.minLevel - current.minLevel).coerceAtLeast(1)
    return ((level - current.minLevel).toFloat() / span).coerceIn(0f, 1f)
}

/** 阶段对应的等级区间文案，如 "Lv.1-4" / "Lv.10+" */
val GrowthStage.levelRangeLabel: String
    get() = next?.let { "Lv.$minLevel-${it.minLevel - 1}" } ?: "Lv.$minLevel+"
