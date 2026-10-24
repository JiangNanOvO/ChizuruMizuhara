package com.astraflow.Chizuru.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astraflow.Chizuru.ui.theme.LocalEnableBlur
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.CupertinoLiquidIconButton
import zone.ien.hig.CupertinoSegmentedControl
import zone.ien.hig.CupertinoSegmentedControlTab
import zone.ien.hig.CupertinoSlider
import zone.ien.hig.CupertinoSwitch
import zone.ien.hig.CupertinoText
import zone.ien.hig.utils.rememberDefaultBackdrop

/*
 * 开关、滑条、分段选择、圆形玻璃按钮:原样用上游 compose-hig 的液态玻璃部件(样子、手感、触感都是上游的)。
 * 浮在页面上的(顶部栏、底栏上的)从页面录下的画面取背后的样子;页面里面的不取(见 AstraScaffold 的说明)。
 * 这里只接上读屏说明、不能点时变淡。
 */

/** 页面录下的画面(上游示例里的 backdrop),给浮在页面上的底栏用。页面外面没有时给一个空的。 */
@Composable
internal fun astraPageBackdrop(): LayerBackdrop = LocalAstraBackdrop.current ?: rememberLayerBackdrop()

/**
 * 玻璃取背后画面的地方:浮在页面上的(顶部栏、底栏上的按钮)取页面录下的画面;页面里面的用上游默认的
 * (上游说明里的写法 rememberDefaultBackdrop)。设置里「工具按钮背景模糊」关掉时不取页面画面。
 */
@Composable
internal fun astraGlassBackdrop(): Backdrop {
    val page = LocalAstraBackdrop.current
    return if (page != null && LocalEnableBlur.current) page else rememberDefaultBackdrop()
}

/**
 * 开关:上游的液态玻璃开关。[onCheckedChange] 为空时只显示,不能点。
 */
@Composable
fun AstraSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    @Suppress("UNUSED_PARAMETER") interactionSource: MutableInteractionSource? = null,
) {
    SwitchImpl(
        checked = checked,
        onToggle = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        contentDescription = contentDescription,
    )
}

/**
 * 上游的开关,外面在整个开关上认「点一下」(公开反馈 #78「总功能开关开启和关闭 bug」)。
 * 上游开关只有圆钮接手指,点在开关空着的那一边收不到;按下后手指只要横着动一点就算拖动,没拖过一半松手就弹回原位。
 * 这里认点按:手指没挪出系统的防抖距离就松开。上游这一下没有切换时由这里切换。
 * 上游部件本身不动,样子、拖动、触感照旧;[onToggle] 只在真的要换成另一个状态时调用。
 * 应用里的开关都经过这里(检查:SwitchTapGuardTest)。
 */
@Composable
internal fun TapFriendlySwitch(
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    backdrop: Backdrop,
    enabled: Boolean = true,
    tapEnabled: Boolean = enabled,
) {
    val latestChecked by rememberUpdatedState(checked)
    val latestToggle by rememberUpdatedState(onToggle)
    val gesture = remember { SwitchGesture() }
    CupertinoSwitch(
        checked = checked,
        onCheckedChange = { want ->
            if (want != latestChecked) {
                gesture.upstreamToggled = true
                latestToggle(want)
            }
        },
        modifier = if (tapEnabled) Modifier.pointerInput(gesture) { recognizeTaps(gesture) { latestToggle(!latestChecked) } } else Modifier,
        enabled = enabled,
        backdrop = backdrop,
    )
}

/** 这一下按住期间上游有没有自己切换。 */
private class SwitchGesture {
    var upstreamToggled = false
}

/**
 * 只看不拦:按下、移动在最先一轮看,松手后等上游(圆钮在开关里面)在中间一轮处理完同一个松手,最后一轮再决定。
 * 手指一直没挪出防抖距离、上游又没切换,就调 [onTap]。
 */
private suspend fun PointerInputScope.recognizeTaps(gesture: SwitchGesture, onTap: () -> Unit) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    gesture.upstreamToggled = false
    val slop = viewConfiguration.touchSlop
    var tap = true
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
        if ((change.position - down.position).getDistance() > slop) tap = false
        if (!change.pressed) break
    }
    awaitPointerEvent(PointerEventPass.Final)
    if (tap && !gesture.upstreamToggled) onTap()
}

/**
 * 开关的实现。[compact] 为真时整体缩到八成,放在行里当小开关。
 * 不能拨时([onToggle] 为空)上面盖一层接住触摸的空层,点了交给 [onBlockedClick](例如没开通时弹开通说明)。
 */
@Composable
internal fun SwitchImpl(
    checked: Boolean,
    onToggle: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    contentDescription: String?,
    compact: Boolean = false,
    onBlockedClick: (() -> Unit)? = null,
) {
    val latestToggle by rememberUpdatedState(onToggle)
    val latestChecked by rememberUpdatedState(checked)
    val interactive = enabled && onToggle != null
    Box(
        modifier
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                if (contentDescription != null) this.contentDescription = contentDescription
                if (interactive) onClick { latestToggle?.invoke(!latestChecked); true }
            }
            .alpha(if (enabled) 1f else 0.4f)
            .scaledLayout(if (compact) 0.8f else 1f),
    ) {
        TapFriendlySwitch(
            checked = checked,
            onToggle = { want -> latestToggle?.invoke(want) },
            backdrop = astraGlassBackdrop(),
            tapEnabled = interactive,
        )
        if (!interactive) {
            Box(
                Modifier
                    .matchParentSize()
                    .then(
                        if (onBlockedClick != null) {
                            Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBlockedClick)
                        } else {
                            Modifier.pointerInput(Unit) {}
                        },
                    ),
            )
        }
    }
}

/**
 * 滑条:上游的液态玻璃滑条。[steps] 是两端之间的分档数(0 为连续)。
 * 读屏念 [contentDescription] 和 [stateText](行右边写的值);没给 [stateText] 时念所在位置的百分比。
 */
@Composable
fun AstraSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    contentDescription: String? = null,
    stateText: String? = null,
) {
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) this.contentDescription = contentDescription
                stateDescription = stateText ?: "${(fraction * 100).roundToInt()}%"
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        CupertinoSlider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            // 分档滑条不画下面一排小圆点
            showStepIndicator = false,
            backdrop = astraGlassBackdrop(),
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

/** 分段选择:上游的液态玻璃分段选择。 */
@Composable
fun AstraSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val selected = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    Box(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f)) {
        CupertinoSegmentedControl(
            selectedTabIndex = selected,
            modifier = Modifier.fillMaxWidth(),
            backdrop = astraPageBackdrop(),
            paddingValues = PaddingValues(0.dp),
        ) {
            options.forEachIndexed { index, label ->
                CupertinoSegmentedControlTab(
                    isSelected = index == selected,
                    onClick = { if (enabled && index != selected) onSelect(index) },
                ) {
                    CupertinoText(label, maxLines = 1)
                }
            }
        }
    }
}

/**
 * 圆形玻璃按钮(返回、使用说明、关闭):上游的液态玻璃圆按钮(按住折射、高光、投影都是上游的)。
 * 上游的圆按钮固定 48、图形 24,在星流的顶栏里偏大;这里按原来的 44 画,里面图形 18(用户 2026-10-23:按钮有些过大)。
 */
@Composable
fun AstraGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = astraGlassBackdrop(),
    enabled: Boolean = true,
) {
    // 不让上游按钮每 0.3 秒把背后的画面拍下来算亮暗(在主线程上取图,会掉帧);图形颜色跟着深浅色设置走。
    // 用上游的圆按钮(圆角与玻璃都画在整块按钮上);上游固定 48,这里压到 44,里面图形 18
    CupertinoLiquidIconButton(
        onClick = onClick,
        enabled = enabled,
        backdrop = backdrop,
        isBackgroundAdaptive = false,
        modifier = modifier.size(GlassButtonSize).semantics { this.contentDescription = contentDescription },
    ) {
        CupertinoIcon(icon, contentDescription = null, modifier = Modifier.size(GlassButtonGlyph))
    }
}

private val GlassButtonSize = 44.dp
private val GlassButtonGlyph = 18.dp

/** 按 [scale] 缩放整个部件,占的位置也跟着缩。 */
private fun Modifier.scaledLayout(scale: Float): Modifier = if (scale == 1f) this else layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    val width = (placeable.width * scale).roundToInt()
    val height = (placeable.height * scale).roundToInt()
    layout(width, height) {
        placeable.placeWithLayer((width - placeable.width) / 2, (height - placeable.height) / 2) {
            scaleX = scale
            scaleY = scale
        }
    }
}
