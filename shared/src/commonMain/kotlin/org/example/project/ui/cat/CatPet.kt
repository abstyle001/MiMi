package org.example.project.ui.cat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import org.example.project.pet.PetMood

// 光源固定在左上方：亮部 → 本色 → 暗部
private val BodyLight = Color(0xFFFFD9A0)
private val BodyColor = Color(0xFFFFB26B)
private val BodyDark = Color(0xFFE8935A)
private val BellyLight = Color(0xFFFFF3DE)
private val BellyColor = Color(0xFFFFE0B0)
private val EarLight = Color(0xFFFFCF95)
private val InnerEarColor = Color(0xFFFF8FA3)
private val DarkColor = Color(0xFF4A3225)
private val BlushCore = Color(0x99FF9E9E)
private val NoseColor = Color(0xFFE26D7A)

private class FloatParticle(val id: Long, val emoji: String, val xBias: Float)

/**
 * 程序化绘制的伪 3D 卡通猫：渐变体积光 + 地面软阴影 + 果冻感点击动画，
 * 表情与动画节奏随 [mood] 变化。虚拟坐标系 240×240。
 */
@Composable
fun CatPet(
    mood: PetMood,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ---- 心情决定动画节奏 ----
    val breathPeriod = when (mood) {
        PetMood.EXCITED -> 1400
        PetMood.SLEEPY -> 3800
        PetMood.LAZY -> 3000
        else -> 2400
    }
    val breathAmp = when (mood) {
        PetMood.EXCITED -> 0.035f
        PetMood.SLEEPY -> 0.015f
        else -> 0.02f
    }
    val floatAmp = if (mood == PetMood.EXCITED) 6f else 3f
    val tailPeriod = when (mood) {
        PetMood.EXCITED -> 500
        PetMood.HAPPY -> 900
        PetMood.NORMAL -> 1400
        PetMood.LAZY -> 2600
        else -> 4200
    }
    val tailAmp = when (mood) {
        PetMood.EXCITED -> 20f
        PetMood.HAPPY -> 15f
        PetMood.NORMAL -> 10f
        PetMood.LAZY -> 6f
        else -> 3f
    }
    val squashAmp = when (mood) {
        PetMood.EXCITED -> 1.2f
        PetMood.SLEEPY, PetMood.LAZY -> 0.6f
        else -> 1f
    }

    val transition = rememberInfiniteTransition(label = "cat")
    val breath by transition.animateFloat(
        initialValue = 1f - breathAmp,
        targetValue = 1f + breathAmp,
        animationSpec = infiniteRepeatable(tween(breathPeriod, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val floatY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -floatAmp,
        animationSpec = infiniteRepeatable(tween(breathPeriod / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    val tailAngle by transition.animateFloat(
        initialValue = -tailAmp,
        targetValue = tailAmp,
        animationSpec = infiniteRepeatable(tween(tailPeriod, easing = LinearEasing), RepeatMode.Reverse),
        label = "tail",
    )

    // ---- 随机眨眼；困倦时大部分时间闭着眼 ----
    var blinking by remember { mutableStateOf(false) }
    LaunchedEffect(mood) {
        while (true) {
            delay(
                when (mood) {
                    PetMood.SLEEPY -> Random.nextLong(700, 1600)
                    else -> Random.nextLong(2500, 5000)
                }
            )
            blinking = true
            delay(if (mood == PetMood.SLEEPY) 900L else 130L)
            blinking = false
        }
    }

    // ---- 点击：下蹲蓄力 → 拉伸起跳 → 落地压扁 → 果冻回弹 ----
    val hop = remember { Animatable(0f) }
    val squash = remember { Animatable(1f) }
    var tapSeq by remember { mutableIntStateOf(0) }
    var tapHappy by remember { mutableStateOf(false) }
    LaunchedEffect(tapSeq) {
        if (tapSeq == 0) return@LaunchedEffect
        tapHappy = true
        hop.snapTo(0f)
        squash.snapTo(1f)
        squash.animateTo(1f - 0.1f * squashAmp, tween(90))
        launch {
            hop.animateTo(-16f, tween(150, easing = LinearOutSlowInEasing))
            hop.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        }
        squash.animateTo(1f + 0.08f * squashAmp, tween(150))
        squash.animateTo(1f, tween(200))
        squash.animateTo(1f - 0.1f * squashAmp, tween(70))
        squash.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        delay(250)
        tapHappy = false
    }

    // ---- 上浮粒子（爱心 / Zzz）----
    val particles = remember { mutableStateListOf<FloatParticle>() }
    var nextParticleId by remember { mutableLongStateOf(0L) }
    fun spawnParticles(emoji: String, count: Int) {
        repeat(count) {
            particles += FloatParticle(nextParticleId++, emoji, Random.nextFloat() * 1.4f - 0.7f)
        }
    }

    LaunchedEffect(mood) {
        if (mood != PetMood.SLEEPY) return@LaunchedEffect
        while (true) {
            delay(1600)
            spawnParticles("💤", 1)
        }
    }

    Box(modifier = modifier.aspectRatio(1f)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        tapSeq++
                        spawnParticles("🧡", 3)
                        onTap()
                    }
                },
        ) {
            val s = size.minDimension / 240f

            // 地面软阴影：不跟随猫咪位移，跳起时缩小变淡，反衬高度
            val hopHeight = -hop.value
            val shadowScale = (1f - hopHeight / 50f + floatY / 120f).coerceIn(0.55f, 1f)
            val shadowAlpha = (0.25f - hopHeight / 90f).coerceIn(0.08f, 0.25f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
                    center = Offset(120f * s, 224f * s),
                    radius = 66f * s * shadowScale,
                ),
                topLeft = Offset((120f - 66f * shadowScale) * s, (224f - 10f) * s),
                size = Size(132f * shadowScale * s, 20f * s),
            )

            translate(top = (floatY + hop.value) * density) {
                scale(
                    scaleX = 1f + (1f - squash.value) * 0.6f,
                    scaleY = breath * squash.value,
                    pivot = Offset(120f * s, 215f * s),
                ) {
                    drawCat(s, mood, blinking, tapHappy, tailAngle)
                }
            }
        }

        particles.forEach { particle ->
            key(particle.id) {
                ParticleView(
                    particle = particle,
                    onDone = { particles.removeAll { it.id == particle.id } },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ParticleView(particle: FloatParticle, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(particle.id) {
        progress.animateTo(1f, tween(durationMillis = 900, easing = LinearEasing))
        onDone()
    }
    Text(
        text = particle.emoji,
        fontSize = 20.sp,
        modifier = Modifier
            .align(BiasAlignment(particle.xBias, -0.3f))
            .graphicsLayer {
                translationY = -64.dp.toPx() * progress.value
                alpha = 1f - progress.value
            },
    )
}

private fun DrawScope.drawCat(
    s: Float,
    mood: PetMood,
    blinking: Boolean,
    tapHappy: Boolean,
    tailAngle: Float,
) {
    // ---- 尾巴（绕根部摇摆，带渐变）----
    rotate(degrees = tailAngle, pivot = Offset(195f * s, 195f * s)) {
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(BodyLight, BodyColor, BodyDark),
                start = Offset(215f * s, 120f * s),
                end = Offset(215f * s, 205f * s),
            ),
            startAngle = 90f,
            sweepAngle = -200f,
            useCenter = false,
            topLeft = Offset(180f * s, 120f * s),
            size = Size(70f * s, 80f * s),
            style = Stroke(width = 13f * s, cap = StrokeCap.Round),
        )
    }

    // ---- 耳朵（圆角三角、自然外撇；低落时整只耷拉下来）----
    if (mood == PetMood.SAD) {
        // 耷拉耳：尖端朝外侧下方，超出头部轮廓
        drawPath(earPath(s, 58f, 92f, 20f, 128f, 100f, 78f), BodyDark)
        drawPath(earPath(s, 64f, 90f, 36f, 116f, 94f, 80f), InnerEarColor)
        drawPath(earPath(s, 182f, 92f, 220f, 128f, 140f, 78f), BodyDark)
        drawPath(earPath(s, 176f, 90f, 204f, 116f, 146f, 80f), InnerEarColor)
    } else {
        rotate(degrees = -12f, pivot = Offset(84f * s, 88f * s)) {
            drawPath(
                earPath(s, 62f, 96f, 50f, 40f, 106f, 74f),
                Brush.linearGradient(listOf(EarLight, BodyColor), Offset(50f * s, 40f * s), Offset(85f * s, 98f * s)),
            )
            drawPath(earPath(s, 69f, 87f, 61f, 52f, 95f, 71f), InnerEarColor)
        }
        rotate(degrees = 12f, pivot = Offset(156f * s, 88f * s)) {
            drawPath(
                earPath(s, 178f, 96f, 190f, 40f, 134f, 74f),
                Brush.linearGradient(listOf(EarLight, BodyColor), Offset(190f * s, 40f * s), Offset(155f * s, 98f * s)),
            )
            drawPath(earPath(s, 171f, 87f, 179f, 52f, 145f, 71f), InnerEarColor)
        }
    }

    // ---- 身体 / 肚皮 / 脚：径向渐变出球体感 ----
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(BodyLight, BodyColor, BodyDark),
            center = Offset(90f * s, 112f * s),
            radius = 115f * s,
        ),
        topLeft = Offset(45f * s, 85f * s),
        size = Size(150f * s, 130f * s),
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(BellyLight, BellyColor),
            center = Offset(112f * s, 158f * s),
            radius = 62f * s,
        ),
        topLeft = Offset(78f * s, 135f * s),
        size = Size(84f * s, 72f * s),
    )
    drawOval(BellyColor, topLeft = Offset(62f * s, 203f * s), size = Size(34f * s, 14f * s))
    drawOval(BellyColor, topLeft = Offset(144f * s, 203f * s), size = Size(34f * s, 14f * s))

    // ---- 头顶呆毛 ----
    drawLine(DarkColor, Offset(116f * s, 86f * s), Offset(112f * s, 75f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
    drawLine(DarkColor, Offset(124f * s, 86f * s), Offset(128f * s, 75f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)

    // ---- 眼睛 ----
    val eyeY = 126f
    val eyeLX = 95f
    val eyeRX = 145f
    val eyeStroke = Stroke(width = 3.5f * s, cap = StrokeCap.Round)
    when {
        tapHappy -> {
            // ∩ ∩ 开心眼
            drawArc(DarkColor, 180f, 180f, false, Offset((eyeLX - 9f) * s, (eyeY - 7f) * s), Size(18f * s, 16f * s), style = eyeStroke)
            drawArc(DarkColor, 180f, 180f, false, Offset((eyeRX - 9f) * s, (eyeY - 7f) * s), Size(18f * s, 16f * s), style = eyeStroke)
        }
        blinking -> {
            // ∪ ∪ 闭眼
            drawArc(DarkColor, 0f, 180f, false, Offset((eyeLX - 9f) * s, (eyeY - 6f) * s), Size(18f * s, 14f * s), style = eyeStroke)
            drawArc(DarkColor, 0f, 180f, false, Offset((eyeRX - 9f) * s, (eyeY - 6f) * s), Size(18f * s, 14f * s), style = eyeStroke)
        }
        mood == PetMood.LAZY -> {
            // 半阖眼：下半圆盘
            drawArc(DarkColor, 0f, 180f, true, Offset((eyeLX - 8f) * s, (eyeY - 8f) * s), Size(16f * s, 16f * s))
            drawArc(DarkColor, 0f, 180f, true, Offset((eyeRX - 8f) * s, (eyeY - 8f) * s), Size(16f * s, 16f * s))
        }
        else -> {
            // 大圆眼 + 常驻双高光（3D 贴纸感）
            val r = if (mood == PetMood.EXCITED) 11f else 9f
            drawCircle(DarkColor, r * s, Offset(eyeLX * s, eyeY * s))
            drawCircle(DarkColor, r * s, Offset(eyeRX * s, eyeY * s))
            drawCircle(Color.White, r * 0.38f * s, Offset((eyeLX - r * 0.3f) * s, (eyeY - r * 0.35f) * s))
            drawCircle(Color.White, r * 0.38f * s, Offset((eyeRX - r * 0.3f) * s, (eyeY - r * 0.35f) * s))
            drawCircle(Color.White.copy(alpha = 0.7f), r * 0.16f * s, Offset((eyeLX + r * 0.35f) * s, (eyeY + r * 0.25f) * s))
            drawCircle(Color.White.copy(alpha = 0.7f), r * 0.16f * s, Offset((eyeRX + r * 0.35f) * s, (eyeY + r * 0.25f) * s))
        }
    }

    // ---- 鼻子（带高光）----
    drawOval(NoseColor, topLeft = Offset(115f * s, 139f * s), size = Size(10f * s, 7f * s))
    drawCircle(Color.White.copy(alpha = 0.8f), 1.6f * s, Offset(117.5f * s, 140.5f * s))

    // ---- 嘴 ----
    val mouthStroke = Stroke(width = 3f * s, cap = StrokeCap.Round)
    when {
        tapHappy || mood == PetMood.HAPPY || mood == PetMood.EXCITED ->
            drawArc(DarkColor, 15f, 150f, false, Offset(108f * s, 148f * s), Size(24f * s, 12f * s), style = mouthStroke)
        mood == PetMood.SAD ->
            drawArc(DarkColor, 195f, 150f, false, Offset(108f * s, 154f * s), Size(24f * s, 12f * s), style = mouthStroke)
        else ->
            // 平静也是浅浅的微笑
            drawArc(DarkColor, 25f, 130f, false, Offset(110f * s, 148f * s), Size(20f * s, 9f * s), style = mouthStroke)
    }

    // ---- 胡须 ----
    val whisker = DarkColor.copy(alpha = 0.55f)
    drawLine(whisker, Offset(60f * s, 132f * s), Offset(24f * s, 124f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
    drawLine(whisker, Offset(60f * s, 141f * s), Offset(22f * s, 141f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
    drawLine(whisker, Offset(60f * s, 150f * s), Offset(24f * s, 158f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
    drawLine(whisker, Offset(180f * s, 132f * s), Offset(216f * s, 124f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
    drawLine(whisker, Offset(180f * s, 141f * s), Offset(218f * s, 141f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
    drawLine(whisker, Offset(180f * s, 150f * s), Offset(216f * s, 158f * s), strokeWidth = 2.5f * s, cap = StrokeCap.Round)

    // ---- 腮红（径向渐变融入皮肤）----
    drawCircle(
        brush = Brush.radialGradient(listOf(BlushCore, Color.Transparent), center = Offset(72f * s, 150f * s), radius = 10f * s),
        radius = 10f * s,
        center = Offset(72f * s, 150f * s),
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(BlushCore, Color.Transparent), center = Offset(168f * s, 150f * s), radius = 10f * s),
        radius = 10f * s,
        center = Offset(168f * s, 150f * s),
    )
}

private fun earPath(s: Float, ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Path {
    // b 为尖端：在两条邻边上取 25% 处的点，用二次贝塞尔绕过尖端形成圆角
    val t = 0.25f
    return Path().apply {
        moveTo(ax * s, ay * s)
        lineTo((bx + (ax - bx) * t) * s, (by + (ay - by) * t) * s)
        quadraticTo(bx * s, by * s, (bx + (cx - bx) * t) * s, (by + (cy - by) * t) * s)
        lineTo(cx * s, cy * s)
        close()
    }
}
