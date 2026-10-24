@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astraflow.Chizuru.ui.kit.icons.AstraGlyphs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import zone.ien.hig.CupertinoScaffold
import zone.ien.hig.CupertinoText
import zone.ien.hig.CupertinoTopAppBar
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.section.CupertinoSection
import zone.ien.hig.section.SectionItem
import zone.ien.hig.theme.CupertinoTheme

/*
 * 页面骨架原样照上游 compose-hig 的示例页写:上游的页面骨架、顶部栏(左边玻璃返回按钮,右边操作按钮)、
 * 跟着内容一起滚的大标题,下面是上游的分组列表。页面内容录成一层画面,顶部栏和顶部栏上的玻璃圆按钮从这层画面取玻璃背后的样子;
 * 页面里面的玻璃部件用上游默认的背景(取页面自己那层画面会画到自己)。
 */

/** 上游示例页的骨架。[tabBarClearance] 是页面最后给底栏让出的高度(标签页用)。 */
@Composable
private fun HigPage(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
    listState: LazyListState,
    modifier: Modifier,
    listModifier: Modifier,
    tabBarClearance: Dp,
    overlay: (@Composable (LayerBackdrop) -> Unit)?,
    titleContent: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val featureHeaderScrolled by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    // 从搜索跳过来时,一屏一屏往下翻找要闪的那一行
    SearchScroll(listState)
    CupertinoScaffold(
        // 页面先在自身坐标内合成,切页平移时复用整页纹理,避免每帧按屏幕坐标重建圆角裁切遮罩。
        modifier = modifier.fillMaxSize().graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
        hasNavigationTitle = titleContent == null,
        topBar = {
            CompositionLocalProvider(LocalAstraBackdrop provides backdrop) {
                // 不让上游顶部栏每 0.3 秒把背后的画面拍下来算亮暗(在主线程上取图,会掉帧);字色跟着深浅色设置走
                CupertinoTopAppBar(
                    title = {
                        if (titleContent == null || featureHeaderScrolled) CupertinoText(title, maxLines = 1)
                    },
                    isBackgroundAdaptive = false,
                    navigationIcon = {
                        if (onBack != null) {
                            AstraGlassIconButton(
                                icon = AstraGlyphs.ChevronLeft,
                                contentDescription = "返回",
                                onClick = onBack,
                                modifier = Modifier.padding(start = 16.dp),
                                backdrop = backdrop,
                            )
                        }
                    },
                    actions = {
                        if (actions != null) {
                            Row(
                                Modifier.padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                content = actions,
                            )
                        }
                    },
                    backdrop = backdrop,
                )
            }
        },
    ) { pv ->
        // 输入法盖住的那一截也铺上页面底色(输入法上沿带圆角时看不出两种颜色)
        Box(Modifier.fillMaxSize().background(CupertinoTheme.colorScheme.systemGroupedBackground)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    // 弹出输入法时列表让出输入法的高度:正在填的框被盖住时自动滚到输入法上面
                    .imePadding()
                    .layerBackdrop(backdrop)
                    .fillMaxSize()
                    .background(CupertinoTheme.colorScheme.systemGroupedBackground)
                    .then(listModifier),
                contentPadding = PaddingValues(
                    top = pv.calculateTopPadding(),
                    bottom = pv.calculateBottomPadding() + tabBarClearance + 24.dp,
                ),
            ) {
                item(key = "astra_navigation_title") {
                    if (titleContent != null) {
                        // 介绍卡按普通分组显示;整张卡滚过顶部栏后,栏中间显示功能名。
                        titleContent()
                    } else {
                        AstraNavigationTitle(subtitle = subtitle?.let { { CupertinoText(it) } }) {
                            CupertinoText(title)
                        }
                    }
                }
                content()
            }
            if (overlay != null) {
                CompositionLocalProvider(LocalAstraBackdrop provides backdrop) { overlay(backdrop) }
            }
        }
    }
}

/**
 * 标签页(首页、星河岛、系统、商店、搜索)的骨架:上游的顶部栏与大标题,右上角放头像([trailing]);
 * 大标题下面可以有一行灰色小字([subtitle]);页面最后给底栏让出位置。
 */
@Composable
fun AstraTabPage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    listState: LazyListState = rememberLazyListState(),
    trailing: (@Composable () -> Unit)? = null,
    listModifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) = HigPage(
    title = title,
    subtitle = subtitle,
    onBack = null,
    actions = trailing?.let { { it() } },
    listState = listState,
    modifier = modifier,
    listModifier = listModifier,
    tabBarClearance = AstraDimens.tabBarClearance,
    overlay = null,
    content = content,
)

/**
 * 二级页面的骨架:上游的顶部栏(左边玻璃返回按钮,右边 [actions])与大标题。
 * [bottomBar] 浮在页面底部(例如保存按钮)。
 */
@Composable
fun AstraSubPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    actions: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    listModifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) = HigPage(
    title = title,
    subtitle = null,
    onBack = onBack,
    actions = actions?.let { { it() } },
    listState = listState,
    modifier = modifier,
    listModifier = listModifier,
    tabBarClearance = if (bottomBar != null) 72.dp else 0.dp,
    overlay = bottomBar?.let { bar ->
        { _ ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp)) { bar() }
            }
        }
    },
    content = content,
)

/** 功能页的总开关:开着为真;没开通的星尘功能由 [onToggle] 去弹开通说明。 */
class AstraFeatureToggle(
    val on: Boolean,
    val onToggle: () -> Unit,
    val enabled: Boolean = true,
)

/**
 * 功能页的骨架(草图方向 A,照 iOS 26 应用商店的应用详情页):上游的顶部栏(左边返回,右边「使用说明」问号),
 * 最上面是介绍卡:居中的图标([icon]、[iconColor])、名字(可带 [badge])与说明([summary]),总开关([toggle])在卡片下半部分的一行;
 * 往下翻到顶部栏下面时栏中间浮出功能名。再往下是各组设置([content])。效果预览只放在首页,功能页里不放。
 */
@Composable
fun AstraFeaturePage(
    title: String,
    summary: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    badge: String? = null,
    toggle: AstraFeatureToggle? = null,
    onHelp: (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    listModifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) = HigPage(
    title = title,
    subtitle = null,
    onBack = onBack,
    actions = onHelp?.let { help -> { AstraGlassIconButton(AstraGlyphs.Question, "使用说明", help) } },
    listState = listState,
    modifier = modifier,
    listModifier = listModifier,
    tabBarClearance = 0.dp,
    overlay = null,
    titleContent = { FeatureHeader(title, summary, icon, iconColor, badge, toggle) },
    content = content,
)

/** 功能页介绍卡:居中的图标、名称与说明;总开关与设置行使用同一种排法。 */
@Composable
private fun FeatureHeader(
    title: String,
    summary: String,
    icon: ImageVector?,
    iconColor: Color?,
    badge: String?,
    toggle: AstraFeatureToggle?,
) {
    CupertinoSection(modifier = Modifier.astraSearchTarget(title)) {
        SectionItem {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) AstraIconTile(icon, iconColor ?: CupertinoTheme.colorScheme.accent, 48.dp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CupertinoText(
                        title,
                        modifier = Modifier.weight(1f, fill = false),
                        style = CupertinoTheme.typography.title2,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (badge != null) AstraBadge(badge)
                }
                CupertinoText(
                    summary,
                    style = CupertinoTheme.typography.subhead,
                    color = CupertinoTheme.colorScheme.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (toggle != null) {
            SectionItem(
                trailingContent = {
                    SnapBackSwitch(toggle.on, toggle.enabled) { want -> if (want != toggle.on) toggle.onToggle() }
                },
            ) { CupertinoText(title, style = CupertinoTheme.typography.body) }
        }
    }
}
