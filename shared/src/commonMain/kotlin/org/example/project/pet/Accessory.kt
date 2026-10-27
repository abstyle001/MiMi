package org.example.project.pet

/** 配饰槽位：每个槽位最多装备一件；BACKGROUND 绘制在猫咪身后 */
enum class AccessorySlot(val label: String) {
    HEAD("帽子"),
    NECK("项圈"),
    BACKGROUND("背景"),
}

/** 配饰解锁途径：商店购买 / 成就奖励 / 等级礼包 */
sealed interface AccessoryUnlock {
    data class Shop(val price: Int) : AccessoryUnlock
    data class AchievementReward(val achievement: Achievement) : AccessoryUnlock
    data class LevelGift(val level: Int) : AccessoryUnlock
}

/**
 * 装扮配饰。id 同时是持久化键与绘制分支的 key，改名会导致老用户装备记录失效。
 * 商店里上架的商品 id 必须与 [ShopCatalog] 同名商品一致。
 */
data class Accessory(
    val id: String,
    val title: String,
    val description: String,
    val slot: AccessorySlot,
    val unlock: AccessoryUnlock,
)

/** 解锁条件的人话描述，装扮页和商店共用 */
val Accessory.unlockHint: String
    get() = when (val u = unlock) {
        is AccessoryUnlock.Shop -> "${u.price} 金币 · 商店购买"
        is AccessoryUnlock.AchievementReward -> "成就「${u.achievement.title}」奖励"
        is AccessoryUnlock.LevelGift -> "等级礼包 · 达到 Lv.${u.level}"
    }

object AccessoryCatalog {
    val STRAW_HAT = Accessory(
        id = "straw_hat",
        title = "草帽",
        description = "麦秆编的遮阳小帽",
        slot = AccessorySlot.HEAD,
        unlock = AccessoryUnlock.LevelGift(level = 3),
    )

    val PARTY_HAT = Accessory(
        id = "party_hat",
        title = "派对帽",
        description = "尖尖的彩帽，节日气氛拉满",
        slot = AccessorySlot.HEAD,
        unlock = AccessoryUnlock.Shop(price = 45),
    )

    val SLEEP_CAP = Accessory(
        id = "sleep_cap",
        title = "睡帽",
        description = "软乎乎的睡帽，做个好梦",
        slot = AccessorySlot.HEAD,
        unlock = AccessoryUnlock.AchievementReward(Achievement.SLEEP_10),
    )

    val RED_BOW = Accessory(
        id = "red_bow",
        title = "红领结",
        description = "经典红色蝴蝶结",
        slot = AccessorySlot.NECK,
        unlock = AccessoryUnlock.Shop(price = 60),
    )

    val BELL_COLLAR = Accessory(
        id = "bell_collar",
        title = "铃铛项圈",
        description = "走起路来叮当响",
        slot = AccessorySlot.NECK,
        unlock = AccessoryUnlock.Shop(price = 50),
    )

    val STAR_SCARF = Accessory(
        id = "star_scarf",
        title = "星星围巾",
        description = "围住脖子，星星会发光",
        slot = AccessorySlot.NECK,
        unlock = AccessoryUnlock.AchievementReward(Achievement.CHAT_100),
    )

    val BG_SUNRISE = Accessory(
        id = "bg_sunrise",
        title = "晨光背景",
        description = "清晨的暖色光晕",
        slot = AccessorySlot.BACKGROUND,
        unlock = AccessoryUnlock.LevelGift(level = 5),
    )

    val BG_SAKURA = Accessory(
        id = "bg_sakura",
        title = "樱花背景",
        description = "飘着花瓣的春天",
        slot = AccessorySlot.BACKGROUND,
        unlock = AccessoryUnlock.Shop(price = 80),
    )

    val BG_STARRY = Accessory(
        id = "bg_starry",
        title = "星空背景",
        description = "安静的夏夜",
        slot = AccessorySlot.BACKGROUND,
        unlock = AccessoryUnlock.AchievementReward(Achievement.STREAK_7),
    )

    val entries = listOf(
        STRAW_HAT, PARTY_HAT, SLEEP_CAP,
        RED_BOW, BELL_COLLAR, STAR_SCARF,
        BG_SUNRISE, BG_SAKURA, BG_STARRY,
    )

    val byId: Map<String, Accessory> = entries.associateBy { it.id }

    fun forSlot(slot: AccessorySlot): List<Accessory> = entries.filter { it.slot == slot }
}

// ---- PetState 装备读写 ----

fun PetState.equippedId(slot: AccessorySlot): String = when (slot) {
    AccessorySlot.HEAD -> equippedHead
    AccessorySlot.NECK -> equippedNeck
    AccessorySlot.BACKGROUND -> equippedBackground
}

/** 只改目标槽位，其他槽位保持不变；传空字符串即取下 */
fun PetState.withEquipped(slot: AccessorySlot, accessoryId: String): PetState = when (slot) {
    AccessorySlot.HEAD -> copy(equippedHead = accessoryId)
    AccessorySlot.NECK -> copy(equippedNeck = accessoryId)
    AccessorySlot.BACKGROUND -> copy(equippedBackground = accessoryId)
}

/** 当前装备的配饰 id 集合，直接传给 CatPet(accessories = ...) */
val PetState.equippedAccessoryIds: Set<String>
    get() = listOf(equippedHead, equippedNeck, equippedBackground)
        .filter { it.isNotBlank() }
        .toSet()

/** 舞台铺底用的背景配饰；id 失效（目录改名/删除）时返回 null，避免画出一片空白 */
val PetState.stageBackgroundId: String?
    get() = equippedBackground.takeIf { AccessoryCatalog.byId[it]?.slot == AccessorySlot.BACKGROUND }

// ---- 解锁判定 ----

fun isAccessoryUnlocked(
    accessory: Accessory,
    state: PetState,
    unlockedAchievements: Set<String>,
): Boolean = when (val unlock = accessory.unlock) {
    is AccessoryUnlock.Shop -> accessory.id in state.ownedItemIds
    is AccessoryUnlock.AchievementReward -> unlock.achievement.name in unlockedAchievements
    is AccessoryUnlock.LevelGift -> state.level >= unlock.level
}

fun unlockedAccessoryIds(state: PetState, unlockedAchievements: Set<String>): Set<String> =
    AccessoryCatalog.entries
        .filter { isAccessoryUnlocked(it, state, unlockedAchievements) }
        .map { it.id }
        .toSet()

/** 等级礼包：达到 level 时已解锁的全部配饰（用于升级提示） */
fun levelGiftAccessories(level: Int): List<Accessory> =
    AccessoryCatalog.entries.filter { acc ->
        val unlock = acc.unlock
        unlock is AccessoryUnlock.LevelGift && unlock.level <= level
    }

/** 指定成就对应的配饰奖励（用于成就解锁提示） */
fun achievementRewardAccessories(achievements: Set<Achievement>): List<Accessory> =
    AccessoryCatalog.entries.filter { acc ->
        val unlock = acc.unlock
        unlock is AccessoryUnlock.AchievementReward && unlock.achievement in achievements
    }

/**
 * 老存档兼容：旧版本只存 ownedItemIds、没有装备槽位，当时所有已购配饰都会画在猫身上。
 * 首次加载时把已购配饰补装到对应槽位（同槽多件按目录顺序取第一件），之后用户可自由更换。
 */
fun migrateEquippedAccessories(state: PetState): PetState {
    if (state.accessoriesInitialized) return state
    var migrated = state.copy(accessoriesInitialized = true)
    AccessoryCatalog.entries
        .filter { it.unlock is AccessoryUnlock.Shop && it.id in state.ownedItemIds }
        .forEach { accessory ->
            if (migrated.equippedId(accessory.slot).isEmpty()) {
                migrated = migrated.withEquipped(accessory.slot, accessory.id)
            }
        }
    return migrated
}
