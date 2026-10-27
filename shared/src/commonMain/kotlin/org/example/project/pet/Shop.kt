package org.example.project.pet

enum class ShopCategory(val label: String) {
    FOOD("高级猫粮"),
    TOY("玩具"),
    ACCESSORY("配饰"),
}

/**
 * 商店商品。id 同时是已购集合的持久化键，改名不要改 id。
 *
 * 食物购买后立即喂食，不进入库存；玩具 / 配饰是一次性永久解锁。
 */
data class ShopItem(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val category: ShopCategory,
    val hungerRestore: Int = 0,
)

object ShopCatalog {
    /** 主页免费喂食用的普通粮，不上架商店 */
    val NORMAL_FOOD = ShopItem(
        id = "normal_food",
        title = "普通粮",
        description = "饱食度 +30",
        price = 0,
        category = ShopCategory.FOOD,
        hungerRestore = 30,
    )

    val CANNED_FOOD = ShopItem(
        id = "canned_food",
        title = "罐头",
        description = "饱食度 +50，更管饱",
        price = 15,
        category = ShopCategory.FOOD,
        hungerRestore = 50,
    )

    val DRIED_FISH = ShopItem(
        id = "dried_fish",
        title = "小鱼干",
        description = "饱食度 +70，小咪最爱",
        price = 30,
        category = ShopCategory.FOOD,
        hungerRestore = 70,
    )

    val YARN_BALL = ShopItem(
        id = "yarn_ball",
        title = "毛线球",
        description = "永久解锁陪玩，每天可玩 1 次 +${GrowthEngine.TOY_PLAY_XP} XP",
        price = 40,
        category = ShopCategory.TOY,
    )

    val RED_BOW = ShopItem(
        id = "red_bow",
        title = "红领结",
        description = "项圈 · 经典红色蝴蝶结",
        price = 60,
        category = ShopCategory.ACCESSORY,
    )

    val BELL_COLLAR = ShopItem(
        id = "bell_collar",
        title = "铃铛项圈",
        description = "项圈 · 走路叮当响",
        price = 50,
        category = ShopCategory.ACCESSORY,
    )

    val PARTY_HAT = ShopItem(
        id = "party_hat",
        title = "派对帽",
        description = "帽子 · 节日气氛拉满",
        price = 45,
        category = ShopCategory.ACCESSORY,
    )

    val SAKURA_BG = ShopItem(
        id = "bg_sakura",
        title = "樱花背景",
        description = "背景 · 飘着花瓣的春天",
        price = 80,
        category = ShopCategory.ACCESSORY,
    )

    /** 商店货架：普通粮留在主页免费喂食 */
    val items = listOf(
        CANNED_FOOD, DRIED_FISH, YARN_BALL,
        RED_BOW, BELL_COLLAR, PARTY_HAT, SAKURA_BG,
    )
}
