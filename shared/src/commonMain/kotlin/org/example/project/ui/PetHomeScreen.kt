package org.example.project.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.pet.DailyQuestState
import org.example.project.pet.GrowthEngine
import org.example.project.pet.MAX_PET_NAME_LENGTH
import org.example.project.pet.QuestType
import org.example.project.pet.level
import org.example.project.pet.sanitizePetName
import org.example.project.pet.xpIntoCurrentLevel
import org.example.project.ui.cat.CatPet

@Composable
fun PetHomeScreen(
    state: PetUiState,
    onFeed: () -> Unit,
    onRefreshSteps: () -> Unit,
    onRefreshWeather: () -> Unit,
    onPetTap: () -> Unit,
    onRename: (String) -> Unit,
    onClaimQuests: () -> Unit,
    onGameFinished: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showGame by rememberSaveable { mutableStateOf(false) }
    if (showGame) {
        FishGameScreen(
            gameXpToday = state.gameXpToday,
            onFinish = onGameFinished,
            onExit = { showGame = false },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 宠物舞台
        Card(Modifier.fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CatPet(
                    mood = state.pet.mood,
                    onTap = onPetTap,
                    modifier = Modifier.size(210.dp),
                )
            }
        }

        // 名字 + 等级 + 心情
        var showRenameDialog by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.pet.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "重命名",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(4.dp))
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Text("Lv.${state.pet.level}") }
            Spacer(Modifier.width(8.dp))
            SuggestionChip(onClick = {}, label = { Text(state.pet.mood.label) })
        }
        if (showRenameDialog) {
            RenamePetDialog(
                currentName = state.pet.name,
                onRename = onRename,
                onDismiss = { showRenameDialog = false },
            )
        }

        // 属性
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatRow(Icons.Filled.Star, "经验值", state.pet.xpIntoCurrentLevel, GrowthEngine.XP_PER_LEVEL, MaterialTheme.colorScheme.primary)
                StatRow(Icons.Filled.Restaurant, "饱食度", state.pet.hunger, 100, Color(0xFFE07B39))
                StatRow(Icons.Filled.Bolt, "精力", state.pet.energy, 100, Color(0xFF5F9E52))
            }
        }

        // 天气 / 步数
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ElevatedCard(onClick = onRefreshWeather, modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WbSunny, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("天气 · ${state.city.name}", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(6.dp))
                    val weather = state.weather
                    Text(
                        if (weather != null) "${state.weatherDesc} ${weather.temperatureC}℃" else state.weatherDesc,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "点击刷新",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            ElevatedCard(onClick = onRefreshSteps, modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DirectionsWalk, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("今日步数", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(6.dp))
                    if (state.stepsSupported) {
                        Text("${state.todaySteps} 步", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "已转化 +${state.todayStepXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            "此设备暂不支持计步",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 喂食
        val feedsLeft = (GrowthEngine.MAX_FEEDS_PER_DAY - state.feedsUsedToday).coerceAtLeast(0)
        FilledTonalButton(onClick = onFeed, enabled = feedsLeft > 0) {
            Icon(Icons.Filled.Restaurant, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("喂食（今日剩余 $feedsLeft 次）")
        }

        // 迷你游戏
        OutlinedButton(onClick = { showGame = true }) {
            Icon(Icons.Filled.SportsEsports, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("玩游戏 · 接小鱼干")
        }

        // 每日任务
        DailyQuestsCard(quests = state.dailyQuests, onClaim = onClaimQuests)
    }
}

@Composable
private fun DailyQuestsCard(quests: List<DailyQuestState>, onClaim: () -> Unit) {
    if (quests.isEmpty()) return
    val claimable = quests.filter { it.completed && !it.claimed }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("每日任务", style = MaterialTheme.typography.titleSmall)
            quests.forEach { qs ->
                QuestRow(qs)
            }
            if (claimable.isNotEmpty()) {
                FilledTonalButton(onClick = onClaim) {
                    Text("领取奖励（+${claimable.sumOf { it.quest.xpReward }} XP）")
                }
            }
        }
    }
}

@Composable
private fun QuestRow(qs: DailyQuestState) {
    val done = qs.completed
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            questIcon(qs.quest.type),
            null,
            Modifier.size(16.dp),
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            qs.quest.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        if (qs.claimed) {
            Icon(
                Icons.Filled.CheckCircle,
                "已领取",
                Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            QuestProgressDots(qs)
        }
    }
}

/** 完成 ✓、进行中的 6 格进度条、未开始 ×，一眼分辨任务状态 */
@Composable
private fun QuestProgressDots(qs: DailyQuestState) {
    val filled = GrowthEngine.questFillCount(qs.progress, qs.quest.target)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (qs.progress == 0) {
            Icon(
                Icons.Filled.Close,
                "未开始",
                Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        } else {
            repeat(6) { index ->
                Box(
                    Modifier
                        .size(14.dp, 8.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (index < filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                )
            }
        }
    }
}

private fun questIcon(type: QuestType): ImageVector = when (type) {
    QuestType.STEPS -> Icons.Filled.DirectionsWalk
    QuestType.FEED -> Icons.Filled.Restaurant
    QuestType.CHAT -> Icons.AutoMirrored.Filled.Chat
    QuestType.SLEEP -> Icons.Filled.Bedtime
    QuestType.PETTING -> Icons.Filled.Favorite
}

@Composable
private fun RenamePetDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    val valid = sanitizePetName(name) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= MAX_PET_NAME_LENGTH) name = it },
                label = { Text("宠物名字") },
                singleLine = true,
                isError = !valid,
                supportingText = {
                    Text(
                        if (valid) "最多 ${MAX_PET_NAME_LENGTH} 个字符"
                        else "名字不能为空",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(name); onDismiss() }, enabled = valid) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: Int, max: Int, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(
                "$value/$max",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value / max.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
    }
}
