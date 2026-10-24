package com.astraflow.Chizuru.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.delay

/**
 * 搜索跳到某一项(照 iOS 设置的搜索):搜索页点了一条设置,先用 [request] 记下要找的行名再打开那一页;
 * 页面里同名的那一行出现时滚到看得见的位置并闪一下底色。行还没排出来时,页面骨架一屏一屏往下翻着找,
 * 翻到底还没有就作罢。要找的行只在某个开关开着时才显示([request] 的 revealedBy):开关关着、那一行不在页面上,
 * 就回到页面顶部改找那个开关闪一下,告诉用户先开它。找到或作罢后清空,之后打开别的页面不再闪。
 */
object AstraSearchHighlight {
    /** 要找的行名。 */
    var target by mutableStateOf<String?>(null)
        private set

    /** 要找的行藏在哪个开关后面(开关的行名);没有为空。 */
    internal var revealedBy: String? = null
        private set

    /** 这一次已经有行认领了,页面骨架不用再往下翻。 */
    internal var found by mutableStateOf(false)

    fun request(title: String, revealedBy: String? = null) {
        found = false
        this.revealedBy = revealedBy
        target = title
    }

    /** 要找的行不在页面上:改找让它显示出来的那个开关。 */
    internal fun retargetToReveal(switchTitle: String) {
        revealedBy = null
        found = false
        target = switchTitle
    }

    internal fun clear() {
        target = null
        revealedBy = null
        found = false
    }
}

/** 搜索页里的结果行与要找的行同名,不参与查找。 */
val LocalSearchHighlightEnabled = staticCompositionLocalOf { true }

/** 页面打开后等它滑进来再找,和页面切换的时长相当。 */
private const val ARRIVE_DELAY_MS = 380L

/** 设置行用:名字与要找的一样时滚到看得见并闪一下。 */
fun Modifier.astraSearchTarget(title: String?): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val flash = remember { Animatable(0f) }
    var flashes by remember { mutableIntStateOf(0) }
    val hit = title != null && LocalSearchHighlightEnabled.current && AstraSearchHighlight.target == title
    LaunchedEffect(hit) {
        if (!hit) return@LaunchedEffect
        AstraSearchHighlight.found = true
        delay(ARRIVE_DELAY_MS)
        requester.bringIntoView()
        flashes++
        AstraSearchHighlight.clear()
    }
    LaunchedEffect(flashes) {
        if (flashes == 0) return@LaunchedEffect
        flash.snapTo(1f)
        flash.animateTo(0f, tween(durationMillis = 1200, delayMillis = 500))
    }
    val color = Astra.colors.accent
    this
        .bringIntoViewRequester(requester)
        .drawBehind { if (flash.value > 0f) drawRect(color.copy(alpha = 0.16f * flash.value)) }
}

/**
 * 页面骨架用:有要找的行而这一屏没有时,一屏一屏往下翻,让后面的行排出来认领;最多翻 15 屏。
 * 翻到底还没有:那一行藏在某个开关后面的,回到顶部改找那个开关;否则作罢。
 */
@Composable
internal fun SearchScroll(listState: LazyListState) {
    val target = AstraSearchHighlight.target ?: return
    if (!LocalSearchHighlightEnabled.current) return
    LaunchedEffect(target) {
        delay(ARRIVE_DELAY_MS + 120)
        var screens = 0
        while (AstraSearchHighlight.target == target && !AstraSearchHighlight.found && screens < 15 && listState.canScrollForward) {
            listState.animateScrollBy(listState.layoutInfo.viewportSize.height * 0.8f)
            screens++
            delay(80)
        }
        if (AstraSearchHighlight.target != target || AstraSearchHighlight.found) return@LaunchedEffect
        val switchTitle = AstraSearchHighlight.revealedBy
        if (switchTitle == null) {
            AstraSearchHighlight.clear()
            return@LaunchedEffect
        }
        listState.animateScrollToItem(0)
        AstraSearchHighlight.retargetToReveal(switchTitle)
    }
}
