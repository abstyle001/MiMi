package org.example.project

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import java.io.File
import kotlin.test.Test
import org.example.project.pet.PetMood
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
}
