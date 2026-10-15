package org.example.project.data

import com.russhwolf.settings.MapSettings
import org.example.project.chat.ChatMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class PetRepositoryTest {

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
}
