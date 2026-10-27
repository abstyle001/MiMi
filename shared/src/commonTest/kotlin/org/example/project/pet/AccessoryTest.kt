package org.example.project.pet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccessoryTest {

    @Test
    fun catalogIdsAreUniqueAndEverySlotHasAccessories() {
        assertEquals(AccessoryCatalog.entries.size, AccessoryCatalog.entries.map { it.id }.distinct().size)
        assertEquals(AccessoryCatalog.entries.size, AccessoryCatalog.byId.size)
        AccessorySlot.entries.forEach { slot ->
            assertTrue(AccessoryCatalog.forSlot(slot).isNotEmpty(), "槽位 ${slot.label} 没有配饰")
        }
    }

    @Test
    fun shopAccessoriesMatchShopItems() {
        val shopAccessories = ShopCatalog.items.filter { it.category == ShopCategory.ACCESSORY }
        shopAccessories.forEach { item ->
            val accessory = AccessoryCatalog.byId[item.id]
            assertNotNull(accessory, "商店配饰 ${item.id} 缺少 Accessory 定义")
            assertEquals(ShopCategory.ACCESSORY, item.category)
            val unlock = accessory.unlock
            assertTrue(unlock is AccessoryUnlock.Shop, "商店配饰 ${item.id} 的解锁方式应为商店")
            assertEquals(unlock.price, item.price, "${item.id} 商店售价与配饰定价不一致")
        }
        // 反向：配饰目录里标记为商店购买的，都必须真的上架
        AccessoryCatalog.entries.forEach { accessory ->
            val unlock = accessory.unlock as? AccessoryUnlock.Shop ?: return@forEach
            val item = shopAccessories.firstOrNull { it.id == accessory.id }
            assertNotNull(item, "配饰 ${accessory.id} 标记为商店解锁但未上架")
            assertEquals(unlock.price, item.price)
        }
    }

    @Test
    fun shopAccessoryUnlocksOnlyAfterPurchase() {
        val bow = AccessoryCatalog.RED_BOW
        assertFalse(isAccessoryUnlocked(bow, PetState(), unlockedAchievements = emptySet()))
        assertTrue(
            isAccessoryUnlocked(
                bow,
                PetState(ownedItemIds = setOf(bow.id)),
                unlockedAchievements = emptySet(),
            )
        )
    }

    @Test
    fun achievementAccessoryUnlocksWithAchievement() {
        val cap = AccessoryCatalog.SLEEP_CAP
        val achievement = (cap.unlock as AccessoryUnlock.AchievementReward).achievement
        assertFalse(isAccessoryUnlocked(cap, PetState(), unlockedAchievements = emptySet()))
        assertTrue(isAccessoryUnlocked(cap, PetState(), unlockedAchievements = setOf(achievement.name)))
        // 只有别的成就不能解锁
        assertFalse(isAccessoryUnlocked(cap, PetState(), unlockedAchievements = setOf(Achievement.CHAT_10.name)))
    }

    @Test
    fun levelGiftUnlocksAtRequiredLevel() {
        val strawHat = AccessoryCatalog.STRAW_HAT
        val level = (strawHat.unlock as AccessoryUnlock.LevelGift).level
        val xpNeeded = (level - 1) * GrowthEngine.XP_PER_LEVEL
        assertFalse(
            isAccessoryUnlocked(strawHat, PetState(xp = xpNeeded - 1), unlockedAchievements = emptySet())
        )
        assertTrue(
            isAccessoryUnlocked(strawHat, PetState(xp = xpNeeded), unlockedAchievements = emptySet())
        )
        assertTrue(levelGiftAccessories(level).any { it.id == strawHat.id })
        assertFalse(levelGiftAccessories(level - 1).any { it.id == strawHat.id })
    }

    @Test
    fun unlockedIdsCombineAllUnlockSources() {
        val state = PetState(
            xp = 200, // Lv.3
            ownedItemIds = setOf(AccessoryCatalog.BG_SAKURA.id),
        )
        val ids = unlockedAccessoryIds(state, unlockedAchievements = setOf(Achievement.STREAK_7.name))
        assertTrue(AccessoryCatalog.STRAW_HAT.id in ids, "等级礼包未解锁")
        assertTrue(AccessoryCatalog.BG_SAKURA.id in ids, "已购背景未解锁")
        assertTrue(AccessoryCatalog.BG_STARRY.id in ids, "成就奖励未解锁")
        assertFalse(AccessoryCatalog.PARTY_HAT.id in ids, "未购买的商店配饰不应解锁")
        assertFalse(AccessoryCatalog.BG_SUNRISE.id in ids, "Lv.5 礼包在 Lv.3 时不应解锁")
    }

    @Test
    fun achievementRewardAccessoriesMapToAchievements() {
        val rewards = achievementRewardAccessories(setOf(Achievement.SLEEP_10))
        assertEquals(listOf(AccessoryCatalog.SLEEP_CAP), rewards)
        assertEquals(emptyList(), achievementRewardAccessories(setOf(Achievement.FEED_10)))
    }

    @Test
    fun equipOnlyChangesTargetSlot() {
        val pet = PetState()
            .withEquipped(AccessorySlot.HEAD, AccessoryCatalog.PARTY_HAT.id)
            .withEquipped(AccessorySlot.NECK, AccessoryCatalog.RED_BOW.id)
        assertEquals(AccessoryCatalog.PARTY_HAT.id, pet.equippedHead)
        assertEquals(AccessoryCatalog.RED_BOW.id, pet.equippedNeck)
        assertEquals("", pet.equippedBackground)

        val clearedHead = pet.withEquipped(AccessorySlot.HEAD, "")
        assertEquals("", clearedHead.equippedHead)
        assertEquals(AccessoryCatalog.RED_BOW.id, clearedHead.equippedNeck)
    }

    @Test
    fun equippedIdsSkipBlankSlots() {
        val pet = PetState(
            equippedHead = AccessoryCatalog.STRAW_HAT.id,
            equippedNeck = "",
            equippedBackground = AccessoryCatalog.BG_STARRY.id,
        )
        assertEquals(
            setOf(AccessoryCatalog.STRAW_HAT.id, AccessoryCatalog.BG_STARRY.id),
            pet.equippedAccessoryIds,
        )
    }

    @Test
    fun migrationEquipsLegacyOwnedAccessoriesOnce() {
        val legacy = PetState(
            ownedItemIds = setOf(AccessoryCatalog.RED_BOW.id, AccessoryCatalog.BG_SAKURA.id),
        )
        val migrated = migrateEquippedAccessories(legacy)
        assertEquals(AccessoryCatalog.RED_BOW.id, migrated.equippedNeck)
        assertEquals(AccessoryCatalog.BG_SAKURA.id, migrated.equippedBackground)
        assertTrue(migrated.accessoriesInitialized)
        // 迁移后用户取下的配饰不会被再次自动装上
        val unequipped = migrated.withEquipped(AccessorySlot.NECK, "")
        assertEquals("", migrateEquippedAccessories(unequipped).equippedNeck)
    }

    @Test
    fun migrationKeepsExistingManualChoice() {
        val state = PetState(
            ownedItemIds = setOf(AccessoryCatalog.RED_BOW.id, AccessoryCatalog.BELL_COLLAR.id),
            equippedNeck = AccessoryCatalog.BELL_COLLAR.id,
            accessoriesInitialized = false,
        )
        assertEquals(AccessoryCatalog.BELL_COLLAR.id, migrateEquippedAccessories(state).equippedNeck)
    }
}
