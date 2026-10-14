package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.project.pet.GrowthEngine
import org.example.project.pet.level
import org.example.project.pet.xpIntoCurrentLevel
import org.example.project.ui.cat.CatPet

@Composable
fun PetHomeScreen(
    state: PetUiState,
    onFeed: () -> Unit,
    onRefreshSteps: () -> Unit,
    onRefreshWeather: () -> Unit,
    onPetTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CatPet(
            mood = state.pet.mood,
            onTap = onPetTap,
            modifier = Modifier.size(220.dp),
        )

        Text(
            text = "${state.pet.name}  Lv.${state.pet.level} · ${state.pet.mood.label}",
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(Modifier.height(16.dp))

        StatBar(
            label = "经验值",
            value = state.pet.xpIntoCurrentLevel,
            max = GrowthEngine.XP_PER_LEVEL,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        StatBar(
            label = "饱食度",
            value = state.pet.hunger,
            max = 100,
            color = Color(0xFFFF9800),
        )
        Spacer(Modifier.height(8.dp))
        StatBar(
            label = "精力",
            value = state.pet.energy,
            max = 100,
            color = Color(0xFF4CAF50),
        )

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeatherCard(state, onRefreshWeather, Modifier.weight(1f))
            StepsCard(state, onRefreshSteps, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        val feedsLeft = (GrowthEngine.MAX_FEEDS_PER_DAY - state.feedsUsedToday).coerceAtLeast(0)
        Button(onClick = onFeed, enabled = feedsLeft > 0) {
            Text("喂食 🐟（今日剩余 $feedsLeft 次）")
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int, max: Int, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text("$value/$max", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { (value / max.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
    }
}

@Composable
private fun WeatherCard(state: PetUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onRefresh, modifier = modifier) {
        Column(Modifier.padding(12.dp).fillMaxWidth()) {
            Text("天气 · ${state.city.name}", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            val weather = state.weather
            if (weather != null) {
                Text(
                    "${GrowthEngine.weatherEmoji(weather.weatherCode)} ${state.weatherDesc} ${weather.temperatureC}℃",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(state.weatherDesc, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "点我刷新",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StepsCard(state: PetUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onRefresh, modifier = modifier) {
        Column(Modifier.padding(12.dp).fillMaxWidth()) {
            Text("今日步数", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            if (state.stepsSupported) {
                Text("🚶 ${state.todaySteps} 步", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "已转化 +${state.todayStepXp} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text("此设备暂不支持计步", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
