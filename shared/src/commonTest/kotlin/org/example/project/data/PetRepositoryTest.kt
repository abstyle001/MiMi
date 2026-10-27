package org.example.project.data

import com.russhwolf.settings.MapSettings
import org.example.project.chat.ChatMessage
import org.example.project.pet.PetState
import kotlin.test.Test
import kotlin.test.assertEquals

class PetRepositoryTest {

    @Test
    fun petNamePersistsThroughRepository() {
        val repo = PetRepository(MapSettings())
        repo.savePet(PetState(name = "咪咪"))
        assertEquals("咪咪", repo.loadPet().name)
        // 模拟重命名后再次读取（跨启动）
        repo.savePet(repo.loadPet().copy(name = "新名字"))
        assertEquals("新名字", repo.loadPet().name)
    }

    @Test
    fun chatHistoryRoundTrip() {
        val repo = PetRepository(MapSettings())
        val messages = listOf(
            ChatMessage(ChatMessage.Role.USER, "你好", 1L),
            ChatMessage(ChatMessage.Role.PET, "喵～", 2L, fromLlm = true),
        )
        repo.saveChatMessages(messages)
        assertEquals(messages, repo.loadChatMessages())
    }

    @Test
    fun chatHistoryCappedAt50() {
        val repo = PetRepository(MapSettings())
        val messages = (1..60).map { ChatMessage(ChatMessage.Role.USER, "msg$it", it.toLong()) }
        repo.saveChatMessages(messages)
        val loaded = repo.loadChatMessages()
        assertEquals(50, loaded.size)
        assertEquals("msg11", loaded.first().content)
        assertEquals("msg60", loaded.last().content)
    }

    @Test
    fun corruptChatHistoryFallsBackToEmpty() {
        val settings = MapSettings()
        settings.putString("chat_messages", "{not json")
        assertEquals(emptyList(), PetRepository(settings).loadChatMessages())
    }

    @Test
    fun chatMessageCountUsesDateKey() {
        val repo = PetRepository(MapSettings())
        repo.setChatMsg("2026-10-20", 2)
        assertEquals(2, repo.chatMsgCount())
        // 换天后读取方按 date 判断，旧值自然失效
        assertEquals("2026-10-20", repo.chatMsgDate())
    }

    @Test
    fun claimedQuestIdsRoundTripAndResetNextDay() {
        val repo = PetRepository(MapSettings())
        repo.addClaimedQuestIds("2026-10-20", setOf("feed"))
        repo.addClaimedQuestIds("2026-10-20", setOf("chat"))
        assertEquals(setOf("feed", "chat"), repo.claimedQuestIds("2026-10-20"))
        assertEquals(emptySet(), repo.claimedQuestIds("2026-10-21"))
    }

    @Test
    fun lifetimeCountersSurviveRestart() {
        val settings = MapSettings()
        val repo = PetRepository(settings)
        repo.addLifetimeSteps(1200L)
        repo.addLifetimeSteps(800L)
        repo.incrementLifetimeFeeds()
        repo.incrementLifetimeChats()
        repo.incrementLifetimeChats()
        repo.incrementLifetimePettings()
        repo.incrementLifetimeSleepCheckIns()
        repo.setSleepStreak("2026-10-27", 3)
        repo.setStepsCounted("2026-10-27", 2000L)

        val reopened = PetRepository(settings)
        assertEquals(2000L, reopened.lifetimeSteps())
        assertEquals(1, reopened.lifetimeFeeds())
        assertEquals(2, reopened.lifetimeChats())
        assertEquals(1, reopened.lifetimePettings())
        assertEquals(1, reopened.lifetimeSleepCheckIns())
        assertEquals(3, reopened.sleepStreak())
        assertEquals("2026-10-27", reopened.sleepStreakDate())
        assertEquals("2026-10-27", reopened.stepsCountedDate())
        assertEquals(2000L, reopened.stepsCountedToday())
    }

    @Test
    fun unlockedAchievementsMergeWithoutDuplicates() {
        val settings = MapSettings()
        val repo = PetRepository(settings)
        repo.addUnlockedAchievements(setOf("FIRST_LEVEL_UP"))
        repo.addUnlockedAchievements(setOf("FIRST_LEVEL_UP", "STEPS_10K"))
        assertEquals(setOf("FIRST_LEVEL_UP", "STEPS_10K"), PetRepository(settings).unlockedAchievements())
    }
}
