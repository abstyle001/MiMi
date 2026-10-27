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
    /** 金币余额，走路 / 喂食 / 聊天 / 每日任务产出，商店消费 */
    val coins: Int = 0,
    /** 已购买的玩具与配饰 id；食物是即时消耗品，不入库存 */
    val ownedItemIds: Set<String> = emptySet(),
    /** 已装备配饰 id，空字符串表示该槽位空置 */
    val equippedHead: String = "",
    val equippedNeck: String = "",
    val equippedBackground: String = "",
    /** 旧存档迁移标记：首次加载时把已购配饰补装到对应槽位 */
    val accessoriesInitialized: Boolean = false,
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

/** 宠物名最大长度（字符数） */
const val MAX_PET_NAME_LENGTH = 12

/**
 * 校验并清洗用户输入的宠物名：去首尾空白、剔除内部换行/制表等控制符。
 * 清洗后为空或超过 [MAX_PET_NAME_LENGTH] 个字符时返回 null，表示非法输入。
 */
fun sanitizePetName(raw: String): String? {
    val name = raw.trim().filterNot { it == '\n' || it == '\r' || it == '\t' }
    if (name.isEmpty()) return null
    if (name.length > MAX_PET_NAME_LENGTH) return null
    return name
}
