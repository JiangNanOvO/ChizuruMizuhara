@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.astraflow.Chizuru.ui.LocalNoticeState
import com.astraflow.Chizuru.ui.NoticeState
import com.astraflow.Chizuru.ui.AstraNoticeHost
import com.astraflow.Chizuru.ui.kit.Astra
import com.astraflow.Chizuru.ui.kit.AstraTab
import com.astraflow.Chizuru.ui.kit.AstraTabBar
import com.astraflow.Chizuru.ui.kit.AstraTheme
import com.astraflow.Chizuru.ui.kit.LocalAstraBackdrop
import com.astraflow.Chizuru.ui.kit.icons.AstraGlyphs
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import zone.ien.hig.ExperimentalCupertinoApi

/**
 * 插件的界面:主应用与星流同一套上游部件(compose-hig 的页面骨架、分组列表、开关、弹层、确认框与玻璃底栏)。
 * 四个标签页左右排成一排,手指左右滑或点底栏切换;底栏是上游的玻璃底栏。
 */
private val ShellTabs = listOf(
    AstraTab("首页", AstraGlyphs.TabToday),
    AstraTab("链接", AstraGlyphs.ExternalApps),
    AstraTab("歌词", AstraGlyphs.Music),
    AstraTab("关于", AstraGlyphs.Info),
)

/** 起始标签页:首页 0、链接 1、歌词 2、关于 3。 */
@Composable
fun ChizuruApp(initialTab: Int = 0) {
    AstraTheme {
        val notice = remember { NoticeState() }
        CompositionLocalProvider(LocalNoticeState provides notice) {
            Shell(initialTab.coerceIn(0, ShellTabs.size - 1))
            AstraNoticeHost()
        }
    }
}

@Composable
private fun Shell(initialTab: Int) {
    val pager = rememberPagerState(initialPage = initialTab) { ShellTabs.size }
    val scope = rememberCoroutineScope()
    val goTo: (Int) -> Unit = { page ->
        if (!pager.isScrollInProgress && page != pager.currentPage) scope.launch { pager.animateScrollToPage(page) }
    }
    Box(Modifier.fillMaxSize().background(Astra.colors.background)) {
        val backdrop = rememberLayerBackdrop()
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
            beyondViewportPageCount = ShellTabs.size - 1,
            key = { it },
        ) { page ->
            when (page) {
                0 -> HomePage()
                1 -> LinkPage()
                2 -> LyricPage()
                else -> AboutPage()
            }
        }
        CompositionLocalProvider(LocalAstraBackdrop provides backdrop) {
            AstraTabBar(
                tabs = ShellTabs,
                selectedIndex = pager.currentPage,
                onSelect = goTo,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}