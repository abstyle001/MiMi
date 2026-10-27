package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.project.chat.LlmConfig
import org.example.project.data.CityPreset
import org.example.project.pet.Achievement
import org.example.project.pet.AchievementState
import org.example.project.pet.GrowthStage
import org.example.project.pet.growthStage
import org.example.project.pet.level
import org.example.project.pet.levelRangeLabel
import org.example.project.pet.levelsToNextStage
import org.example.project.pet.nextGrowthStage
import org.example.project.pet.stageProgress
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
        GrowthStageCard(state)
        AchievementCard(state.achievements)
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
            Text("游戏互动：+${state.gameXpToday} XP")
            Text("喂食：${state.feedsUsedToday} 次 · 抚摸：${state.pettingsUsedToday} 次")
            Text("金币：${state.pet.coins}（走路 / 喂食 / 聊天 / 任务获得）")
        }
    }
}

@Composable
private fun GrowthStageCard(state: PetUiState) {
    val level = state.pet.level
    val current = state.pet.growthStage
    val next = state.pet.nextGrowthStage
    val remaining = levelsToNextStage(level)
    val progress = stageProgress(level)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(Icons.Filled.Pets, "成长阶段")
                Spacer(Modifier.weight(1f))
                Text(
                    "Lv.$level",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GrowthStage.entries.forEach { stage ->
                    GrowthStagePill(
                        stage = stage,
                        active = stage == current,
                        reached = level >= stage.minLevel,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                current.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (next != null && remaining != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "再升 $remaining 级进化为${next.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
private fun GrowthStagePill(
    stage: GrowthStage,
    active: Boolean,
    reached: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = when {
        active -> MaterialTheme.colorScheme.primaryContainer
        reached -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = when {
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        reached -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stage.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = content,
        )
        Text(
            stage.levelRangeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

@Composable
private fun AchievementCard(achievements: List<AchievementState>) {
    if (achievements.isEmpty()) return
    var selected by remember { mutableStateOf<AchievementState?>(null) }
    val unlockedCount = achievements.count { it.unlocked }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(Icons.Filled.EmojiEvents, "成就墙")
                Spacer(Modifier.weight(1f))
                Text(
                    "已解锁 $unlockedCount/${achievements.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            achievements.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item ->
                        AchievementBadge(
                            state = item,
                            onClick = { selected = item },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    selected?.let { item ->
        AchievementDetailDialog(state = item, onDismiss = { selected = null })
    }
}

@Composable
private fun AchievementBadge(
    state: AchievementState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unlocked = state.unlocked
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (unlocked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                achievementIcon(state.achievement),
                contentDescription = state.achievement.title,
                modifier = Modifier.size(22.dp),
                tint = if (unlocked) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            state.achievement.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (unlocked) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
        Text(
            if (unlocked) "已解锁" else "${state.progress}/${state.achievement.target}",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AchievementDetailDialog(state: AchievementState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.achievement.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.achievement.description)
                Text(
                    if (state.unlocked) "已解锁"
                    else "进度：${state.progress}/${state.achievement.target}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.unlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
}

private fun achievementIcon(achievement: Achievement): ImageVector = when (achievement) {
    Achievement.FIRST_LEVEL_UP -> Icons.Filled.Star
    Achievement.STEPS_10K, Achievement.STEPS_100K -> Icons.AutoMirrored.Filled.DirectionsWalk
    Achievement.CHAT_10, Achievement.CHAT_100 -> Icons.AutoMirrored.Filled.Chat
    Achievement.FEED_10, Achievement.FEED_50 -> Icons.Filled.Restaurant
    Achievement.PET_100 -> Icons.Filled.Favorite
    Achievement.SLEEP_10 -> Icons.Filled.Bedtime
    Achievement.STREAK_7 -> Icons.Filled.LocalFireDepartment
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
