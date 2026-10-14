package org.example.project

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.chat.LlmChatService
import org.example.project.data.OpenMeteoWeatherDataSource
import org.example.project.data.PetRepository
import org.example.project.data.WeatherDataSource
import org.example.project.data.createSettings
import org.example.project.data.createStepDataSource
import org.example.project.ui.PetViewModel

/** 简易服务装配：各平台通过 expect/actual 提供 Settings 与 StepDataSource */
object AppServices {
    val settings: Settings by lazy { createSettings() }
    val repository: PetRepository by lazy { PetRepository(settings) }
    val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }
    val weatherDataSource: WeatherDataSource by lazy { OpenMeteoWeatherDataSource(httpClient) }
    val chatService: LlmChatService by lazy { LlmChatService(httpClient) }
    val viewModel: PetViewModel by lazy {
        PetViewModel(repository, createStepDataSource(), weatherDataSource, chatService)
    }
}
