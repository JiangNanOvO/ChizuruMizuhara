@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.kit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.CupertinoNavigationBar
import zone.ien.hig.CupertinoNavigationBarItem
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi

/** 底栏上的一个标签。 */
class AstraTab(val label: String, val icon: ImageVector)

/** 底栏标签的图标与文字:上游底栏默认的 18 与 10 太小,按星流同款放大到 24 与 12。 */
private val TabIconSize = 24.dp
private val TabLabelStyle = TextStyle(fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)

/** 上游底栏自带的外侧留白、两端内边距(新版上游内部左右各 8,格子之间没有间隔)。 */
private val OuterInset = 36.dp
private val BarPadding = 8.dp

/**
 * 浮在页面底部的底栏:上游 compose-hig 的玻璃底栏(滑块、按住拖动换标签都是上游的)。
 * 与星流那一套同源,只是插件只有四个标签、没有右边单独的搜索格。
 * [selectedIndex] 为 -1 时不显示滑块。
 */
@Composable
fun AstraTabBar(
    tabs: List<AstraTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastTab by remember { mutableIntStateOf(selectedIndex.coerceAtLeast(0)) }
    if (selectedIndex >= 0) lastTab = selectedIndex
    val latestLastTab by rememberUpdatedState(lastTab)
    // 上游底栏按「选中项」这个函数本身记住拖动状态:每次刷新换一个新函数会把拖动状态清掉,拖起来发粘;这里只建一次
    val tabsSelected = remember { { latestLastTab } }
    BoxWithConstraints(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
        // 整条底栏的宽和高按同一倍率画,系统显示大小变了要重建:
        // 上游底栏内部有些尺寸按像素记着,只换密度不一定重排,会显得被拉高
        val density = LocalDensity.current
        val densityDpi = LocalConfiguration.current.densityDpi
        val width = with(density) { constraints.maxWidth.toDp() }
        val item = ((width - OuterInset * 2 - BarPadding * 2) / tabs.size).coerceAtMost(90.dp)
        val barDensity = remember(density) { Density(density.density, density.fontScale) }
        CompositionLocalProvider(LocalDensity provides barDensity) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                key(barDensity, item, densityDpi) {
                    CupertinoNavigationBar(
                        modifier = Modifier.width(OuterInset * 2 + BarPadding * 2 + item * tabs.size),
                        windowInsets = WindowInsets(left = OuterInset, right = OuterInset),
                        backdrop = astraPageBackdrop(),
                        selectedTabIndex = tabsSelected,
                        onTabSelected = onSelect,
                        tabsCount = tabs.size,
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            CupertinoNavigationBarItem(
                                onClick = { onSelect(index) },
                                modifier = Modifier.semantics { selected = index == selectedIndex },
                                icon = { CupertinoIcon(tab.icon, contentDescription = null, modifier = Modifier.requiredSize(TabIconSize)) },
                                label = { CupertinoText(tab.label, style = TabLabelStyle, maxLines = 1) },
                            )
                        }
                    }
                }
            }
        }
    }
}
