package org.example.project.pet

import kotlinx.serialization.Serializable

enum class PetMood(val emoji: String, val label: String) {
    HAPPY("😺", "开心"),
    EXCITED("😸", "兴奋"),
    NORMAL("🐱", "平静"),
    LAZY("😾", "慵懒"),
    SLEEPY("😴", "困倦"),
    SAD("😿", "低落"),
}

@Serializable
data class PetState(
    val name: String = "小咪",
    val xp: Int = 0,
    /** 饱食度 0-100，越高越饱 */
    val hunger: Int = 80,
    /** 精力 0-100 */
    val energy: Int = 80,
    val mood: PetMood = PetMood.NORMAL,
    /** 上次结算衰减的时间戳；0 表示首次创建 */
    val lastUpdatedEpochMs: Long = 0L,
)

val PetState.level: Int get() = GrowthEngine.levelForXp(xp)

val PetState.xpIntoCurrentLevel: Int get() = GrowthEngine.xpIntoCurrentLevel(xp)
