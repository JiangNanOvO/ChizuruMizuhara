@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.kit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import zone.ien.hig.CupertinoBottomSheetContent
import zone.ien.hig.CupertinoBottomSheetScaffold
import zone.ien.hig.CupertinoBottomSheetScaffoldDefaults
import zone.ien.hig.CupertinoButton
import zone.ien.hig.CupertinoButtonDefaults
import zone.ien.hig.CupertinoSheetValue
import zone.ien.hig.CupertinoText
import zone.ien.hig.PresentationContentInteraction
import zone.ien.hig.PresentationStyle
import zone.ien.hig.CupertinoTopAppBar
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.rememberCupertinoBottomSheetScaffoldState
import zone.ien.hig.rememberCupertinoSheetState

/*
 * 底部弹层:原样用上游 compose-hig 的弹层(从下往上盖住页面、后面的页面缩小、往下拖收起、上方标题栏右边「完成」)。
 * 上游的弹层要把页面包在它的骨架里,所以在主题最外层放一个弹层骨架,各页面的 [AstraSheet] 把要显示的内容交给它。
 * 配色照上游示例:弹层底色浅灰(深色深灰),里面的分组白色(深色稍亮的灰),见 [AstraSheetColors]。
 */

/** 一次要显示的弹层:标题、收起时通知页面、收起后让开时通知页面、上方固定不滚动的内容(可以没有)、内容。 */
internal class SheetRequest(
    val id: Any,
    val title: () -> String,
    val onDismiss: () -> Unit,
    val onHidden: () -> Unit,
    val header: (@Composable () -> Unit)?,
    val content: @Composable ColumnScope.() -> Unit,
)

@Stable
internal class AstraSheetHostState {
    var current by mutableStateOf<SheetRequest?>(null)
}

internal val LocalAstraSheetHost = staticCompositionLocalOf<AstraSheetHostState?> { null }

/** 主题最外层的弹层骨架:上游的弹层骨架包住整个界面,显示各页面交来的弹层。 */
@Composable
internal fun AstraSheetHost(content: @Composable () -> Unit) {
    val host = remember { AstraSheetHostState() }
    // 内容交互用上游默认的 Resize(官方示例同款):内容优先滚,滚到头后剩余手势才交给弹层
    // ——往上顶弹层顶住、往下拉即收起;把手与标题栏可直接拖动收起(用户 2026-10-24:
    // 点小滑条没法往下拖收起,只能点「完成」)。之前选 Scroll 模式是误判:该模式下嵌套
    // 滚动连接不启用,内容滚到头的剩余手势和表面的直接拖拽混在一起,才出现
    // 「一划整个弹窗被提起来」——那是 Scroll 模式的缺陷,不是 Resize 的问题。
    val sheetState = rememberCupertinoSheetState(
        presentationStyle = PresentationStyle.Modal(),
    )
    val scaffoldState = rememberCupertinoBottomSheetScaffoldState(sheetState)
    val closeScope = rememberCoroutineScope()
    val request = host.current
    // 收起动画进行中仍画着上一次的内容;收起到底后放下,屏幕外面不再留着它(试播这类内容会一直重画)
    var shown by remember { mutableStateOf<SheetRequest?>(null) }
    if (request != null) shown = request
    var hostHeight by remember { mutableIntStateOf(0) }
    // 「收到底」时弹层停的那个偏移(隐藏锚点)与当时的窗口高度。上游把 Hidden 锚点设成弹层容器
    // 高度(CupertinoBottomSheetScaffold 的 layoutHeight),与内容高度无关,所以展开前量到的那一次
    // 就是它。用途:区分「真的收起」和「展开动画被手指打断」——后者上游会把 currentValue 留在旧值
    // Hidden,而 offset 还停在半空,只看 currentValue 会把展开途中点一下当成用户关闭
    //(用户 2026-10-24:呼出后一秒内点弹层任意处,整个弹层都会消失;等一下再点就没事)。
    var hiddenAnchor by remember { mutableStateOf<Pair<Float, Int>?>(null) }
    // 程序控制的 show/hide 计数(而非布尔):上游 animateTo 被打断时 finally 会把 currentValue
    // 直接设到最近锚点(SwipeableV2.animateTo),「用户拖拽收起」的监听必须跳过这种非用户到达的
    // Hidden,否则旧收起被新请求的 show() 抢走时,新弹层刚组合就被当成用户关闭弹掉
    //(AstraSheetBehaviorTest.anotherSheetOpensAfterTheFirstMadeWayAndStaysOpen 抓的就是这个)。
    // 用计数是因为交接时两段程序动画会重叠:新 show 先 +1、旧 hide 的 finally 才 -1,
    // 布尔会在旧 finally 置 false 时把新 show 的窗口提前关掉,漏出一帧缝隙。
    val programmaticSheetChanges = remember { mutableIntStateOf(0) }
    suspend fun <T> withProgrammaticSheetChange(block: suspend () -> T): T {
        programmaticSheetChanges.intValue += 1
        try {
            return block()
        } finally {
            programmaticSheetChanges.intValue -= 1
        }
    }
    LaunchedEffect(request?.id) {
        if (request != null) {
            // 上游弹层的展开依赖锚点:锚点由 sheet 首次布局的 onSizeChanged 算出(SwipeableV2.kt
            // 的 swipeAnchors),锚点没注册时 show()=animateTo(Expanded) 只把 currentValue 改成
            // 目标、offset 不动(SwipeableV2.animateTo 的 else 分支),弹层留在屏幕外。
            // 以前在这里「重试 N 次 show()」是错的:每次失败的 show 都把状态改乱,重试还会
            // 重启进行中的动画、从头再播一遍——开合掉帧、要点好几下都来自这里。
            // 正确做法是等条件本身:锚点就绪(offset 可用、Hidden 与展开位是两个不同位置)
            // 才 show,一次到位。等待不限次数:内容首帧排版要多久就等多久(每帧一查,
            // 成本可忽略),点一下必出。
            // 兜底超时:内容为空/零高度等极端情况下 anchors 永远是单点(就绪判断永假),
            // 等待会无限挂起——用户看到弹层收走了、下一个永远不出来。等满 2 秒直接
            // 尝试 show(),由上游自己的状态机兜住(不就绪也只是没动画,不会卡死交互)。
            var waitedFrames = 0
            while (isActive) {
                withFrameNanos { }
                val offsetReady = runCatching { sheetState.requireOffset() }.isSuccess
                if (offsetReady) break
                if (++waitedFrames > 120) break
            }
            // 竞态善后:若之前的中断已把 currentValue 改成 Expanded 而 offset 还在屏外
            //(弹层「假开」),先无动画复位到 Hidden 再展开,否则 show() 认为已经开了、什么都不播。
            val measuredOffset = runCatching { sheetState.requireOffset() }.getOrNull()
            val offsetNow = measuredOffset ?: Float.MAX_VALUE
            if (measuredOffset != null && hostHeight > 0 && measuredOffset >= hostHeight * 0.9f) {
                hiddenAnchor = measuredOffset to hostHeight
            }
            withProgrammaticSheetChange {
                if (sheetState.currentValue != CupertinoSheetValue.Hidden && hostHeight > 0 && offsetNow >= hostHeight * 0.9f) {
                    sheetState.hide()
                }
                sheetState.show()
            }
            return@LaunchedEffect
        }
        val closing = shown ?: return@LaunchedEffect
        var madeWay = false
        fun makeWay() {
            if (madeWay) return
            madeWay = true
            closing.onHidden()
        }
        coroutineScope {
            // 往下收到只露一小截时告诉页面已经让开(例如使用说明二选一选了一份,等弹层让开再弹出那份说明)
            val watcher = launch {
                snapshotFlow { runCatching { sheetState.requireOffset() }.getOrDefault(0f) }
                    .first { hostHeight > 0 && it >= hostHeight * MADE_WAY_FRACTION }
                makeWay()
            }
            withProgrammaticSheetChange {
                try {
                    sheetState.hide()
                } catch (e: CancellationException) {
                    // 收起途中被另一段动画接手(例如新请求的 show()、弹层里的内容变了高度):
                    // 自己没被取消时等它收到底。新请求的 show() 各自持有计数,
                    // 被打断那一刻的 currentValue 突变不会泄给「用户收起」监听。
                    ensureActive()
                    snapshotFlow { sheetState.currentValue }.first { it == CupertinoSheetValue.Hidden }
                }
            }
            watcher.cancel()
            makeWay()
        }
        shown = null
    }
    // 用户往下拖收起时告诉页面,并走同一条干净收尾路径。只在「非程序控制」下到达
    // Hidden 才处理:程序化收起/交接的 Hidden 由各自的流程负责,不在这里重复通知。
    // 收尾(onHidden 通知 + shown 清空)在这里一并做:手势 settle 的动画与程序收起
    // 的 hide() 会抢同一把滚动锁,靠 LaunchedEffect 的收起分支收尾会在抢锁失败后
    // 挂在等待里——弹窗看着关了、宿主状态没清干净,下一次点击要先消化残留状态,
    // 用户要多点两下(用户 2026-10-24:关闭后要再点两下才能呼出)。
    LaunchedEffect(sheetState) {
        var lastHandled: Any? = null
        // 采样里带上 offset:展开途中被手指打断时 currentValue 会停在 Hidden 而 offset 还在半空,
        // 光看 currentValue 就会把「刚呼出来点一下」当成用户收起(弹层整层消失)。真收起(点「完成」、
        // 返回、往下拖到底)都会停在隐藏锚点上,位置对不上就只忽略这一次,不碰状态机。
        // offset 要一起采样:被拖回去的那一段 currentValue 一直是 Hidden,只有位置变化能触发重新判断。
        snapshotFlow {
            Triple(
                sheetState.currentValue,
                programmaticSheetChanges.intValue,
                runCatching { sheetState.requireOffset() }.getOrNull(),
            )
        }
            .drop(1)
            .collect { (value, changes, offset) ->
                val anchor = hiddenAnchor?.takeIf { it.second == hostHeight }?.first
                val atBottom = anchor != null && offset != null && offset >= anchor - 1f
                if (value == CupertinoSheetValue.Hidden && changes == 0 && atBottom) {
                    val closing = shown
                    host.current?.onDismiss?.invoke()
                    if (closing != null && lastHandled != closing.id) {
                        lastHandled = closing.id
                        closing.onHidden()
                    }
                    shown = null
                }
            }
    }
    // 返回手势和「完成」走同一条收起路径(用户 2026-10-24:返回手势没有收起动画,只有「完成」有):
    // 按下时直接 onDismiss 会让页面当场把内容撤下,弹层是瞬间消失的。这里先在弹层自己这一层播收起,
    // 收到底由下面的 Hidden 监听通知页面收起。
    SheetBackHandler(enabled = request != null) {
        closeScope.launch {
            try {
                withProgrammaticSheetChange { sheetState.hide() }
            } catch (_: CancellationException) {
                // 收起被另一次动画接手时由弹层骨架的收起流程收尾
            }
        }
    }
    CompositionLocalProvider(LocalAstraSheetHost provides host) {
        CupertinoBottomSheetScaffold(
            sheetContent = {
                val body = shown
                if (body != null) AstraSheetColors {
                    CupertinoBottomSheetContent(
                        topBar = {
                            // 不另建一层「页面录音」:上游弹层骨架自己给内容提供了 backdrop,顶栏直接用它,
                            // 每秒少录一整层画面,开合弹层时不掉帧
                            CupertinoTopAppBar(
                                title = { CupertinoText(body.title()) },
                                isBackgroundAdaptive = false,
                                actions = {
                                    CupertinoButton(
                                        colors = CupertinoButtonDefaults.plainButtonColors(),
                                        // 点「完成」先走弹层自己的收起动画;收到底后由下面的监听通知页面收起,
                                        // 不在按下时直接把弹层从画面上拿掉。
                                        onClick = {
                                            closeScope.launch {
                                                try {
                                                    withProgrammaticSheetChange { sheetState.hide() }
                                                } catch (_: CancellationException) {
                                                    // 收起被另一次动画接手时由弹层骨架的收起流程收尾
                                                }
                                            }
                                        },
                                    ) { CupertinoText("完成") }
                                },
                            )
                        },
                    ) { pv ->
                        val header = body.header
                        // 弹出输入法时内容区让出输入法的高度,正在填的框自动滚到输入法上面
                        if (header == null) {
                            // 内容铺满弹层(照上游示例的写法):滑动区域才盖得住整块弹层,
                            // 手指落在正文下面那片空白时也是在滚内容,而不是把弹层拖走
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .imePadding()
                                    .verticalScroll(rememberScrollState())
                                    .padding(pv)
                                    .padding(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                content = body.content,
                            )
                        } else {
                            // 上方固定的内容(例如试播画面)不跟着下面的列表滚动
                            Column(Modifier.fillMaxSize().imePadding().padding(pv)) {
                                header()
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                        .padding(bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    content = body.content,
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.onSizeChanged { hostHeight = it.height },
            scaffoldState = scaffoldState,
            colors = CupertinoBottomSheetScaffoldDefaults.colors(sheetContainerColor = Astra.colors.sheet),
        ) { content() }
    }
}

/** 弹层上沿往下收过整个界面高度的这个比例(只露出底下一小截)时算已经让开。 */
private const val MADE_WAY_FRACTION = 0.85f

/**
 * 弹层开着时返回手势先收起弹层:按「盖在页面上面的一层」登记(官方返回手势分发的 overlay 优先级),
 * 排在页面切换的返回前面;只登记普通优先级时,先登记的弹层会被后登记的页面切换抢先,返回直接退掉弹层下面的页面。
 */
@Composable
private fun SheetBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current) { "弹层要放在返回手势分发里面" }.navigationEventDispatcher
    val latest by rememberUpdatedState(onBack)
    val handler = remember {
        object : NavigationEventHandler<NavigationEventInfo>(NavigationEventInfo.None, false) {
            override fun onBackCompleted() = latest()
        }
    }
    SideEffect { handler.isBackEnabled = enabled }
    DisposableEffect(dispatcher) {
        dispatcher.addHandler(handler, NavigationEventDispatcher.PRIORITY_OVERLAY)
        onDispose { handler.remove() }
    }
}

/**
 * 底部弹层:[visible] 为真时从下往上弹出,上方标题栏写 [title]、右边「完成」;往下拖、点「完成」或按返回都收起,收起时调 [onDismiss]。
 * 收起后往下滑到快看不见时调 [onHidden](要在弹层让开后再打开下一页时用)。
 * [header] 放在标题栏下面、固定不滚动(例如边选边看的试播画面),[content] 在它下面滚动。
 */
@Composable
fun AstraSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    header: (@Composable () -> Unit)? = null,
    onHidden: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val host = checkNotNull(LocalAstraSheetHost.current) { "AstraSheet 要放在 AstraTheme 里面" }
    val id = remember { Any() }
    val latestTitle by rememberUpdatedState(title)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val latestHidden by rememberUpdatedState(onHidden)
    val latestHeader by rememberUpdatedState(header)
    val latestContent by rememberUpdatedState(content)
    val request = remember(id) {
        val pinned: (@Composable () -> Unit)? = if (header == null) null else ({ latestHeader?.invoke() })
        SheetRequest(id, { latestTitle }, { latestDismiss() }, { latestHidden() }, pinned, { latestContent(this) })
    }
    SideEffect {
        if (visible && host.current?.id != id) host.current = request
        if (!visible && host.current?.id == id) host.current = null
    }
    DisposableEffect(id) {
        onDispose { if (host.current?.id == id) host.current = null }
    }
}
