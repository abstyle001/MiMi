package org.example.project.ui.cat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import org.example.project.pet.AccessoryCatalog
import org.example.project.pet.AccessorySlot
import org.example.project.pet.GrowthStage
import org.example.project.pet.PetMood
import org.example.project.pet.growthStageForLevel
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
 *
 * [accessories] 传已装备的配饰 id 集合（帽子 / 项圈 / 背景各取一件），
 * 会跟随精灵图一起做呼吸/跳动的变换；背景绘制在猫咪身后。
 *
 * [level] 决定成长阶段（幼猫 → 青年猫 → 成年猫），只影响体型与毛色，
 * 配饰复用同一套 graphicsLayer 形变，不需要单独调整坐标。
 *
 * [drawBackground] 为 false 时不再画背景，由调用方用 [AccessoryBackground] 铺满整个舞台，
 * 避免背景被精灵图方框裁成「框中框」。
 */
@Composable
fun CatPet(
    mood: PetMood,
    onTap: () -> Unit,
    accessories: Set<String> = emptySet(),
    level: Int = 1,
    modifier: Modifier = Modifier,
    drawBackground: Boolean = true,
) {
    val growthStage = growthStageForLevel(level)
    val bodyScale = when (growthStage) {
        GrowthStage.KITTEN -> 0.82f
        GrowthStage.YOUNG -> 0.92f
        GrowthStage.ADULT -> 1f
    }
    // 跨阶段时弹性长大，避免体型瞬间跳变；首次组合直接使用目标值
    val animatedBodyScale by animateFloatAsState(
        targetValue = bodyScale,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "growthScale",
    )
    val furFilter = remember(growthStage) {
        when (growthStage) {
            // 幼猫偏柔和的浅色，成年后毛色更饱和
            GrowthStage.KITTEN -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.88f) })
            GrowthStage.YOUNG -> null
            GrowthStage.ADULT -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.12f) })
        }
    }
    val wornAccessories = remember(accessories) { accessories.mapNotNull { AccessoryCatalog.byId[it] } }
    val backgroundId = wornAccessories.firstOrNull { it.slot == AccessorySlot.BACKGROUND }?.id
    val worn = wornAccessories.filter { it.slot != AccessorySlot.BACKGROUND }

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
        // 地面软阴影：不跟随猫咪位移，跳起时缩小变淡，反衬高度；体型越小阴影越小
        val hopHeight = -hop.value
        val shadowScale = ((1f - hopHeight / 50f + floatY / 120f).coerceIn(0.55f, 1f)) * animatedBodyScale
        val shadowAlpha = (0.25f - hopHeight / 90f).coerceIn(0.08f, 0.25f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (drawBackground) backgroundId?.let { drawAccessoryBackground(it) }
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
                scaleX = (1f + (1f - squash.value) * 0.6f - (stretch.value - 1f) * 0.4f) * swapScale * animatedBodyScale
                scaleY = breath * squash.value * stretch.value * swapScale * animatedBodyScale
                alpha = 0.5f + 0.5f * swap.value
                transformOrigin = TransformOrigin(0.5f, 0.9f)
            }
        if (shownBitmap != null) {
            Image(
                bitmap = shownBitmap,
                contentDescription = mood.label,
                modifier = imageModifier,
                colorFilter = furFilter,
            )
        } else {
            Image(
                painter = painterResource(imageForKey(shownKey)),
                contentDescription = mood.label,
                modifier = imageModifier,
                colorFilter = furFilter,
            )
        }

        // 配饰叠加在精灵图之上，复用同一套形变，呼吸和跳跃时不会脱节
        if (worn.isNotEmpty()) {
            Canvas(modifier = imageModifier) {
                worn.forEach { accessory -> drawAccessory(accessory.id) }
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

/** 红领结：按画布边长比例画在脖子/胸口，精灵图缩放时一起跟随 */
private fun DrawScope.drawRedBow() {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height * 0.635f
    val wingW = s * 0.085f
    val wingH = s * 0.052f
    val ribbon = Color(0xFFD94848)
    val shadow = Color(0xFFA82F2F)

    drawPath(
        Path().apply {
            moveTo(cx, cy)
            lineTo(cx - wingW, cy - wingH)
            lineTo(cx - wingW * 0.82f, cy + wingH)
            close()
        },
        ribbon,
    )
    drawPath(
        Path().apply {
            moveTo(cx, cy)
            lineTo(cx + wingW, cy - wingH)
            lineTo(cx + wingW * 0.82f, cy + wingH)
            close()
        },
        ribbon,
    )
    // 两条垂下的飘带
    drawLine(
        shadow,
        start = Offset(cx - wingW * 0.16f, cy + wingH * 0.4f),
        end = Offset(cx - wingW * 0.5f, cy + wingH * 2.1f),
        strokeWidth = s * 0.02f,
        cap = StrokeCap.Round,
    )
    drawLine(
        shadow,
        start = Offset(cx + wingW * 0.16f, cy + wingH * 0.4f),
        end = Offset(cx + wingW * 0.5f, cy + wingH * 2.1f),
        strokeWidth = s * 0.02f,
        cap = StrokeCap.Round,
    )
    drawCircle(shadow, radius = s * 0.027f, center = Offset(cx, cy))
    drawCircle(
        Color.White.copy(alpha = 0.55f),
        radius = s * 0.009f,
        center = Offset(cx - s * 0.008f, cy - s * 0.008f),
    )
}

/** 统一分发：新增配饰在这里加分支，并保持 id 与 AccessoryCatalog 一致 */
private fun DrawScope.drawAccessory(id: String) {
    when (id) {
        AccessoryCatalog.STRAW_HAT.id -> drawStrawHat()
        AccessoryCatalog.PARTY_HAT.id -> drawPartyHat()
        AccessoryCatalog.SLEEP_CAP.id -> drawSleepCap()
        AccessoryCatalog.RED_BOW.id -> drawRedBow()
        AccessoryCatalog.BELL_COLLAR.id -> drawBellCollar()
        AccessoryCatalog.STAR_SCARF.id -> drawStarScarf()
    }
}

/**
 * 背景配饰单独铺底：传 MatchParentSize 即可铺满整个舞台卡片（圆角由 Card 裁切）。
 * 配合 `CatPet(drawBackground = false)` 使用，背景就不会被精灵图方框圈住。
 */
@Composable
fun AccessoryBackground(accessoryId: String, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawAccessoryBackground(accessoryId, CornerRadius.Zero) }
}

/** 背景：铺满猫咪舞台的圆角场景，绘制在阴影与猫之前 */
private fun DrawScope.drawAccessoryBackground(
    id: String,
    corner: CornerRadius = CornerRadius(size.minDimension * 0.07f),
) {
    val s = size.minDimension
    when (id) {
        AccessoryCatalog.BG_SUNRISE.id -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFE0A8), Color(0xFFFFC98A), Color(0xFFFFF6E3)),
                    startY = 0f,
                    endY = size.height,
                ),
                size = size,
                cornerRadius = corner,
            )
            val sunCenter = Offset(size.width * 0.80f, size.height * 0.15f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF3B0), Color(0x00FFF3B0)),
                    center = sunCenter,
                    radius = s * 0.17f,
                ),
                radius = s * 0.17f,
                center = sunCenter,
            )
            drawCircle(
                Color(0xFFFFE066),
                radius = s * 0.052f,
                center = sunCenter,
            )
            drawOval(
                Color(0xFFCBE3A2).copy(alpha = 0.65f),
                topLeft = Offset(-s * 0.1f, size.height * 0.84f),
                size = Size(size.width + s * 0.2f, size.height * 0.3f),
            )
        }

        AccessoryCatalog.BG_SAKURA.id -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFE6EE), Color(0xFFFFD3E0), Color(0xFFFFF1F5)),
                    startY = 0f,
                    endY = size.height,
                ),
                size = size,
                cornerRadius = corner,
            )
            val petals = listOf(
                0.10f to 0.14f, 0.26f to 0.07f, 0.52f to 0.12f, 0.80f to 0.08f,
                0.16f to 0.34f, 0.68f to 0.30f, 0.90f to 0.40f, 0.07f to 0.56f,
                0.60f to 0.60f, 0.84f to 0.72f, 0.28f to 0.80f, 0.48f to 0.90f,
            )
            petals.forEachIndexed { index, (x, y) ->
                drawOval(
                    color = if (index % 3 == 0) Color(0xFFFFB3C9) else Color(0xFFFFDCE6),
                    topLeft = Offset(size.width * x, size.height * y),
                    size = Size(s * 0.052f, s * 0.028f),
                )
            }
        }

        AccessoryCatalog.BG_STARRY.id -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2B2E5C), Color(0xFF3E3F78), Color(0xFF5A4E86)),
                    startY = 0f,
                    endY = size.height,
                ),
                size = size,
                cornerRadius = corner,
            )
            val stars = listOf(
                0.10f to 0.12f, 0.24f to 0.22f, 0.42f to 0.10f, 0.62f to 0.18f,
                0.84f to 0.12f, 0.14f to 0.40f, 0.76f to 0.36f, 0.34f to 0.52f,
                0.90f to 0.56f, 0.08f to 0.70f, 0.56f to 0.74f, 0.26f to 0.88f,
            )
            stars.forEachIndexed { index, (x, y) ->
                val radius = if (index % 4 == 0) s * 0.009f else s * 0.005f
                drawCircle(
                    color = Color(0xFFFFF3C4).copy(alpha = if (index % 4 == 0) 0.95f else 0.65f),
                    radius = radius,
                    center = Offset(size.width * x, size.height * y),
                )
            }
            val moonCenter = Offset(size.width * 0.80f, size.height * 0.16f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF6CE), Color(0x66FFF6CE), Color(0x00FFF6CE)),
                    center = moonCenter,
                    radius = s * 0.12f,
                ),
                radius = s * 0.12f,
                center = moonCenter,
            )
            drawCircle(
                Color(0xFFFFF2C0),
                radius = s * 0.04f,
                center = moonCenter,
            )
        }
    }
}

/** 草帽：帽檐 + 梯形帽体，卡在两只耳朵中间 */
private fun DrawScope.drawStrawHat() {
    val s = size.minDimension
    val cx = size.width / 2f
    val brimY = size.height * 0.215f
    val brimW = s * 0.44f
    val brimH = s * 0.082f
    val straw = Color(0xFFEACB8C)
    val strawDark = Color(0xFFC7A05B)

    drawOval(
        strawDark,
        topLeft = Offset(cx - brimW / 2f, brimY - brimH / 2f + s * 0.008f),
        size = Size(brimW, brimH),
    )
    drawOval(
        straw,
        topLeft = Offset(cx - brimW / 2f, brimY - brimH / 2f),
        size = Size(brimW, brimH),
    )

    val topY = size.height * 0.105f
    val bottomHalf = s * 0.15f
    val topHalf = s * 0.105f
    val crown = Path().apply {
        moveTo(cx - bottomHalf, brimY)
        quadraticTo(cx - bottomHalf * 0.95f, topY + s * 0.03f, cx - topHalf, topY)
        lineTo(cx + topHalf, topY)
        quadraticTo(cx + bottomHalf * 0.95f, topY + s * 0.03f, cx + bottomHalf, brimY)
        close()
    }
    drawPath(crown, straw)
    repeat(3) { index ->
        val y = topY + s * (0.03f + index * 0.024f)
        drawLine(
            strawDark.copy(alpha = 0.55f),
            start = Offset(cx - topHalf * 0.9f, y),
            end = Offset(cx + topHalf * 0.9f, y),
            strokeWidth = s * 0.005f,
        )
    }
    drawRoundRect(
        color = Color(0xFFD9634B),
        topLeft = Offset(cx - bottomHalf * 0.98f, brimY - s * 0.03f),
        size = Size(bottomHalf * 1.96f, s * 0.028f),
        cornerRadius = CornerRadius(s * 0.012f),
    )
}

/** 派对帽：尖顶彩帽 + 顶部绒球 */
private fun DrawScope.drawPartyHat() {
    val s = size.minDimension
    val cx = size.width / 2f
    val baseY = size.height * 0.215f
    val apexY = size.height * 0.055f
    val halfW = s * 0.115f

    val cone = Path().apply {
        moveTo(cx - halfW, baseY)
        quadraticTo(cx - halfW * 0.35f, baseY - s * 0.08f, cx, apexY)
        quadraticTo(cx + halfW * 0.35f, baseY - s * 0.08f, cx + halfW, baseY)
        close()
    }
    drawPath(
        cone,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFA386E8), Color(0xFF7B5BC7)),
            start = Offset(cx - halfW, baseY),
            end = Offset(cx + halfW, baseY),
        ),
    )
    // 斜向彩条
    drawLine(
        Color(0xFFFFD56B),
        start = Offset(cx - halfW * 0.55f, baseY - s * 0.02f),
        end = Offset(cx + halfW * 0.25f, apexY + s * 0.05f),
        strokeWidth = s * 0.018f,
        cap = StrokeCap.Round,
    )
    drawLine(
        Color(0xFFFF8FB1),
        start = Offset(cx - halfW * 0.05f, baseY - s * 0.005f),
        end = Offset(cx + halfW * 0.55f, apexY + s * 0.07f),
        strokeWidth = s * 0.014f,
        cap = StrokeCap.Round,
    )
    drawCircle(Color(0xFFFFD56B), radius = s * 0.036f, center = Offset(cx, apexY - s * 0.012f))
    drawCircle(
        Color.White.copy(alpha = 0.5f),
        radius = s * 0.013f,
        center = Offset(cx - s * 0.008f, apexY - s * 0.02f),
    )
}

/** 睡帽：软趴趴的帽身 + 垂到一侧的绒球 */
private fun DrawScope.drawSleepCap() {
    val s = size.minDimension
    val cx = size.width / 2f
    val capColor = Color(0xFF9FB4E8)
    val bandColor = Color(0xFFEDF1FF)

    val cap = Path().apply {
        moveTo(cx - s * 0.16f, size.height * 0.225f)
        quadraticTo(cx - s * 0.18f, size.height * 0.13f, cx - s * 0.04f, size.height * 0.125f)
        quadraticTo(cx + s * 0.09f, size.height * 0.12f, cx + s * 0.12f, size.height * 0.175f)
        quadraticTo(cx + s * 0.14f, size.height * 0.205f, cx + s * 0.16f, size.height * 0.225f)
        close()
    }
    drawPath(cap, capColor)
    drawRoundRect(
        color = bandColor,
        topLeft = Offset(cx - s * 0.165f, size.height * 0.21f),
        size = Size(s * 0.33f, s * 0.036f),
        cornerRadius = CornerRadius(s * 0.018f),
    )
    // 垂下的帽尖 + 绒球
    val tail = Path().apply {
        moveTo(cx + s * 0.1f, size.height * 0.14f)
        quadraticTo(cx + s * 0.19f, size.height * 0.135f, cx + s * 0.185f, size.height * 0.175f)
    }
    drawPath(tail, capColor, style = Stroke(width = s * 0.03f, cap = StrokeCap.Round))
    drawCircle(bandColor, radius = s * 0.042f, center = Offset(cx + s * 0.185f, size.height * 0.185f))
    drawCircle(
        Color.White.copy(alpha = 0.6f),
        radius = s * 0.015f,
        center = Offset(cx + s * 0.172f, size.height * 0.172f),
    )
}

/** 铃铛项圈：弧形项带 + 金色小铃铛 */
private fun DrawScope.drawBellCollar() {
    val s = size.minDimension
    val cx = size.width / 2f
    val neckY = size.height * 0.585f
    val halfW = s * 0.155f
    val band = Path().apply {
        moveTo(cx - halfW, neckY)
        quadraticTo(cx, neckY + s * 0.07f, cx + halfW, neckY)
    }
    drawPath(band, Color(0xFF32697C), style = Stroke(width = s * 0.042f, cap = StrokeCap.Round))
    drawPath(band, Color(0xFF69B2C4), style = Stroke(width = s * 0.02f, cap = StrokeCap.Round))

    val bell = Offset(cx, neckY + s * 0.058f)
    drawCircle(Color(0xFFC99733), radius = s * 0.033f, center = bell)
    drawCircle(Color(0xFFF6D983), radius = s * 0.024f, center = Offset(bell.x, bell.y - s * 0.004f))
    drawCircle(
        Color.White.copy(alpha = 0.7f),
        radius = s * 0.008f,
        center = Offset(bell.x - s * 0.008f, bell.y - s * 0.012f),
    )
    drawLine(
        Color(0xFF8A6420),
        start = Offset(bell.x - s * 0.009f, bell.y + s * 0.002f),
        end = Offset(bell.x + s * 0.009f, bell.y + s * 0.002f),
        strokeWidth = s * 0.006f,
        cap = StrokeCap.Round,
    )
    drawCircle(Color(0xFF8A6420), radius = s * 0.006f, center = Offset(bell.x, bell.y + s * 0.014f))
}

/** 星星围巾：绕颈一圈 + 垂下的围巾尾 + 星星装饰 */
private fun DrawScope.drawStarScarf() {
    val s = size.minDimension
    val cx = size.width / 2f
    val neckY = size.height * 0.585f
    val halfW = s * 0.16f
    val scarf = Color(0xFF6D7FC4)
    val scarfLight = Color(0xFF98A8E2)

    val band = Path().apply {
        moveTo(cx - halfW, neckY)
        quadraticTo(cx, neckY + s * 0.072f, cx + halfW, neckY)
    }
    drawPath(band, scarf, style = Stroke(width = s * 0.055f, cap = StrokeCap.Round))
    drawPath(band, scarfLight, style = Stroke(width = s * 0.02f, cap = StrokeCap.Round))

    val tail = Path().apply {
        moveTo(cx - halfW * 0.75f, neckY + s * 0.03f)
        lineTo(cx - halfW * 0.95f, neckY + s * 0.15f)
        lineTo(cx - halfW * 0.55f, neckY + s * 0.14f)
        lineTo(cx - halfW * 0.35f, neckY + s * 0.05f)
        close()
    }
    drawPath(tail, scarf)
    drawStar(
        center = Offset(cx - halfW * 0.75f, neckY + s * 0.165f),
        radius = s * 0.032f,
        color = Color(0xFFFFD66B),
    )
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    repeat(10) { index ->
        val r = if (index % 2 == 0) radius else radius * 0.45f
        val angle = (-PI / 2 + index * PI / 5).toFloat()
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@Composable
private fun BoxScope.ParticleView(particle: FloatParticle, onDone: () -> Unit) {    val progress = remember { Animatable(0f) }
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
