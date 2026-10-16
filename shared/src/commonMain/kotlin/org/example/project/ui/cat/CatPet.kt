package org.example.project.ui.cat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.cat_excited
import kotlinproject.shared.generated.resources.cat_happy
import kotlinproject.shared.generated.resources.cat_lazy
import kotlinproject.shared.generated.resources.cat_normal
import kotlinproject.shared.generated.resources.cat_sad
import kotlinproject.shared.generated.resources.cat_sleepy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import org.example.project.pet.PetMood
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.rememberResourceEnvironment

private class FloatParticle(val id: Long, val emoji: String, val xBias: Float)

/** 点击时短暂展示的表情 key（非心情，而是互动反应） */
private const val TAP_REACTION_KEY = "tap_excited"

/** 心情 → 精灵图（assets/cat 导入的 3D 粘土渲染风橘猫，透明底 1024×1024） */
private fun moodImage(mood: PetMood): DrawableResource = when (mood) {
    PetMood.HAPPY -> Res.drawable.cat_happy
    PetMood.EXCITED -> Res.drawable.cat_excited
    PetMood.NORMAL -> Res.drawable.cat_normal
    PetMood.LAZY -> Res.drawable.cat_lazy
    PetMood.SLEEPY -> Res.drawable.cat_sleepy
    PetMood.SAD -> Res.drawable.cat_sad
}

private fun imageForKey(key: String): DrawableResource =
    if (key == TAP_REACTION_KEY) Res.drawable.cat_excited else moodImage(PetMood.valueOf(key))

/**
 * 3D 渲染风猫咪精灵图 + 程序带动效：呼吸缩放、果冻感点击弹跳、
 * 贴纸弹跳式表情切换、地面软阴影、上浮粒子，动画节奏随 [mood] 变化。
 * 图集替换只需改 [moodImage] 映射，调用方签名不变。
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

    // ---- 点击：下蹲蓄力 → 换兴奋表情拉伸起跳 → 加速落地 → 压扁果冻回弹 ----
    val hop = remember { Animatable(0f) }
    val squash = remember { Animatable(1f) }
    val stretch = remember { Animatable(1f) }
    val tilt = remember { Animatable(0f) }
    var tapSeq by remember { mutableIntStateOf(0) }
    var tapHappy by remember { mutableStateOf(false) }
    LaunchedEffect(tapSeq) {
        if (tapSeq == 0) return@LaunchedEffect
        hop.snapTo(0f)
        squash.snapTo(1f)
        stretch.snapTo(1f)
        tilt.snapTo(0f)
        // 1) 蓄力下蹲（表情不变，像真的在攒劲）
        squash.animateTo(1f - 0.12f * squashAmp, tween(110, easing = FastOutLinearInEasing))
        // 2) 起跳瞬间换兴奋表情，纵向拉伸 + 微侧倾，跳起慢、落回快
        tapHappy = true
        launch {
            hop.animateTo(-18f, tween(220, easing = LinearOutSlowInEasing))
            hop.animateTo(0f, tween(180, easing = FastOutLinearInEasing))
        }
        launch {
            tilt.animateTo(-4f, tween(140))
            tilt.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
        launch {
            stretch.animateTo(1f + 0.07f * squashAmp, tween(120))
            stretch.animateTo(1f, tween(150))
        }
        // 3) 空中形态恢复正常，等待落地（跳起共约 400ms）
        squash.animateTo(1f, tween(120))
        delay(150)
        // 4) 落地压扁 → 果冻回弹
        squash.animateTo(1f - 0.1f * squashAmp, tween(70))
        squash.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        delay(350)
        tapHappy = false
    }

    // ---- 表情切换：缩小收起 → 换图 → 弹性放大（贴纸弹跳式，避免叠影） ----
    val desiredKey = if (tapHappy) TAP_REACTION_KEY else mood.name
    var shownKey by remember { mutableStateOf(desiredKey) }
    val swap = remember { Animatable(1f) }
    LaunchedEffect(desiredKey) {
        if (desiredKey == shownKey) return@LaunchedEffect
        swap.animateTo(0f, tween(130, easing = FastOutLinearInEasing))
        shownKey = desiredKey
        swap.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }

    // ---- 后台异步预解码全部精灵图：切换直接用内存位图，不在 UI 线程解码 ----
    var bitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    val resourceEnv = rememberResourceEnvironment()
    LaunchedEffect(Unit) {
        val decoded = withContext(Dispatchers.Default) {
            PetMood.entries.mapNotNull { m ->
                runCatching {
                    m.name to getDrawableResourceBytes(resourceEnv, moodImage(m)).decodeToImageBitmap()
                }.getOrNull()
            }
        }
        bitmaps = decoded.toMap()
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

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures {
                    tapSeq++
                    spawnParticles("🧡", 3)
                    onTap()
                }
            },
    ) {
        // 地面软阴影：不跟随猫咪位移，跳起时缩小变淡，反衬高度
        val hopHeight = -hop.value
        val shadowScale = (1f - hopHeight / 50f + floatY / 120f).coerceIn(0.55f, 1f)
        val shadowAlpha = (0.25f - hopHeight / 90f).coerceIn(0.08f, 0.25f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.minDimension / 240f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
                    center = Offset(120f * s, 224f * s),
                    radius = 66f * s * shadowScale,
                ),
                topLeft = Offset((120f - 66f * shadowScale) * s, (224f - 10f) * s),
                size = Size(132f * shadowScale * s, 20f * s),
            )
        }

        // 猫咪精灵图：呼吸/果冻形变/侧倾叠加在整体上，换图经 swap 弹跳过渡
        // 优先用预解码位图；预加载完成前（首帧/预览截图）回退 painterResource
        val shownBitmap = bitmaps[if (shownKey == TAP_REACTION_KEY) PetMood.EXCITED.name else shownKey]
        val swapScale = 0.85f + 0.15f * swap.value
        val imageModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = (floatY + hop.value) * density
                rotationZ = tilt.value
                scaleX = (1f + (1f - squash.value) * 0.6f - (stretch.value - 1f) * 0.4f) * swapScale
                scaleY = breath * squash.value * stretch.value * swapScale
                alpha = 0.5f + 0.5f * swap.value
                transformOrigin = TransformOrigin(0.5f, 0.9f)
            }
        if (shownBitmap != null) {
            Image(bitmap = shownBitmap, contentDescription = mood.label, modifier = imageModifier)
        } else {
            Image(painter = painterResource(imageForKey(shownKey)), contentDescription = mood.label, modifier = imageModifier)
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
