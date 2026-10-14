package org.example.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Open-Meteo 免费天气 API，无需 Key */
class OpenMeteoWeatherDataSource(private val client: HttpClient) : WeatherDataSource {

    override suspend fun currentWeather(lat: Double, lon: Double): WeatherInfo? = try {
        val response: OpenMeteoResponse = client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", lat)
            parameter("longitude", lon)
            parameter("current", "temperature_2m,weather_code")
        }.body()
        response.current?.let { WeatherInfo(weatherCode = it.weatherCode, temperatureC = it.temperature2m) }
    } catch (_: Exception) {
        null
    }
}

@Serializable
private data class OpenMeteoResponse(
    val current: Current? = null,
) {
    @Serializable
    data class Current(
        @SerialName("temperature_2m") val temperature2m: Double = 0.0,
        @SerialName("weather_code") val weatherCode: Int = 0,
    )
}
