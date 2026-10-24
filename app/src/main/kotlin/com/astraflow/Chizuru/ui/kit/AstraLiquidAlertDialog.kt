/*
 * Copyright (c) 2023-2024. Compose Cupertino project and open source contributors.
 * Copyright (c) 2025. Scott Lanoue.
 * Copyright (c) 2026. IENGROUND of IENLAB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modified by AstraFlow: keep the upstream 27.0.1 dialog look (300dp wide, 34dp
// corner, translucent glass panel, pill action buttons) while preserving AstraFlow's
// original action semantics: pressing an action calls the caller's callback and the
// caller decides when the dialog closes. Upstream 27.0.1 closes the dialog before
// running the action, which would break 46 existing call sites (upload progress,
// update download, inline validation). The body slot keeps the AstraFlow full-bleed
// option for content that has to touch the dialog edges.
// Source: zone.ien.hig:hig-android:27.0.1-alpha05, CupertinoDialogs.kt /
// CupertinoLiquidDialogs.kt (Apache-2.0).
package com.astraflow.Chizuru.ui.kit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.kyant.backdrop.Backdrop
import com.astraflow.Chizuru.ui.theme.LocalEnableBlur
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight
import zone.ien.hig.AlertActionStyle
import zone.ien.hig.AlertDialogActionsScope
import zone.ien.hig.CupertinoLiquidDialogsDefaults
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.LocalContentColor
import zone.ien.hig.LocalTextStyle
import zone.ien.hig.ProvideTextStyle
import zone.ien.hig.theme.CupertinoColors
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.systemRed

private val DialogMaxWidth = 300.dp
private val DialogMinHeight = 110.dp
private val DialogOuterPadding = 14.dp
private val DialogHorizontalPadding = 22.dp
private val HeaderBottomPadding = 24.dp
private val TitleMessageSpacing = 10.dp
private val ButtonMinHeight = 48.dp
private val ButtonSpacing = 8.dp
private val DialogBlurRadius = 30.dp

/**
 * AstraFlow 的液态玻璃确认框：外观照上游 27.0.1（宽 300、圆角 34、半透明玻璃面板、
 * 胶囊按钮），动作语义仍是星流原来的——点按钮只调回调，关不关由调用方决定。
 *
 * @param body 正文下面的内容（输入框、出错提示、开关行等）。[contentFullBleed] 为真时
 * 内容左右不留白，由内容自己留 22；否则统一留 22。
 */
@Composable
@ExperimentalCupertinoApi
internal fun AstraLiquidAlertDialog(
    visible: Boolean = true,
    onDismissRequest: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    message: (@Composable () -> Unit)? = null,
    body: (@Composable ColumnScope.() -> Unit)? = null,
    containerColor: Color = CupertinoLiquidDialogsDefaults.ContainerColor,
    shape: Shape = CupertinoLiquidDialogsDefaults.Shape,
    shadowElevation: Dp = 1.dp,
    properties: DialogProperties = DialogProperties(),
    backdrop: Backdrop,
    buttonsOrientation: Orientation = CupertinoLiquidDialogsDefaults.ButtonOrientation,
    contentFullBleed: Boolean = false,
    buttons: AlertDialogActionsScope.() -> Unit,
) {
    AnimatedDialog(
        visible = visible,
        properties = properties,
        onDismissRequest = onDismissRequest,
        enterTransition = scaleIn(initialScale = 0.94f) + fadeIn(tween(180)),
        exitTransition = scaleOut(targetScale = 0.98f, animationSpec = tween(120)) + fadeOut(tween(120)),
    ) {
        val isDark = CupertinoTheme.colorScheme.isDark
        // 只有拿到页面那层录好的画面时弹窗才需要做玻璃模糊;没有(弹窗挂在主界面外层)时
        // drawBackdrop 背后是空的,再开 30dp 模糊只是每帧白白多一次离屏层 + 模糊(开合掉帧)
        val blursPageBackdrop = LocalAstraBackdrop.current != null && LocalEnableBlur.current
        // 弹窗底下这一层不透明:背景页面的文字不再透出来和弹窗文字叠在一起(用户 2026-10-23)
        val panelBase = if (isDark) Color(0xFF1E1E21) else Color(0xFFF5F5F7)
        val border = if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.16f)
        val highlight = Color.White.copy(alpha = if (isDark) 0.42f else 0.9f)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 40.dp)
                .widthIn(max = DialogMaxWidth)
                .fillMaxWidth()
                .heightIn(min = DialogMinHeight)
                .clip(shape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = { if (blursPageBackdrop) blur(DialogBlurRadius.toPx()) },
                    highlight = { Highlight.Default.copy(alpha = 0.35f) },
                    onDrawSurface = {
                        drawRect(panelBase)
                        drawRect(containerColor)
                    },
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(highlight, border, border)),
                    shape = shape,
                ),
        ) {
            if (title != null || message != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = DialogHorizontalPadding, end = DialogHorizontalPadding, top = DialogHorizontalPadding, bottom = HeaderBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(TitleMessageSpacing),
                    horizontalAlignment = Alignment.Start,
                ) {
                    title?.let {
                        ProvideTextStyle(
                            value = CupertinoTheme.typography.headline.copy(textAlign = TextAlign.Start),
                            content = it,
                        )
                    }
                    message?.let {
                        ProvideTextStyle(
                            value = CupertinoTheme.typography.body.copy(
                                color = CupertinoTheme.colorScheme.secondaryLabel,
                                textAlign = TextAlign.Start,
                            ),
                            content = it,
                        )
                    }
                }
            }
            if (body != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (contentFullBleed) 0.dp else DialogHorizontalPadding),
                    content = body,
                )
            }
            val scope = AstraLiquidAlertDialogButtonsScopeImpl(buttonsOrientation).apply(buttons)
            scope.Content(hasHeader = title != null || message != null, hasBody = body != null)
        }
    }
}

@Composable
private fun AnimatedDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    scrimColor: Color = CupertinoLiquidDialogsDefaults.ScrimColor,
    content: @Composable BoxScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // 收起动画播完才拿掉窗口;一开始就关着的不建窗口(用户 2026-10-23:换另一个弹窗时没有过渡动画)
    // 初值必须是 false:用 visible(true) 初始化时 MutableTransitionState 一来就是「已经在」,
    // AnimatedVisibility 不会播进入动画——弹窗是直接蹦出来的(用户 2026-10-23:点击「重启作用域」出来的
    // 确认框没有任何过渡动画)。从 false 起、组合时把 target 设成 true,进/出动画才都播得出来。
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = visible
    if (!visibility.currentState && visibility.isIdle) return

    // 灰底交给页面窗口画(LocalAstraDialogScrim + AstraDialogScrimOverlay):灰底不跟着窗口走,
    // 两个弹窗交接时不会叠成两层、也不会露出亮背景,全程只有一份(用户 2026-10-23)。
    // 没有页面窗口时(不在主界面里)才退回窗口自己画。
    val scrimHost = LocalAstraDialogScrim.current
    rememberDialogScrimRegistration(visible)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                // 弹窗自己画遮罩:窗口做成边到边,遮罩才能盖住状态栏与手势条
                decorFitsSystemWindows = false,
                dismissOnBackPress = properties.dismissOnBackPress,
                dismissOnClickOutside = properties.dismissOnClickOutside,
                usePlatformDefaultWidth = false,
            ),
    ) {
        // 弹窗自成一个窗口:透明窗口底色、去掉系统自带压暗,状态栏与手势条里的图标跟着深浅色
        val dialogView = LocalView.current
        val isDarkTheme = CupertinoTheme.colorScheme.isDark
        SideEffect {
            val window = dialogView.dialogWindow() ?: return@SideEffect
            // 主题里已经设成整屏透明;这里再兜一次,免得某些系统主题或重建后又被按系统栏收回去
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setDimAmount(0f)
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
        // 返回手势也走 visible 状态:平台弹窗的 cancel() 是空实现,但有的系统/手势导航会把窗口直接收掉,
        // 窗口一收就播不了收起动画(用户 2026-10-23:用手势退出弹窗时没有收起动画)。这里自己占住返回,
        // 关弹窗仍然先播动画、播完才拿掉窗口。
        BackHandler(enabled = properties.dismissOnBackPress && visible) { onDismissRequest() }
        CompositionLocalProvider(LocalHapticFeedback provides haptic) {
            // 有页面窗口时灰底由页面窗口画(上面的 scrimHost),窗口本身全透明:交接时灰底不跟着窗口走。
            // 没有页面窗口时退回原来的画法:打开即满、收起时淡出。
            val drawsScrimInWindow = scrimHost == null
            val scrimFade = remember { Animatable(1f) }
            LaunchedEffect(visible) {
                if (!visible) scrimFade.animateTo(0f, tween(120))
            }
            val scrimColorShown =
                when {
                    !drawsScrimInWindow -> Color.Transparent
                    visible -> scrimColor
                    else -> scrimColor.copy(alpha = scrimColor.alpha * scrimFade.value)
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawRect(scrimColorShown)
                            drawContent()
                        }.then(
                            if (properties.dismissOnClickOutside && visible) {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures {
                                        onDismissRequest()
                                    }
                                }
                            } else {
                                Modifier
                            },
                        )
                        .imePadding(),
            ) {
                // 缩放与淡入淡出只作用在面板这一块:以前整屏（1272×2800）都进一个离屏层再缩放,
                // 弹窗开合期间每帧多合成一整屏,165Hz 上很容易掉帧。面板居中,观感不变。
                AnimatedVisibility(
                    visibleState = visibility,
                    enter = enterTransition,
                    exit = exitTransition,
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Box(content = content)
                }
            }
        }
    }
}

/** 弹窗内容所在的视图链里挂着它自己的窗口(Compose 的 DialogLayout 实现 DialogWindowProvider)。 */
private fun android.view.View.dialogWindow(): android.view.Window? {
    var current: android.view.View? = this
    while (current != null) {
        val provider = current as? DialogWindowProvider
        if (provider != null) return provider.window
        current = current.parent as? android.view.View
    }
    return null
}

private class AstraLiquidAlertDialogButtonsScopeImpl(
    private val orientation: Orientation,
): AlertDialogActionsScope {
    private val buttons = mutableListOf<Pair<AlertActionStyle, @Composable (isPrimary: Boolean) -> Unit>>()

    override fun action(
        onClick: () -> Unit,
        style: AlertActionStyle,
        enabled: Boolean,
        title: @Composable () -> Unit,
    ) {
        buttons.add(style to { isPrimary -> ActionButton(style, enabled, isPrimary, title, onClick) })
    }

    @Composable
    private fun ActionButton(
        style: AlertActionStyle,
        enabled: Boolean,
        isPrimary: Boolean,
        title: @Composable () -> Unit,
        onClick: () -> Unit,
    ) {
        val colorScheme = CupertinoTheme.colorScheme
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        )
        val accent = colorScheme.accent
        val highlighted = isPrimary && style == AlertActionStyle.Default
        val buttonBackground =
            if (highlighted) accent else colorScheme.tertiarySystemFill
        val buttonContentColor =
            when {
                !enabled -> colorScheme.tertiaryLabel
                style == AlertActionStyle.Destructive -> CupertinoColors.systemRed(colorScheme.isDark)
                highlighted -> if (accent.luminance() > 0.5f) Color.Black else Color.White
                else -> colorScheme.label
            }
        val animatedBackground by animateColorAsState(
            targetValue = if (isPressed && highlighted) accent.copy(alpha = 0.86f) else buttonBackground,
        )
        Box(
            modifier = Modifier
                .heightIn(min = ButtonMinHeight)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
            propagateMinConstraints = true,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonMinHeight)
                    .clip(CircleShape)
                    .background(animatedBackground)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                ProvideTextStyle(
                    CupertinoTheme.typography.body.copy(
                        color = buttonContentColor,
                        fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    ),
                ) {
                    CompositionLocalProvider(LocalContentColor provides LocalTextStyle.current.color) {
                        title()
                    }
                }
            }
        }
    }

    @Composable
    fun Content(hasHeader: Boolean, hasBody: Boolean) {
        if (buttons.isEmpty()) return
        val ordered =
            if (orientation == Orientation.Horizontal) {
                buttons.filter { it.first == AlertActionStyle.Cancel } +
                    buttons.filter { it.first != AlertActionStyle.Cancel }
            } else {
                buttons.filter { it.first != AlertActionStyle.Cancel } +
                    buttons.filter { it.first == AlertActionStyle.Cancel }
            }
        // 只有一两个按钮的确认框才把默认动作画成主题色胶囊(确定/知道了);三个以上或竖排(维护菜单、
        // 有第二动作的确认框)不突出某一个,免得把次要动作画成主按钮。
        val primaryIndex =
            if (orientation == Orientation.Horizontal && ordered.size <= 2) {
                ordered.indexOfLast { it.first == AlertActionStyle.Default }
            } else {
                -1
            }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DialogOuterPadding, end = DialogOuterPadding, bottom = DialogOuterPadding)
                // 只有标题时,标题自己的下留白已经隔开按钮;有正文或没有标题时,按钮上方要留出间距(输入框底部不再贴住按钮)
                .then(if (hasHeader && !hasBody) Modifier else Modifier.padding(top = DialogOuterPadding)),
        ) {
            if (orientation == Orientation.Horizontal && ordered.size <= 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonSpacing)) {
                    ordered.forEachIndexed { index, (_, content) ->
                        Box(Modifier.weight(1f)) { content(index == primaryIndex) }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(ButtonSpacing)) {
                    ordered.forEachIndexed { index, (_, content) ->
                        content(index == primaryIndex)
                    }
                }
            }
        }
    }
}
