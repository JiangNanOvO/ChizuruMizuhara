@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.kit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astraflow.Chizuru.ui.kit.icons.AstraGlyphs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import zone.ien.hig.CupertinoActionSheet
import zone.ien.hig.CupertinoActivityIndicator
import zone.ien.hig.CupertinoText
import zone.ien.hig.section.CupertinoSection
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.cancel
import zone.ien.hig.default
import zone.ien.hig.AlertActionStyle
import zone.ien.hig.theme.CupertinoTheme

/*
 * 确认框、操作菜单、加载转圈直接用上游 compose-hig 的(液态玻璃确认框、玻璃菜单、iOS 的菊花转圈)。
 * 上游没有能单独弹出的底部弹层,底部弹层仍是星流自己的,里面的按钮和文字用上游的。
 */


/**
 * 确认框:上游的液态玻璃确认框。粗体标题和正文,下方按钮(取消在左、确定在右,有三个按钮时竖排:确定、第二个、取消)。
 * 改不回来的操作(删除、清理、注销)把 [destructive] 设为真,确定按钮变红。
 * [confirmEnabled] 为假时确定按钮变灰(例如还没填完、正在提交)。[content] 放在正文下面(输入框、出错提示等)。
 * [contentFullBleed] 为真时正文下面这块内容铺满弹窗左右边缘(按下时的背景能顶到边),内容自己留左右 22;
 * 标题和正文照旧留 22。
 * [dismissible] 为假时点框外和返回键都不关闭(例如必须更新)。点确定有触感。
 */
@Composable
fun AstraAlert(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    message: String? = null,
    dismissText: String? = "取消",
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    dismissible: Boolean = true,
    contentFullBleed: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val haptics = rememberAstraHaptics()
    val latestConfirm by rememberUpdatedState(onConfirm)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val latestSecondary by rememberUpdatedState(onSecondary)
    val messageMaxHeight = (LocalConfiguration.current.screenHeightDp - 280).coerceAtLeast(80).dp
    AstraLiquidAlertDialog(
        visible = visible,
        onDismissRequest = { if (dismissible) latestDismiss() },
        title = { CupertinoText(title) },
        message = message?.let { text -> { CupertinoText(text) } },
        // 内容(输入框、出错提示、开关行)放在正文下面;长内容在框内滚动
        body = content?.let { block ->
            {
                Column(
                    Modifier.heightIn(max = messageMaxHeight).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = block,
                )
            }
        },
        properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible),
        buttonsOrientation = if (secondaryText != null) Orientation.Vertical else Orientation.Horizontal,
        contentFullBleed = contentFullBleed,
    ) {
        val confirm = {
            haptics.confirm()
            latestConfirm()
        }
        if (secondaryText != null) {
            if (destructive) action(confirm, AlertActionStyle.Destructive, confirmEnabled) { CupertinoText(confirmText) }
            else default(confirm, confirmEnabled) { CupertinoText(confirmText) }
            default({ latestSecondary?.invoke() }) { CupertinoText(secondaryText) }
            if (dismissText != null) cancel({ latestDismiss() }) { CupertinoText(dismissText) }
        } else {
            if (dismissText != null) cancel({ latestDismiss() }) { CupertinoText(dismissText) }
            if (destructive) action(confirm, AlertActionStyle.Destructive, confirmEnabled) { CupertinoText(confirmText) }
            else default(confirm, confirmEnabled) { CupertinoText(confirmText) }
        }
    }
}

/** 弹窗里的出错提示:红色小字,左对齐。 */
@Composable
fun AstraErrorText(text: String, modifier: Modifier = Modifier) {
    AstraText(text, modifier = modifier.fillMaxWidth(), style = Astra.type.footnote, color = Astra.colors.danger)
}

/**
 * 记住上一次不为空的参数:确认框、弹层收起动画播完前内容先留着,不因为调用方把状态清空而空白或闪一下。
 * 调用方据此把弹窗常驻(传 visible),而不是在状态为空时整个不画,这样收起才有动画。
 */
@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    val last = remember { mutableStateOf<T?>(null) }
    SideEffect { if (value != null) last.value = value }
    return value ?: last.value
}

/** 操作菜单里的一项:[selected] 为真时前面打勾;[sectionStart] 为真时上方加一条分隔线。 */
class AstraMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val sectionStart: Boolean = false,
    val destructive: Boolean = false,
)

/**
 * 操作选择框:和星流维护按钮用的是同一个部件(上游的 Action Sheet),从屏幕底部滑上来、滑下去。
 * 每个操作一块玻璃面板、按下去缩一下,最后一块是「取消」;面板外面点一下收起,返回键也收起。
 * [title]/[message] 写在最上面那块的中缝。
 * 选中一项后先收起本框,滑落动画播完(onGone)再执行那一项,不在同一时间叠两份绘制。
 */
@Composable
fun AstraQuickActionsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    items: List<AstraMenuItem>,
    title: String? = null,
    message: String? = null,
    cancelText: String = "取消",
) {
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(visible) {
        if (!visible) {
            pendingAction?.invoke()
            pendingAction = null
        }
    }
    val latestItems by rememberUpdatedState(items)
    val haptics = rememberAstraHaptics()
    CupertinoActionSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        title = title?.let { text -> { CupertinoText(text) } },
        message = message?.let { text -> { CupertinoText(text) } },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        // 和页面上其它浮层一样,面板取页面录下的画面做玻璃(不传就退化成不透明底色)。
        containerColor = CupertinoTheme.colorScheme.tertiarySystemBackground,
    ) {
        latestItems.forEach { item ->
            action(
                onClick = {
                    haptics.confirm()
                    pendingAction = item.onClick
                    onDismiss()
                },
                style = if (item.destructive) AlertActionStyle.Destructive else AlertActionStyle.Default,
                enabled = item.enabled,
            ) {
                CupertinoText(item.label, modifier = Modifier.astraSearchTarget(item.label))
            }
        }
        cancel(onClick = onDismiss) {
            CupertinoText(cancelText)
        }
    }
}

/**
 * 操作菜单:点右上角按钮后从底部弹出的列表(上游的弹层与分组),标题写 [title];几组分开放(例如排序方式一组、全选反选一组),
 * 选项一组里当前那项右边打勾,动作用主题色字;点一项后收起。
 */
@Composable
fun AstraActionMenu(expanded: Boolean, onDismiss: () -> Unit, title: String, items: List<AstraMenuItem>) {
    val groups = items.fold(mutableListOf<MutableList<AstraMenuItem>>()) { acc, item ->
        if (acc.isEmpty() || item.sectionStart) acc += mutableListOf(item) else acc.last() += item
        acc
    }
    AstraSheet(visible = expanded, onDismiss = onDismiss, title = title) {
        groups.forEach { group ->
            val picker = group.any { it.selected }
            CupertinoSection {
                group.forEach { item ->
                    CheckRow(item.label, selected = item.selected, enabled = item.enabled, accent = !picker) {
                        onDismiss()
                        item.onClick()
                    }
                }
            }
        }
    }
}

/** 加载中的转圈加一行字,放在页面中间。 */
@Composable
fun AstraLoading(text: String, modifier: Modifier = Modifier) {
    val colors = Astra.colors
    Column(modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AstraSpinner()
        AstraText(text, style = Astra.type.subheadline, color = colors.labelSecondary)
    }
}

/** 转圈:上游的 iOS 菊花转圈。 */
@Composable
fun AstraSpinner(modifier: Modifier = Modifier, size: Dp = 28.dp, color: Color = Astra.colors.labelSecondary) {
    CupertinoActivityIndicator(modifier = modifier, size = size, color = color)
}
