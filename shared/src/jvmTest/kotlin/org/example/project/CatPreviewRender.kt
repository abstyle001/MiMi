package org.example.project

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import java.io.File
import kotlin.test.Test
import org.example.project.chat.ChatMessage
import org.example.project.chat.LlmConfig
import org.example.project.pet.AccessoryCatalog
import org.example.project.pet.Achievement
import org.example.project.pet.AchievementState
import org.example.project.pet.DailyQuestState
import org.example.project.pet.GrowthEngine
import org.example.project.pet.PetMood
import org.example.project.pet.PetState
import org.example.project.pet.QuestActivity
import org.example.project.pet.ShopCatalog
import org.example.project.ui.AppTheme
import org.example.project.ui.ChatScreen
import org.example.project.ui.FishGameScreen
import org.example.project.ui.PetHomeScreen
import org.example.project.ui.PetUiState
import org.example.project.ui.ShopScreen
import org.example.project.ui.StatsScreen
import org.example.project.ui.WardrobeScreen
import org.example.project.ui.cat.CatPet

/** 开发期视觉快照：把六种心情各渲染一张 PNG 到 shared/build/catpreviews/，人工检查造型 */
@OptIn(ExperimentalComposeUiApi::class)
class CatPreviewRender {

    @Test
    fun renderAllMoods() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        PetMood.entries.forEach { mood ->
            val scene = ImageComposeScene(width = 480, height = 480, density = Density(2f))
            try {
                scene.setContent { CatPet(mood = mood, onTap = {}) }
                val pngBytes = scene.render().encodeToData()?.bytes
                    ?: error("渲染 $mood 失败")
                File(dir, "${mood.name}.png").writeBytes(pngBytes)
            } finally {
                scene.close()
            }
        }
    }

    @Test
    fun renderChatSourceTags() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        val scene = ImageComposeScene(width = 560, height = 700, density = Density(2f))
        try {
            scene.setContent {
                AppTheme {
                    ChatScreen(
                        state = PetUiState(
                            chatMessages = listOf(
                                ChatMessage(ChatMessage.Role.USER, "hello"),
                                ChatMessage(ChatMessage.Role.PET, "喵呜……网络好像不太顺畅，我好饿喵……", fromLlm = false),
                                ChatMessage(ChatMessage.Role.PET, "主人好呀喵～今天超适合晒太阳！", fromLlm = true),
                            ),
                            llmConfig = LlmConfig(apiKey = ""),
                        ),
                        onSend = {},
                    )
                }
            }
            val pngBytes = scene.render().encodeToData()?.bytes ?: error("渲染聊天页失败")
            File(dir, "chat_source_tags.png").writeBytes(pngBytes)
        } finally {
            scene.close()
        }
    }

    @Test
    fun renderAchievementWall() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        val achievements = Achievement.entries.mapIndexed { index, achievement ->
            val unlocked = index % 3 != 1
            AchievementState(
                achievement = achievement,
                progress = if (unlocked) achievement.target else achievement.target / 3,
                unlocked = unlocked,
            )
        }
        val scene = ImageComposeScene(width = 560, height = 2400, density = Density(2f))
        try {
            scene.setContent {
                AppTheme {
                    StatsScreen(
                        state = PetUiState(
                            pet = PetState(name = "小咪", xp = 250, hunger = 72, energy = 61),
                            todaySteps = 6420,
                            todayStepXp = 16,
                            feedsUsedToday = 1,
                            pettingsUsedToday = 3,
                            chatXpToday = 6,
                            sleepCheckedToday = false,
                            lastSleepHours = 7.0,
                            achievements = achievements,
                        ),
                        onCheckInSleep = {},
                        onSaveSettings = { _, _ -> },
                    )
                }
            }
            val pngBytes = scene.render().encodeToData()?.bytes ?: error("渲染成就墙失败")
            File(dir, "achievement_wall.png").writeBytes(pngBytes)
        } finally {
            scene.close()
        }
    }

    @Test
    fun renderFishGame() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        val scene = ImageComposeScene(width = 560, height = 1100, density = Density(2f))
        try {
            scene.setContent {
                AppTheme {
                    FishGameScreen(gameXpToday = 3, onFinish = {}, onExit = {})
                }
            }
            // 推进约 3 秒游戏时间，让画面里出现掉落的鱼和炸弹
            var frame = 0L
            repeat(90) {
                frame += 32_000_000L
                scene.render(frame)
            }
            val pngBytes = scene.render(frame).encodeToData()?.bytes ?: error("渲染游戏页失败")
            File(dir, "fish_game.png").writeBytes(pngBytes)

            // 继续推进到 30 秒结束，检查结算面板
            repeat(950) {
                frame += 32_000_000L
                scene.render(frame)
            }
            val resultBytes = scene.render(frame).encodeToData()?.bytes ?: error("渲染游戏结算失败")
            File(dir, "fish_game_result.png").writeBytes(resultBytes)
        } finally {
            scene.close()
        }
    }

    @Test
    fun renderPetWithBow() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        val scene = ImageComposeScene(width = 480, height = 480, density = Density(2f))
        try {
            scene.setContent {
                CatPet(
                    mood = PetMood.HAPPY,
                    onTap = {},
                    accessories = setOf(ShopCatalog.RED_BOW.id),
                )
            }
            val pngBytes = scene.render().encodeToData()?.bytes ?: error("渲染配饰失败")
            File(dir, "pet_with_bow.png").writeBytes(pngBytes)
        } finally {
            scene.close()
        }
    }

    /** 每个配饰单独渲染一张，检查帽子/项圈/背景与精灵图的对位 */
    @Test
    fun renderAccessoryShowcase() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        AccessoryCatalog.entries.forEach { accessory ->
            val scene = ImageComposeScene(width = 480, height = 480, density = Density(2f))
            try {
                scene.setContent {
                    CatPet(
                        mood = PetMood.HAPPY,
                        onTap = {},
                        accessories = setOf(accessory.id),
                    )
                }
                val pngBytes = scene.render().encodeToData()?.bytes
                    ?: error("渲染配饰 ${accessory.id} 失败")
                File(dir, "accessory_${accessory.id}.png").writeBytes(pngBytes)
            } finally {
                scene.close()
            }
        }
    }

    /** 三槽位同时装备的整体效果 */
    @Test
    fun renderFullOutfit() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        val scene = ImageComposeScene(width = 480, height = 480, density = Density(2f))
        try {
            scene.setContent {
                CatPet(
                    mood = PetMood.EXCITED,
                    onTap = {},
                    accessories = setOf(
                        AccessoryCatalog.STRAW_HAT.id,
                        AccessoryCatalog.STAR_SCARF.id,
                        AccessoryCatalog.BG_SAKURA.id,
                    ),
                )
            }
            val pngBytes = scene.render().encodeToData()?.bytes ?: error("渲染整体装扮失败")
            File(dir, "full_outfit.png").writeBytes(pngBytes)
        } finally {
            scene.close()
        }
    }

    @Test
    fun renderWardrobe() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        val achievements = Achievement.entries.mapIndexed { index, achievement ->
            val unlocked = index % 3 != 1
            AchievementState(
                achievement = achievement,
                progress = if (unlocked) achievement.target else achievement.target / 3,
                unlocked = unlocked,
            )
        }
        val scene = ImageComposeScene(width = 560, height = 2600, density = Density(2f))
        try {
            scene.setContent {
                AppTheme {
                    WardrobeScreen(
                        state = PetUiState(
                            pet = PetState(
                                xp = 450,
                                ownedItemIds = setOf(
                                    AccessoryCatalog.RED_BOW.id,
                                    AccessoryCatalog.PARTY_HAT.id,
                                    AccessoryCatalog.BG_SAKURA.id,
                                ),
                                equippedHead = AccessoryCatalog.PARTY_HAT.id,
                                equippedNeck = AccessoryCatalog.RED_BOW.id,
                                equippedBackground = AccessoryCatalog.BG_SAKURA.id,
                                accessoriesInitialized = true,
                            ),
                            achievements = achievements,
                        ),
                        onEquip = {},
                        onClearSlot = {},
                        onOpenShop = {},
                        onExit = {},
                    )
                }
            }
            val pngBytes = scene.render().encodeToData()?.bytes ?: error("渲染装扮页失败")
            File(dir, "wardrobe.png").writeBytes(pngBytes)
        } finally {
            scene.close()
        }
    }

    @Test
    fun renderShop() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        val scene = ImageComposeScene(width = 560, height = 2700, density = Density(2f))
        try {
            scene.setContent {
                AppTheme {
                    ShopScreen(
                        state = PetUiState(
                            pet = PetState(coins = 50, ownedItemIds = setOf(ShopCatalog.YARN_BALL.id)),
                            feedsUsedToday = 1,
                        ),
                        onBuy = {},
                        onExit = {},
                    )
                }
            }
            val pngBytes = scene.render().encodeToData()?.bytes ?: error("渲染商店失败")
            File(dir, "shop.png").writeBytes(pngBytes)
        } finally {
            scene.close()
        }
    }

    @Test
    fun renderHomeWithCoins() {
        val dir = File("build/catpreviews").apply { mkdirs() }
        val quests = GrowthEngine.selectDailyQuests("2026-10-27").map { quest ->
            DailyQuestState(quest, GrowthEngine.questProgress(quest, QuestActivity(steps = 3200)), false)
        }
        val scene = ImageComposeScene(width = 560, height = 1900, density = Density(2f))
        try {
            scene.setContent {
                AppTheme {
                    PetHomeScreen(
                        state = PetUiState(
                            pet = PetState(
                                name = "小咪",
                                xp = 250,
                                coins = 50,
                                hunger = 72,
                                energy = 61,
                                ownedItemIds = setOf(ShopCatalog.RED_BOW.id, ShopCatalog.YARN_BALL.id),
                                equippedNeck = ShopCatalog.RED_BOW.id,
                                accessoriesInitialized = true,
                            ),
                            todaySteps = 6420,
                            todayStepXp = 16,
                            feedsUsedToday = 1,
                            pettingsUsedToday = 3,
                            dailyQuests = quests,
                        ),
                        onFeed = {},
                        onRefreshSteps = {},
                        onRefreshWeather = {},
                        onPetTap = {},
                        onRename = {},
                        onClaimQuests = {},
                        onGameFinished = {},
                        onBuyItem = {},
                        onPlayWithToy = {},
                        onEquipAccessory = {},
                        onClearAccessory = {},
                    )
                }
            }
            val pngBytes = scene.render().encodeToData()?.bytes ?: error("渲染主页失败")
            File(dir, "home_with_coins.png").writeBytes(pngBytes)
        } finally {
            scene.close()
        }
    }
}
