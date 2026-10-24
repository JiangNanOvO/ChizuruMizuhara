@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.kit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.astraflow.Chizuru.ui.kit.icons.AstraGlyphs
import zone.ien.hig.CupertinoIcon
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import zone.ien.hig.CupertinoSegmentedControl
import zone.ien.hig.CupertinoSegmentedControlTab
import zone.ien.hig.CupertinoSlider
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.section.CupertinoSection
import zone.ien.hig.section.CupertinoSectionDefaults
import zone.ien.hig.section.LazySectionScope
import zone.ien.hig.section.SectionItem
import zone.ien.hig.section.SectionLink
import zone.ien.hig.section.SectionScope
import zone.ien.hig.section.section
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.utils.rememberDefaultBackdrop

/*
 * 页面的分组列表:原样用上游 compose-hig 的分组写法(LazyColumn 里的 section {}),写法照上游示例。
 * 下面的行都是上游的行(打开下一页的行、开关、选项菜单、滑条、分段选择)放进上游分组里,样子、大小、留白全是上游的;
 * 只多接两样星流的东西:没开通的星尘设置([locked],和上游一样变灰不能动,打开下一页的行照常能点),
 * 从搜索跳来时找到这一行([astraSearchTarget])。上游有现成写法的(例如 link、textField、item)页面也可以直接用;开关一律用这里的([astraSwitch]、[astraSwitchRow]),点按由 TapFriendlySwitch 认。
 */

/** 上游的分组行要在「分组」里面写;借这一个调用上游的行。 */
private object HigRows : SectionScope

/**
 * 上游的分组:上方灰色组标题([title])。分组下面不写说明:页面底色上不放说明文字,
 * 设置的作用、连带关系与例外写在页面右上角的使用说明里(docs/design/ui-standard.md 第二部分)。
 */
fun LazyListScope.astraSection(
    title: String? = null,
    searchTarget: String? = null,
    content: LazySectionScope.() -> Unit,
) = section(
    title = title?.let { {
        CupertinoText(it, modifier = if (searchTarget != null) Modifier.astraSearchTarget(searchTarget) else Modifier)
    } },
    content = content,
)

/**
 * 单独一张分组卡:用在懒加载列表的 item 里,不带上下的分组间距,由调用方用 [modifier] 控制;
 * 左右留白与普通分组卡一致,里面仍是上游的分组行。首页的「店铺」卡用它,避免两张相邻分组卡
 * 各自带上下间距导致与公告卡的间距不一致(2026-10-19)。
 */
@Composable
fun AstraStandaloneLinkCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CupertinoSection(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
    ) {
        SectionLink(onClick = onClick) { CupertinoText(title) }
    }
}

/** 行左边的方块图标:与功能入口使用相同的浅色、深色样式,尺寸保持为上游的 32。 */
@Composable
fun AstraLinkIcon(icon: ImageVector, color: Color) = AstraIconTile(icon, color, 32.dp)

private fun iconOf(icon: ImageVector?, color: Color?): (@Composable () -> Unit)? =
    if (icon == null) null else ({ AstraLinkIcon(icon, color ?: CupertinoTheme.colorScheme.accent) })

private fun dividerFor(icon: Any?): Dp =
    if (icon != null) CupertinoSectionDefaults.DividerPaddingWithIcon else CupertinoSectionDefaults.DividerPadding

/** 行的标题;[summary] 不为空时标题下面一行灰色小字。 */
@Composable
private fun RowTitle(title: String, summary: String?, badge: String? = null, color: Color = Color.Unspecified) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CupertinoText(title, color = color)
            if (badge != null) AstraBadge(badge)
        }
        if (!summary.isNullOrEmpty()) {
            CupertinoText(summary, style = CupertinoTheme.typography.footnote, color = CupertinoTheme.colorScheme.secondaryLabel)
        }
    }
}

/**
 * 打开另一页的行:上游的 link 行,右边「›」,箭头前可显示当前值([value])。
 * 左边可以是彩色小方块图标([icon]),也可以是别的([leading],例如头像);[leading] 比小方块宽时,
 * 用 [leadingWidth] 告诉分隔线从文字下面开始。
 */
fun LazySectionScope.astraLink(
    title: String,
    onClick: () -> Unit,
    key: Any? = null,
    summary: String? = null,
    value: String? = null,
    valueDanger: Boolean = false,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    badge: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    leadingWidth: Dp? = null,
) = item(
    key = key,
    // 上游的行:左留白、左边的图、图与文字之间各一份行内留白
    dividerPadding = leadingWidth?.let { CupertinoSectionDefaults.DividerPadding * 2 + it } ?: dividerFor(leading ?: icon),
) {
    with(HigRows) {
        SectionLink(
            onClick = onClick,
            modifier = Modifier.astraSearchTarget(title),
            enabled = enabled,
            icon = leading ?: iconOf(icon, iconColor) ?: {},
            caption = { if (value != null) CupertinoText(value, maxLines = 1, color = if (valueDanger) Astra.colors.danger else Color.Unspecified) },
            title = { RowTitle(title, summary, badge) },
        )
    }
}

/**
 * 一行动作文字(恢复默认、检查更新这类):上游的 link 行去掉箭头,字用主题色;删除、注销这类用红色([destructive]);不能点时灰字。
 * [value] 写在右边(例如此刻不能用的原因)。
 */
fun LazySectionScope.astraAction(
    title: String,
    onClick: () -> Unit,
    key: Any? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    value: String? = null,
    showChevron: Boolean = false,
) = item(key = key) {
    with(HigRows) {
        SectionLink(
            onClick = onClick,
            modifier = Modifier.astraSearchTarget(title),
            enabled = enabled,
            caption = { if (value != null) CupertinoText(value, maxLines = 1) },
            chevron = if (showChevron) ({ CupertinoSectionDefaults.LabelChevron() }) else ({}),
            title = {
                CupertinoText(
                    title,
                    color = when {
                        // 不能点时和上游不能点的行一样用灰字
                        !enabled -> CupertinoTheme.colorScheme.secondaryLabel
                        destructive -> Astra.colors.danger
                        else -> CupertinoTheme.colorScheme.accent
                    },
                )
            },
        )
    }
}

/** 只显示信息的行:上游的行,左标题,右边灰色的值。 */
fun LazySectionScope.astraValue(
    title: String,
    value: String?,
    key: Any? = null,
    summary: String? = null,
    icon: ImageVector? = null,
    iconColor: Color? = null,
) = item(key = key, dividerPadding = dividerFor(icon)) {
    with(HigRows) {
        SectionItem(
            modifier = Modifier.astraSearchTarget(title),
            leadingContent = iconOf(icon, iconColor) ?: {},
            trailingContent = { if (value != null) CupertinoText(value, maxLines = 1, color = CupertinoTheme.colorScheme.secondaryLabel) },
            title = { RowTitle(title, summary) },
        )
    }
}

/**
 * 开关行:上游的行,右边上游的开关。和上游一样点开关切换。
 * [locked] 为真时是没开通的星尘设置:开关照常显示真实状态,拨了交给 [onLockedClick](弹开通说明)。
 * 页面没有跟着改状态时(没开通、被拦下),开关弹回真实状态。
 */
fun LazySectionScope.astraSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    key: Any? = null,
    summary: String? = null,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    badge: String? = null,
    enabled: Boolean = true,
    locked: Boolean = false,
    onLockedClick: (() -> Unit)? = null,
    titleColor: Color = Color.Unspecified,
) = item(key = key, dividerPadding = dividerFor(icon)) {
    with(HigRows) {
        SectionItem(
            modifier = Modifier.astraSearchTarget(title),
            leadingContent = iconOf(icon, iconColor) ?: {},
            trailingContent = {
                SnapBackSwitch(checked, enabled) { want ->
                    if (locked) onLockedClick?.invoke() else onCheckedChange(want)
                }
            },
            title = { RowTitle(title, summary, badge, titleColor) },
        )
    }
}

/**
 * 上游的开关(点按由 [TapFriendlySwitch] 认);拨了以后页面没有改状态时(两帧后还是原来的值),
 * 重新放一个开关,弹回真实状态。
 */
@Composable
internal fun SnapBackSwitch(checked: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var reset by remember { mutableIntStateOf(0) }
    val latestChecked by rememberUpdatedState(checked)
    key(reset) {
        TapFriendlySwitch(
            checked = checked,
            onToggle = { want ->
                onToggle(want)
                scope.launch {
                    withFrameNanos { }
                    withFrameNanos { }
                    if (latestChecked != want) reset++
                }
            },
            backdrop = rememberDefaultBackdrop(),
            enabled = enabled,
        )
    }
}

/**
 * 开关行,左边的图和名字由页面自己画(应用图标、名字下面写包名这类行用)。开关和 [astraSwitch] 是同一个。
 */
fun LazySectionScope.astraSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    key: Any? = null,
    enabled: Boolean = true,
    dividerPadding: Dp = CupertinoSectionDefaults.DividerPadding,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
) = item(key = key, dividerPadding = dividerPadding) {
    with(HigRows) {
        SectionItem(
            leadingContent = icon ?: {},
            trailingContent = { SnapBackSwitch(checked, enabled, onCheckedChange) },
            title = title,
        )
    }
}

/** 选项在这个数以内时直接放一排格子,多了点开从底部弹出列表。 */
private const val SEGMENTED_MAX_OPTIONS = 3

/**
 * 从几项里选一项。两三项:标题下面一排格子(上游的分段选择),点哪格选哪格;
 * 四项以上:上游的打开下一页的行,右边写当前选项,点开从底部弹出列表(上游的弹层与分组),当前那项右边打勾,点一项就收起。
 * 给了 [preview](主题这类要边看边选的):不论几项都弹出列表,试播画面固定在列表上方,点一项就换上,列表不收起,点「完成」收起。
 * [locked] 为真时点了交给 [onLockedClick](弹开通说明)。[lockedOptions] 里的选项(例如星尘没开通时的「动态背景」)点了不选,
 * 也交给 [onLockedClick];在弹出列表里点的,列表先收起,往下让开后再交给它(弹层让开后才打开下一层)。
 * [forceSheet] 为真时即使只有两三项也使用底部选项列表,适合文字较长的选项。
 */
fun LazySectionScope.astraPicker(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    key: Any? = null,
    summary: String? = null,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    badge: String? = null,
    enabled: Boolean = true,
    locked: Boolean = false,
    onLockedClick: (() -> Unit)? = null,
    lockedOptions: Set<Int> = emptySet(),
    forceSheet: Boolean = false,
) = if (!forceSheet && options.size in 2..SEGMENTED_MAX_OPTIONS) {
    item(key = key, dividerPadding = dividerFor(icon)) { padding ->
        Column(
            Modifier.astraSearchTarget(title).fillMaxWidth().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CupertinoSectionDefaults.DividerPadding)) {
                iconOf(icon, iconColor)?.invoke()
                RowTitle(title, summary, badge)
            }
            Segments(options, selectedIndex, enabled) { index -> if (locked || index in lockedOptions) onLockedClick?.invoke() else onSelect(index) }
        }
    }
} else {
    item(key = key, dividerPadding = dividerFor(icon)) {
        var open by remember { mutableStateOf(false) }
        // 点了锁着的选项:先收起列表,等它往下让开再弹开通说明;重新打开列表时清掉还没弹的
        var afterSheet by remember { mutableStateOf<(() -> Unit)?>(null) }
        with(HigRows) {
            SectionLink(
                onClick = {
                    if (locked) {
                        onLockedClick?.invoke()
                    } else {
                        afterSheet = null
                        open = true
                    }
                },
                modifier = Modifier.astraSearchTarget(title),
                enabled = enabled,
                icon = iconOf(icon, iconColor) ?: {},
                caption = { CupertinoText(options.getOrNull(selectedIndex) ?: "", maxLines = 1) },
                title = { RowTitle(title, summary, badge) },
            )
        }
        AstraSheet(
            visible = open,
            onDismiss = { open = false },
            title = title,
            onHidden = {
                afterSheet?.invoke()
                afterSheet = null
            },
        ) {
            CupertinoSection {
                options.forEachIndexed { index, label ->
                    CheckRow(label, selected = index == selectedIndex) {
                        if (index in lockedOptions) {
                            afterSheet = onLockedClick
                            open = false
                        } else {
                            open = false
                            if (index != selectedIndex) onSelect(index)
                        }
                    }
                }
            }
        }
    }
}

/** 一排格子:上游的分段选择;不能用时整排变淡,点了不选。 */
@Composable
private fun Segments(options: List<String>, selectedIndex: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    CupertinoSegmentedControl(
        selectedTabIndex = selectedIndex,
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
        paddingValues = PaddingValues(0.dp),
    ) {
        options.forEachIndexed { index, label ->
            CupertinoSegmentedControlTab(
                isSelected = index == selectedIndex,
                onClick = { if (enabled && index != selectedIndex) onSelect(index) },
            ) { CupertinoText(label, maxLines = 1) }
        }
    }
}

/** 底部弹出列表里的一行:上游的行,当前选中的那项右边打勾;[accent] 为真时字用主题色(动作)。 */
@Composable
internal fun SectionScope.CheckRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    SectionLink(
        onClick = onClick,
        enabled = enabled,
        chevron = {
            if (selected) CupertinoIcon(AstraGlyphs.Check, contentDescription = null, modifier = Modifier.size(17.dp), tint = CupertinoTheme.colorScheme.accent)
        },
    ) {
        CupertinoText(
            label,
            color = when {
                !enabled -> CupertinoTheme.colorScheme.secondaryLabel
                accent -> CupertinoTheme.colorScheme.accent
                else -> Color.Unspecified
            },
        )
    }
}

/**
 * 调数值:上游的行里放标题与当前值,下面上游的液态玻璃滑条。[steps] 是两端之间的分档数(0 为连续)。
 * [locked] 为真时滑条变灰拖不动,点这一行交给 [onLockedClick](弹开通说明)。
 */
fun LazySectionScope.astraSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueText: String,
    key: Any? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    summary: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
    locked: Boolean = false,
    onLockedClick: (() -> Unit)? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) = item(key = key) { padding ->
    Column(
        Modifier
            .astraSearchTarget(title)
            .fillMaxWidth()
            .then(if (locked && onLockedClick != null) Modifier.clickable(onClick = onLockedClick) else Modifier)
            .padding(padding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { RowTitle(title, summary, badge) }
            CupertinoText(valueText, maxLines = 1, style = CupertinoTheme.typography.subhead, color = CupertinoTheme.colorScheme.secondaryLabel)
        }
        CupertinoSlider(
            value = value,
            onValueChange = onValueChange,
            // 读屏念右边写的值
            modifier = Modifier.semantics { stateDescription = valueText },
            enabled = enabled && !locked,
            valueRange = valueRange,
            steps = steps,
            // 分档滑条不画下面一排小圆点
            showStepIndicator = false,
            backdrop = rememberDefaultBackdrop(),
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

/**
 * 可以交给系统自动调的数值:标题一行右边是上游的开关(「自动」),自动关掉后下面展开上游的滑条。
 * [locked] 为真时开关拨了交给 [onLockedClick],滑条变灰拖不动。
 */
fun LazySectionScope.astraAutoSlider(
    title: String,
    auto: Boolean,
    onAutoChange: (Boolean) -> Unit,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueText: String,
    key: Any? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    summary: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
    locked: Boolean = false,
    onLockedClick: (() -> Unit)? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) = item(key = key) { padding ->
    Column(
        Modifier
            .astraSearchTarget(title)
            .fillMaxWidth()
            .then(if (locked && onLockedClick != null) Modifier.clickable(onClick = onLockedClick) else Modifier)
            .padding(padding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { RowTitle(title, summary, badge) }
            CupertinoText(if (auto) "自动" else valueText, maxLines = 1, style = CupertinoTheme.typography.subhead, color = CupertinoTheme.colorScheme.secondaryLabel)
            SnapBackSwitch(auto, enabled) { want -> if (locked) onLockedClick?.invoke() else onAutoChange(want) }
        }
        AnimatedVisibility(visible = !auto, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            CupertinoSlider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.semantics { stateDescription = valueText },
                enabled = enabled && !locked,
                valueRange = valueRange,
                steps = steps,
                showStepIndicator = false,
                backdrop = rememberDefaultBackdrop(),
                onValueChangeFinished = onValueChangeFinished,
            )
        }
    }
}

/** 分段选择占一行(可带标题):上游的液态玻璃分段选择。 */
fun LazySectionScope.astraSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    key: Any? = null,
    title: String? = null,
    enabled: Boolean = true,
) = item(key = key) { padding ->
    Column(Modifier.astraSearchTarget(title).fillMaxWidth().padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) CupertinoText(title)
        CupertinoSegmentedControl(selectedTabIndex = selectedIndex, paddingValues = PaddingValues(0.dp)) {
            options.forEachIndexed { index, label ->
                CupertinoSegmentedControlTab(
                    isSelected = index == selectedIndex,
                    onClick = { if (enabled && index != selectedIndex) onSelect(index) },
                ) { CupertinoText(label, maxLines = 1) }
            }
        }
    }
}

/** 分组里的一段说明文字(灰色);[warning] 为真时用橙色(影响使用的提醒)。 */
fun LazySectionScope.astraNote(text: String, key: Any? = null, warning: Boolean = false) = item(key = key) { padding ->
    CupertinoText(
        text,
        modifier = Modifier.fillMaxWidth().padding(padding),
        style = CupertinoTheme.typography.footnote,
        color = if (warning) Astra.colors.warning else CupertinoTheme.colorScheme.secondaryLabel,
    )
}
