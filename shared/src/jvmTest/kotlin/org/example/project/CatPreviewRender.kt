package org.example.project

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import java.io.File
import kotlin.test.Test
import org.example.project.chat.ChatMessage
import org.example.project.chat.LlmConfig
import org.example.project.pet.PetMood
import org.example.project.ui.AppTheme
import org.example.project.ui.ChatScreen
import org.example.project.ui.PetUiState
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
}
