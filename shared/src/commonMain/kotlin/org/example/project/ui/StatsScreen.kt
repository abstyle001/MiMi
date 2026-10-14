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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.example.project.chat.LlmConfig
import org.example.project.data.CityPreset
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    state: PetUiState,
    onCheckInSleep: (Double) -> Unit,
    onSaveSettings: (LlmConfig, CityPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SleepCard(state, onCheckInSleep)
        TodayStatsCard(state)
        LlmSettingsCard(state, onSaveSettings)
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SleepCard(state: PetUiState, onCheckInSleep: (Double) -> Unit) {
    var hours by remember { mutableStateOf(7f) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.Bedtime, "睡眠打卡")
            Spacer(Modifier.height(12.dp))
            if (state.sleepCheckedToday) {
                Text(
                    "今日已打卡：昨晚睡了 ${state.lastSleepHours ?: "-"} 小时",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text("昨晚睡了 ${hours.roundToHalf()} 小时", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = hours,
                    onValueChange = { hours = it },
                    valueRange = 0f..12f,
                )
                Button(
                    onClick = { onCheckInSleep(hours.roundToHalf()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("打卡（≥7h +15 XP，≥6h +8 XP）")
                }
            }
        }
    }
}

@Composable
private fun TodayStatsCard(state: PetUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.BarChart, "今日数据")
            Spacer(Modifier.height(12.dp))
            Text("步数：${state.todaySteps} 步（+${state.todayStepXp} XP）")
            Text("聊天互动：+${state.chatXpToday} XP")
            Text("喂食：${state.feedsUsedToday} 次 · 抚摸：${state.pettingsUsedToday} 次")
        }
    }
}

@Composable
private fun LlmSettingsCard(
    state: PetUiState,
    onSaveSettings: (LlmConfig, CityPreset) -> Unit,
) {
    var baseUrl by remember { mutableStateOf(state.llmConfig.baseUrl) }
    var apiKey by remember { mutableStateOf(state.llmConfig.apiKey) }
    var model by remember { mutableStateOf(state.llmConfig.model) }
    var selectedCity by remember { mutableStateOf(state.city) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(Icons.Filled.Settings, "聊天与城市设置")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL（OpenAI 兼容）") },
                leadingIcon = { Icon(Icons.Filled.Link, null, Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                leadingIcon = { Icon(Icons.Filled.Key, null, Modifier.size(20.dp)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("模型") },
                leadingIcon = { Icon(Icons.Filled.SmartToy, null, Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("城市", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            CityPreset.PRESETS.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { city ->
                        FilterChip(
                            selected = selectedCity.name == city.name,
                            onClick = { selectedCity = city },
                            label = { Text(city.name) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSaveSettings(LlmConfig(baseUrl.trim(), apiKey.trim(), model.trim()), selectedCity) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存设置")
            }
            Text(
                "默认填 Kimi（platform.moonshot.cn 申请 Key）。留空 Key 则使用离线聊天模式。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun Float.roundToHalf(): Double = (this * 2).roundToInt() / 2.0
