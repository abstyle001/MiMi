package org.example.project.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.project.pet.FishGameConfig
import org.example.project.pet.FishGameEngine
import org.example.project.pet.FishGameItem
import org.example.project.pet.FishGameState
import org.example.project.pet.GrowthEngine

/**
 * 30 秒接小鱼干小游戏。玩法与结算都在本文件内闭环：
 * 引擎只产出状态，Canvas 只负责绘制，结束时通过 [onFinish] 交给 ViewModel 结算 XP。
 */
@Composable
fun FishGameScreen(
    gameXpToday: Int,
    onFinish: (Int) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = remember { FishGameEngine() }
    var round by remember { mutableIntStateOf(0) }
    var game by remember(round) { mutableStateOf(FishGameState()) }
    var playerX by remember(round) { mutableFloatStateOf(FishGameConfig.WIDTH / 2) }
    var earnedXp by remember(round) { mutableIntStateOf(0) }
    var settled by remember(round) { mutableStateOf(false) }

    LaunchedEffect(round) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val deltaMs = ((now - last) / 1_000_000L).coerceIn(0L, 64L)
            last = now
            val next = engine.update(game.copy(playerX = playerX), deltaMs)
            game = next
            if (!next.running) break
        }
    }

    LaunchedEffect(game.running) {
        if (!game.running && !settled) {
            settled = true
            earnedXp = GrowthEngine.xpForFishGame(game.score, gameXpToday)
            onFinish(game.score)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.SportsEsports,
                null,
                Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text("接小鱼干", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, "退出游戏")
            }
        }

        GameHud(game)

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            playerX = playerXFor(change.position.x, size.width)
                            change.consume()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            playerX = playerXFor(offset.x, size.width)
                        }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val s = size.minDimension / FishGameConfig.WIDTH
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF2C6474), Color(0xFF0E2430))))
                    repeat(10) { index ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = (2 + index % 3) * s,
                            center = Offset(((index * 47) % 240) * s, ((index * 71) % 240) * s),
                        )
                    }
                    withTransform({ scale(s, s, pivot = Offset.Zero) }) {
                        game.items.forEach { drawItem(it) }
                        drawBasket(game.playerX)
                    }
                }
                if (!game.running) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xE60B1A22)),
                        contentAlignment = Alignment.Center,
                    ) {
                        ResultPanel(
                            bombedOut = game.lives <= 0,
                            score = game.score,
                            earnedXp = earnedXp,
                            onReplay = { round++ },
                            onExit = onExit,
                        )
                    }
                }
            }
        }

        Text(
            "拖动或用手指移动篮子，接住小鱼干，躲开炸弹。每条鱼 +${GrowthEngine.FISH_GAME_XP_PER_FISH} XP，" +
                "今日上限 ${GrowthEngine.MAX_FISH_GAME_XP_PER_DAY} XP（已获得 $gameXpToday）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GameHud(game: FishGameState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Timer,
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
            val seconds = ((game.remainingMs + 999) / 1000).toInt()
            Text("${seconds}s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            "得分 ${game.score}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(FishGameConfig.MAX_LIVES) { index ->
                Icon(
                    if (index < game.lives) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (index < game.lives) "剩余生命" else "已失去生命",
                    modifier = Modifier.size(18.dp),
                    tint = if (index < game.lives) Color(0xFFE06C75) else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun ResultPanel(
    bombedOut: Boolean,
    score: Int,
    earnedXp: Int,
    onReplay: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.SportsEsports,
            null,
            Modifier.size(28.dp),
            tint = Color(0xFFFFC857),
        )
        Text(
            if (bombedOut) "被炸弹炸到啦" else "时间到！",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text("接住 $score 条小鱼干", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Text(
            when {
                earnedXp > 0 -> "经验 +$earnedXp"
                score > 0 -> "今日游戏经验已满"
                else -> "再接再厉喵"
            },
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFFFC857),
        )
        Button(onClick = onReplay) { Text("再来一局") }
        TextButton(onClick = onExit) { Text("返回主页", color = Color.White) }
    }
}

private fun playerXFor(offsetX: Float, canvasWidth: Int): Float {
    if (canvasWidth <= 0) return FishGameConfig.WIDTH / 2
    val half = FishGameConfig.PLAYER_WIDTH / 2
    return (offsetX / canvasWidth * FishGameConfig.WIDTH)
        .coerceIn(half, FishGameConfig.WIDTH - half)
}

private fun DrawScope.drawItem(item: FishGameItem) {
    if (item.isBomb) {
        drawCircle(Color(0xFF3A3A46), radius = 8f, center = Offset(item.x, item.y))
        drawCircle(
            Color(0xFF6E6E7C),
            radius = 8f,
            center = Offset(item.x, item.y),
            style = Stroke(1f),
        )
        drawLine(
            Color(0xFFD9A441),
            start = Offset(item.x + 4f, item.y - 6f),
            end = Offset(item.x + 8f, item.y - 12f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round,
        )
        drawCircle(Color(0xFFFFC857), radius = 2f, center = Offset(item.x + 8f, item.y - 12f))
        drawCircle(
            Color.White.copy(alpha = 0.35f),
            radius = 2f,
            center = Offset(item.x - 3f, item.y - 3f),
        )
    } else {
        val tail = Path().apply {
            moveTo(item.x - 7f, item.y)
            lineTo(item.x - 13f, item.y - 5f)
            lineTo(item.x - 13f, item.y + 5f)
            close()
        }
        drawPath(tail, Color(0xFFE9964A))
        drawOval(
            Color(0xFFFFB866),
            topLeft = Offset(item.x - 9f, item.y - 5f),
            size = Size(18f, 10f),
        )
        drawOval(
            Color.White.copy(alpha = 0.9f),
            topLeft = Offset(item.x + 2f, item.y - 2f),
            size = Size(3f, 3f),
        )
        drawCircle(
            Color(0xFF3A2A1A),
            radius = 0.8f,
            center = Offset(item.x + 3.5f, item.y - 0.5f),
        )
        drawLine(
            Color(0xFFD97A2B),
            start = Offset(item.x - 1f, item.y - 5f),
            end = Offset(item.x + 3f, item.y),
            strokeWidth = 1f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawBasket(playerX: Float) {
    val left = playerX - FishGameConfig.PLAYER_WIDTH / 2
    val top = FishGameConfig.PLAYER_Y
    drawRoundRect(
        color = Color(0xFFE07B39),
        topLeft = Offset(left, top),
        size = Size(FishGameConfig.PLAYER_WIDTH, FishGameConfig.PLAYER_HEIGHT),
        cornerRadius = CornerRadius(4f),
    )
    drawRoundRect(
        color = Color(0xFF9C5423),
        topLeft = Offset(left - 2f, top),
        size = Size(FishGameConfig.PLAYER_WIDTH + 4f, 4f),
        cornerRadius = CornerRadius(2f),
    )
    repeat(3) { index ->
        val x = left + FishGameConfig.PLAYER_WIDTH * (index + 1) / 4f
        drawLine(
            Color(0xFFB9672E),
            start = Offset(x, top + 5f),
            end = Offset(x, top + FishGameConfig.PLAYER_HEIGHT - 1f),
            strokeWidth = 1f,
        )
    }
}
