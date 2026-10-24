@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.page

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.astraflow.Chizuru.ui.AppActions
import com.astraflow.Chizuru.ui.kit.Astra
import com.astraflow.Chizuru.ui.kit.AstraMenuItem
import com.astraflow.Chizuru.ui.kit.AstraQuickActionsDialog
import com.astraflow.Chizuru.ui.kit.rememberLastNonNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.theme.CupertinoTheme

/*
 * 贡献者头像墙:名单第一位居中放得最大,其余按顺时针围成一圈,支持拖动与双指缩放,轻点头像查看详情。
 * 头像与页面其它部件使用同一套上游部件;这一版的动效:
 *  - 排布:中心一位 + 外圈一圈,圈半径按"相邻两个头像之间留出一个间距"算出来,
 *    任意两个头像都不会叠在一起,也不会为了球面观感往中心挤;
 *  - 入场:从中心那位开始、顺着外圈的顺序依次弹出(缩放过冲+淡入),不是整排同时出现;
 *  - 静止:每个头像按各自相位轻微浮动;
 *  - 按下:被按住的头像略微放大、浮起,并显示主题色描边;
 *  - 点开:自头像位置扩散一圈水波,头像留在放大位置并亮起主题色光环,
 *    其余头像退后、变淡,等详情收起后再一起落回(不会出现放大一下又缩回去);
 *  - 收起:面板(260 毫秒)快落到底时,选中的头像用 300 毫秒缓出回到原位,光环与主题色描边
 *    一起渐隐、渐回静息的白描边,其余头像同时回位;落回播完才清掉选中,中途不会换公式跳一下;
 *  - 拖动:边缘阻尼,松手弹回边界;双指缩放范围为 0.7 至 2.2 倍。
 */

private val WallHeight = 240.dp

/** 中心那一位(名单第一位)的直径,比外圈大一号。 */
private val CentreAvatar = 74.dp

/** 外圈头像的直径。 */
private val RingAvatar = 54.dp

/** 绘制盒取最大那个头像的尺寸;外圈在绘图层里等比缩小,不走放大,头像不会发虚。 */
private val AvatarBox = CentreAvatar

/** 相邻两个头像最紧处留出的间距。 */
private val AvatarGap = 8.dp

/** 头像群离卡片边的最小留白(决定整群的缩放)。 */
private val AvatarInset = 8.dp

/** 一圈最多放几个:名单多于这个数就往外套第二圈。 */
private const val RingCount = 8

private const val MinZoom = 0.7f
private const val MaxZoom = 2.2f

/** 头像位置(相对卡片中心)、各自的显示直径,以及把整群放进卡片所需的缩放。 */
private class WallGeometry(
    val centers: List<Offset>,
    val sizes: List<Float>,
    val radius: Float,
    val fitScale: Float,
    val contentWidth: Float,
    val contentHeight: Float,
)

/**
 * 中心放名单第一位,其余按顺时针均匀围成一圈(人多时自动往外再加一圈)。
 * 圈半径按"相邻两个头像之间留出 AvatarGap"算:同一圈看弦长(2r·sin(π/n)),
 * 与上一圈看径向最短距离。这样任意两个头像都不会叠在一起 —— 原来是按球面观感把外圈
 * 往中心压缩、再缩小,压出来的间距小于头像直径,看起来就是几个头像叠在一起。
 */
private fun buildGeometry(width: Int, height: Int, count: Int, density: Float): WallGeometry {
    if (count <= 0 || width <= 0 || height <= 0) {
        return WallGeometry(emptyList(), emptyList(), 0f, 1f, 0f, 0f)
    }
    val centreSize = CentreAvatar.value * density
    val ringSize = RingAvatar.value * density
    val gap = AvatarGap.value * density
    val centers = ArrayList<Offset>(count)
    val sizes = ArrayList<Float>(count)
    centers += Offset.Zero
    sizes += centreSize
    var previousOuter = centreSize / 2f
    var placed = 1
    while (placed < count) {
        val inRing = min(RingCount, count - placed)
        val radius = max(
            previousOuter + gap + ringSize / 2f,
            (ringSize + gap) / (2f * sin(PI.toFloat() / inRing)),
        )
        repeat(inRing) { seat ->
            // 从正上方开始顺时针排,名单顺序就是绕一圈的顺序
            val angle = -PI.toFloat() / 2f + seat * 2f * PI.toFloat() / inRing
            centers += Offset(cos(angle) * radius, sin(angle) * radius)
            sizes += ringSize
        }
        placed += inRing
        previousOuter = radius + ringSize / 2f
    }
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    centers.forEachIndexed { index, centre ->
        val half = sizes[index] / 2f
        minX = min(minX, centre.x - half)
        maxX = max(maxX, centre.x + half)
        minY = min(minY, centre.y - half)
        maxY = max(maxY, centre.y + half)
    }
    val offsetX = (minX + maxX) / 2f
    val offsetY = (minY + maxY) / 2f
    val centered = centers.map { Offset(it.x - offsetX, it.y - offsetY) }
    val contentWidth = maxX - minX
    val contentHeight = maxY - minY
    val inset = AvatarInset.value * density
    val fit = min(
        (width - inset * 2f) / contentWidth,
        (height - inset * 2f) / contentHeight,
    ).coerceAtMost(1f)
    return WallGeometry(centered, sizes, max(contentWidth, contentHeight) / 2f, fit, contentWidth, contentHeight)
}

/** 弹出的过冲曲线:收尾时略微超过 1 再落回,弹一下。 */
private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val x = t - 1f
    return 1f + (c1 + 1f) * x * x * x + c1 * x * x
}

/** 读取头像:缩到 200 像素见方、居中裁成正方形,画成圆形;读不到时返回空。 */
private fun loadAvatar(context: android.content.Context, name: String): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { context.assets.open("avatars/$name").use { BitmapFactory.decodeStream(it, null, bounds) } }
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 200 && bounds.outHeight / (sample * 2) >= 200) sample *= 2
    val source = runCatching {
        context.assets.open("avatars/$name").use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull() ?: return null
    val side = min(source.width, source.height)
    val square: Bitmap = if (side == source.width && side == source.height) {
        source
    } else {
        Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
    }
    return square.asImageBitmap()
}

@Composable
fun ContributorWall(
    contributors: List<Contributor>,
    onPick: (Int) -> Unit,
    selectedIndex: Int = -1,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val avatars by produceState<List<ImageBitmap?>>(emptyList(), contributors) {
        value = withContext(Dispatchers.IO) { contributors.map { loadAvatar(context, it.avatar) } }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current.density
    val geometry = remember(size, contributors.size, density) {
        buildGeometry(size.width, size.height, contributors.size, density)
    }
    val scope = rememberCoroutineScope()
    var pan by remember { mutableStateOf(Offset.Zero) }
    var zoomValue by remember { mutableFloatStateOf(1f) }
    var pressed by remember { mutableIntStateOf(-1) }
    var focusIndex by remember { mutableIntStateOf(-1) }
    var rippleAt by remember { mutableStateOf(Offset.Zero) }
    val ripple = remember { mutableFloatStateOf(0f) }
    val enter = remember { mutableFloatStateOf(0f) }
    val clock = remember { mutableFloatStateOf(0f) }
    val dark = CupertinoTheme.colorScheme.isDark
    val accent = Astra.colors.accent
    val avatarPx = AvatarBox.value * density

    // 选中的头像留在放大位置,详情收起(selectedIndex 变回 -1)后再落回。
    // 原来点开后 420 毫秒就自己把放大撤掉,于是出现"放大一下又缩回去"。
    // 放大用弹簧(点和开的手感),落回用定长缓出:收起时是一段明确的回位,
    // 不能像弹簧那样拖几百毫秒的尾巴(用户 2026-10-24:收起动画不行)。
    // focusIndex 要等落回播完才清,否则中途换公式(选中分支 1.26 倍、普通分支 0.88 倍)
    // 会让头像一个照面缩掉再慢慢长回来。
    // 这里按"第几位"认人,不能按名字反查:名单里有三位都叫「开发人员」,
    // 按名字找永远命中第一位,点后面那两位会把高亮放到别人身上(用户 2026-10-24:点了头像跳的是另一个人)。
    val focus = remember { Animatable(0f) }
    LaunchedEffect(selectedIndex, contributors) {
        val index = selectedIndex.takeIf { it in contributors.indices } ?: -1
        if (index >= 0) {
            focusIndex = index
            focus.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow))
        } else if (focusIndex >= 0 || focus.value > 0f) {
            // 面板收起是 260 毫秒的缓出,这里等它走到末尾再落回,两段动画接得上、也不互相抢
            delay(220)
            focus.animateTo(0f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
            focusIndex = -1
        }
    }

    // 入场:自中心向外依次弹出;进度只在绘图层读取,不引起重组
    LaunchedEffect(contributors.size) {
        enter.floatValue = 0f
        animate(0f, 1f, animationSpec = tween(durationMillis = 900)) { value, _ -> enter.floatValue = value }
    }
    // 静止时的轻微浮动:只在绘图层里读取,不引起重组
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { clock.floatValue = it / 1_000_000_000f }
        }
    }

    val halfW = (size.width / 2f).coerceAtLeast(1f)
    val halfH = (size.height / 2f).coerceAtLeast(1f)

    fun drawPosition(index: Int): Pair<Offset, Float> {
        val scale = geometry.fitScale * zoomValue
        val centre = geometry.centers[index]
        val position = Offset(centre.x * scale + pan.x, centre.y * scale + pan.y)
        val radius = geometry.sizes[index] / 2f * scale
        return Offset(halfW + position.x, halfH + position.y) to radius
    }

    fun hitTest(position: Offset): Int {
        // 先看手指是不是落在某个头像的圆里:落在里面就一定算这一位,
        // 否则相邻头像向外放宽的点击范围会在缝里抢走这一下,点谁都可能开出旁边那位。
        var inside = -1
        var insideDistance = Float.MAX_VALUE
        var near = -1
        var nearDistance = Float.MAX_VALUE
        geometry.centers.indices.forEach { index ->
            val (center, radius) = drawPosition(index)
            val distance = hypot(position.x - center.x, position.y - center.y)
            if (distance <= radius && distance < insideDistance) {
                insideDistance = distance
                inside = index
            }
            if (distance <= radius + 6f * density && distance < nearDistance) {
                nearDistance = distance
                near = index
            }
        }
        return if (inside >= 0) inside else near
    }

    fun pick(index: Int) {
        if (index < 0) return
        rippleAt = drawPosition(index).first
        scope.launch {
            animate(0f, 1f, animationSpec = tween(420)) { value, _ -> ripple.floatValue = value }
            ripple.floatValue = 0f
        }
        onPick(index)
    }

    fun settle() {
        val limitX = max(0f, (geometry.contentWidth * geometry.fitScale * zoomValue - size.width) / 2f)
        val limitY = max(0f, (geometry.contentHeight * geometry.fitScale * zoomValue - size.height) / 2f)
        val target = Offset(pan.x.coerceIn(-limitX, limitX), pan.y.coerceIn(-limitY, limitY))
        if (target == pan) return
        val start = pan
        scope.launch {
            animate(0f, 1f, animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                pan = start + (target - start) * value
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(WallHeight)
            .onSizeChanged { size = it },
    ) {
        if (size.width > 0 && geometry.centers.isNotEmpty()) {
            // 背景:一圈很淡的轨道线加一层随头像群的柔光,让这一块有里外层次
            Canvas(Modifier.fillMaxSize()) {
                val scale = geometry.fitScale * zoomValue
                val ring = geometry.radius * scale + 6.dp.toPx()
                val reveal = (enter.floatValue / 0.4f).coerceIn(0f, 1f)
                val glowCentre = Offset(halfW + pan.x * 0.35f, halfH + pan.y * 0.35f)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to accent.copy(alpha = 0.10f * reveal),
                        0.7f to accent.copy(alpha = 0.04f * reveal),
                        1f to Color.Transparent,
                        center = glowCentre,
                        radius = ring,
                    ),
                    radius = ring,
                    center = glowCentre,
                )
                drawCircle(
                    color = accent.copy(alpha = 0.10f * reveal),
                    radius = ring,
                    center = Offset(halfW + pan.x, halfH + pan.y),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            // 点开时自头像位置扩散的一圈水波,画在头像后面
            if (ripple.floatValue > 0f) {
                Canvas(Modifier.fillMaxSize()) {
                    val progress = ripple.floatValue
                    val radius = 18.dp.toPx() + 36.dp.toPx() * progress
                    drawCircle(accent.copy(alpha = 0.10f * (1f - progress)), radius = radius, center = rippleAt)
                    drawCircle(
                        color = accent.copy(alpha = 0.5f * (1f - progress)),
                        radius = radius,
                        center = rippleAt,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            contributors.forEachIndexed { index, contributor ->
                val bitmap = avatars.getOrNull(index)
                val isFocused = index == focusIndex
                val isPressed = pressed == index
                Box(
                    Modifier
                        .size(AvatarBox)
                        .graphicsLayer {
                            val (center, radius) = drawPosition(index)
                            val phase = index * 0.9f
                            // 入场:从中心那位开始、顺着外圈的顺序依次弹出
                            val revealAt = 0.05f + 0.62f * index / max(1, contributors.size - 1).toFloat()
                            val reveal = ((enter.floatValue - revealAt) / 0.24f).coerceIn(0f, 1f)
                            val appear = easeOutBack(reveal)
                            // 选中时其余头像退后、变淡;选中的自己放大,并缓慢呼吸。
                            // 收起时 current 平滑回 0,两个分支到 0 都是 1 倍,所以落回不会跳
                            val current = focus.value
                            val rest = if (isFocused) 1f else 1f - 0.45f * current
                            val focusScale = if (isFocused) {
                                1f + 0.26f * current + 0.015f * current * sin(clock.floatValue * 1.6f)
                            } else {
                                1f - 0.12f * current
                            }
                            val drift = sin(clock.floatValue * 0.7f + phase) * 1.4.dp.toPx() * rest
                            val driftY = cos(clock.floatValue * 0.55f + phase) * 1.0.dp.toPx() * rest
                            translationX = center.x - avatarPx / 2f + drift
                            translationY = center.y - avatarPx / 2f + driftY
                            val press = if (isPressed) 1.06f else 1f
                            scaleX = (radius * 2f / avatarPx) * appear * press * focusScale
                            scaleY = scaleX
                            alpha = reveal.coerceAtMost(1f) * rest
                        }
                        .drawBehind {
                            // 光环画在头像底下。这里的 size 是画布自己的尺寸,外层同名变量是卡片尺寸(IntSize)
                            val current = focus.value
                            if (isFocused && current > 0.01f) {
                                drawCircle(color = accent.copy(alpha = 0.16f * current), radius = this.size.minDimension * 0.72f)
                            }
                        }
                        .clip(CircleShape)
                        .then(
                            when {
                                // 选中时的主题色描边改画在头像上层(见下面的 Canvas),淡出只走绘制阶段
                                isFocused -> Modifier
                                isPressed -> Modifier.border(2.dp, accent.copy(alpha = 0.9f), CircleShape)
                                else -> Modifier.border(1.dp, Color.White.copy(alpha = if (dark) 0.18f else 0.65f), CircleShape)
                            },
                        ),
                ) {
                    if (bitmap != null) {
                        Image(bitmap, contentDescription = contributor.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(Astra.colors.fill), contentAlignment = Alignment.Center) {
                            CupertinoText(contributor.name.take(1), style = CupertinoTheme.typography.headline)
                        }
                    }
                    // 选中的描边画在头像上层:落回时由主题色渐变回静息的白描边,
                    // 清掉选中那一刻不会换个颜色跳一下
                    if (isFocused) {
                        Canvas(Modifier.matchParentSize()) {
                            val current = focus.value
                            val restColor = Color.White.copy(alpha = if (dark) 0.18f else 0.65f)
                            drawCircle(
                                color = lerp(restColor, accent.copy(alpha = 0.9f), current),
                                // 外层有个同名变量(卡片尺寸 IntSize)会遮蔽,这里要写 this.size
                                radius = this.size.minDimension / 2f - 1.dp.toPx(),
                                style = Stroke(width = (1f + current).dp.toPx()),
                            )
                        }
                    }
                }
            }
            // 手势:单指拖动、双指缩放、轻点选人;拖动时不让外层列表跟着滚
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(geometry, contributors.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val hit = hitTest(down.position)
                            pressed = hit
                            var moved = false
                            var panAccum = Offset.Zero
                            var zoomAccum = 1f
                            val startPan = pan
                            val startZoom = zoomValue
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                if (!moved && (abs(zoomChange - 1f) > 0.01f || panChange.getDistance() > viewConfiguration.touchSlop)) {
                                    moved = true
                                }
                                if (moved) {
                                    event.changes.forEach { it.consume() }
                                    panAccum += panChange
                                    if (pressedCount >= 2) zoomAccum = (zoomAccum * zoomChange).coerceIn(0.8f, 1.25f)
                                    val nextZoom = (startZoom * zoomAccum).coerceIn(MinZoom, MaxZoom)
                                    zoomValue = nextZoom
                                    val limitX = max(0f, (geometry.contentWidth * geometry.fitScale * nextZoom - size.width) / 2f)
                                    val limitY = max(0f, (geometry.contentHeight * geometry.fitScale * nextZoom - size.height) / 2f)
                                    pan = Offset(
                                        rubber(startPan.x + panAccum.x, limitX, size.width.toFloat()),
                                        rubber(startPan.y + panAccum.y, limitY, size.height.toFloat()),
                                    )
                                }
                                if (event.changes.all { !it.pressed }) {
                                    // 这一次触摸由头像墙处理完，不再传给外层
                                    if (!moved && hit >= 0) event.changes.forEach { it.consume() }
                                    break
                                }
                            }
                            pressed = -1
                            if (!moved) pick(hit)
                            settle()
                        }
                    },
            )
        }
    }
}

/** 拖过边界时只移动一点(阻尼),松手弹回。 */
private fun rubber(value: Float, bound: Float, extent: Float): Float {
    val overflow = abs(value) - bound
    if (overflow <= 0f) return value
    val travel = extent * 0.18f
    return sign(value) * (bound + travel * overflow / (travel + overflow))
}

/**
 * 贡献者详情:和星流维护按钮同一个部件(底部操作面板)。姓名写在面板中缝,简介与标签接在下面,
 * 「打开链接」是面板里的一项操作;面板外面点一下、按返回或点取消收起。
 */
@Composable
fun ContributorDetailSheet(contributor: Contributor?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    // 收起动画播完前内容先留着:调用方按 contributor 是否为空来收,收起途中不能先变空白
    val shown = rememberLastNonNull(contributor)
    val summary = shown?.summary?.takeIf { it.isNotBlank() } ?: "这位贡献者没有留下简介。"
    val links = shown?.chips.orEmpty()
    val hasLink = shown != null && shown.link.isNotBlank()
    val message = buildString {
        append(summary)
        if (links.isNotEmpty()) {
            append("\n\n")
            append(links.joinToString(" · "))
        }
        if (shown != null && !hasLink) {
            append("\n\n暂无公开链接")
        }
    }
    AstraQuickActionsDialog(
        visible = contributor != null,
        onDismiss = onDismiss,
        title = shown?.name ?: "贡献者",
        message = message,
        items = if (shown != null && hasLink) {
            listOf(AstraMenuItem("打开链接", onClick = { AppActions.openUri(context, shown.link) }))
        } else {
            emptyList()
        },
    )
}
