/*
 * 弹窗灰底（遮罩）的宿主：灰底画在页面窗口里，不由每个弹窗窗口自己画。
 *
 * 弹窗是独立窗口，创建窗口、画出第一帧都要等系统，旧的收起、新的出来之间必然有一段两个窗口都算"在"的时间。
 * 灰底画在弹窗窗口里时，「操作框 → 确认框」这种一个关一个开的交接就一定不连续：新窗口的灰底先压上去、
 * 旧窗口的灰底还在淡出，页面会先变深再回到原样（2026-10-23 设备实测：191→153→191，约 70–115ms，
 * 正好是两层 20% 灰底叠在一起）；两头都做淡入淡出的版本则是露出亮背景的空档，看起来像灰色消失又出现
 * （用户 2026-10-23 两次反馈的都是这一下）。灰底跟着窗口走就治不好，只有一个办法：不放在弹窗窗口里。
 * 改成页面窗口统一画一份：页面窗口一直在，弹出与交接都不会多一份也不会少一份，
 * 灰底只在最后一个弹窗关掉之后才淡出。
 */
package com.astraflow.Chizuru.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.delay
import zone.ien.hig.CupertinoLiquidDialogsDefaults
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.darkColorScheme
import zone.ien.hig.theme.lightColorScheme

/** 最后一个弹窗关掉后，灰底淡出用多久：比弹窗面板收起（120ms）稍长，灰底在弹窗完全收起之后才消失。 */
private const val ScrimFadeOutMillis = 220

/**
 * 页面窗口里的灰底宿主：记着此刻开着几个弹窗，关到 0 才把灰底收起来。
 * 由页面根组合（MainActivity）提供，各弹窗在开合时登记（见 AnimatedDialog）。
 */
@Stable
class AstraDialogScrimHost {
    /** 此刻开着的弹窗数。 */
    var openDialogs by mutableIntStateOf(0)
        private set

    internal fun dialogOpened() {
        openDialogs += 1
    }

    internal fun dialogClosed() {
        openDialogs = (openDialogs - 1).coerceAtLeast(0)
    }
}

/**
 * 页面窗口里的灰底宿主。没提供时（弹窗不在主界面的组合里）弹窗窗口退回自己画灰底。
 */
internal val LocalAstraDialogScrim =
    staticCompositionLocalOf<AstraDialogScrimHost?> { null }

/**
 * 弹窗开合时在页面窗口的灰底宿主上登记：开着就占一份，收起到 0 灰底才淡出。
 * 页面窗口里没有宿主时（弹窗不在主界面的组合里）什么也不做，由弹窗窗口自己画灰底。
 */
@Composable
internal fun rememberDialogScrimRegistration(visible: Boolean) {
    val host = LocalAstraDialogScrim.current
    DisposableEffect(host, host != null && visible) {
        if (host != null && visible) {
            host.dialogOpened()
            onDispose { host.dialogClosed() }
        } else {
            onDispose {}
        }
    }
}

/**
 * 页面窗口里的灰底：任一弹窗开着就画满（不做淡入，弹窗一出现灰底就已经在），
 * 最后一个弹窗关掉后再淡出。要放在页面里的弹层骨架（AstraSheet）之上、弹窗窗口之下：
 * 弹层开着再弹确认框时（例如倒计时事件里选日期）弹层也要被压暗，弹窗窗口是独立窗口天然在这层上面。
 *
 * @param dark 按界面的深浅色取灰底（和弹窗窗口里自己画时同一个颜色）。
 */
@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun AstraDialogScrimOverlay(dark: Boolean, modifier: Modifier = Modifier) {
    val host = LocalAstraDialogScrim.current ?: return
    // 这一层画在 AstraFlowTheme 外面（要盖住弹层骨架），拿不到应用的主题；起一个最小主题只为取
    // 上游的灰底色（CupertinoLiquidDialogsDefaults.ScrimColor：浅色黑 20%、深色黑 40%），颜色仍由上游决定。
    CupertinoTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        DialogScrimLayer(host, modifier)
    }
}

/** 计数清零后先等这么久再开始淡出：这段时间里又有弹窗打开（「操作框 → 确认框」交接）就不淡出。 */
private const val ScrimHandoffGraceMillis = 160

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun DialogScrimLayer(host: AstraDialogScrimHost, modifier: Modifier) {
    val scrimColor = CupertinoLiquidDialogsDefaults.ScrimColor
    val isOpen = host.openDialogs > 0
    // 关闭时的淡出；打开时直接是满的（万一上一帧还在淡出，重新打开也立刻回到满）
    val fade = remember { Animatable(0f) }
    LaunchedEffect(isOpen) {
        if (isOpen) {
            fade.snapTo(1f)
        } else if (fade.value > 0f) {
            // 交接宽限：旧弹窗收起、新弹窗打开几乎同帧时，计数会先跌到 0 再回 1。
            // 立刻按 0 淡出的话，头几帧已经画出去，新弹窗一开又瞬间回满——看起来就是灰底闪一下
            //(星流 2026-10-23 同款修复)。宽限期内重新打开，上面的 snapTo(1f) 会取消这里的等待，
            // 灰底全程不动。
            delay(ScrimHandoffGraceMillis.toLong())
            if (host.openDialogs == 0) {
                fade.animateTo(0f, tween(ScrimFadeOutMillis, easing = LinearEasing))
            }
        }
    }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawBehind {
                    if (isOpen) {
                        drawRect(scrimColor)
                    } else {
                        // 读在画图阶段：淡出只重画这一层，不重组页面
                        val fading = fade.value
                        if (fading > 0f) {
                            drawRect(scrimColor.copy(alpha = scrimColor.alpha * fading))
                        }
                    }
                },
    )
}
