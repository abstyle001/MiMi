package org.example.project.data

/** 步数数据源：返回开机以来的累计步数；设备不支持或权限未授予时返回 null */
interface StepDataSource {
    suspend fun cumulativeSteps(): Long?
}

/** 桌面 / Web / iOS 端的占位实现 */
class NoOpStepDataSource : StepDataSource {
    override suspend fun cumulativeSteps(): Long? = null
}

data class WeatherInfo(
    val weatherCode: Int,
    val temperatureC: Double,
)

interface WeatherDataSource {
    suspend fun currentWeather(lat: Double, lon: Double): WeatherInfo?
}

data class CityPreset(val name: String, val lat: Double, val lon: Double) {
    companion object {
        val PRESETS = listOf(
            CityPreset("北京", 39.9042, 116.4074),
            CityPreset("上海", 31.2304, 121.4737),
            CityPreset("广州", 23.1291, 113.2644),
            CityPreset("深圳", 22.5431, 114.0579),
            CityPreset("杭州", 30.2741, 120.1551),
            CityPreset("成都", 30.5728, 104.0668),
        )
        val DEFAULT = PRESETS.first()
    }
}
