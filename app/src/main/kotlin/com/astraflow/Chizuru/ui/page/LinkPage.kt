@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.astraflow.Chizuru.capability.link.BrowserChoice
import com.astraflow.Chizuru.capability.link.ClipIntent
import com.astraflow.Chizuru.capability.link.ClipKind
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.capability.link.MapChoice
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.LocalNoticeState
import com.astraflow.Chizuru.ui.kit.AstraGlassIconButton
import com.astraflow.Chizuru.ui.kit.AstraSheet
import com.astraflow.Chizuru.ui.kit.AstraTabPage
import com.astraflow.Chizuru.ui.kit.astraAction
import com.astraflow.Chizuru.ui.kit.astraPicker
import com.astraflow.Chizuru.ui.kit.astraSection
import com.astraflow.Chizuru.ui.kit.astraSwitch
import com.astraflow.Chizuru.ui.kit.icons.AstraGlyphs
import com.astraflow.Chizuru.ui.rememberPluginPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.hig.ExperimentalCupertinoApi

/**
 * 链接助手:只放这一项能力自己的设置(识别哪几类内容、在哪打开、自检)。
 * 运行状态与总开关都在首页,这一页不重复显示。
 */
@Composable
fun LinkPage() {
    val prefs = rememberPluginPrefs()
    val context = LocalContext.current
    val notice = LocalNoticeState.current
    val scope = rememberCoroutineScope()
    val browsers = remember { BrowserChoice.installed(context) }
    val maps = remember { MapChoice.installed(context) }
    var help by remember { mutableStateOf(false) }
    var testBusy by remember { mutableStateOf(false) }

    val postTest: (ClipKind, String) -> Unit = { kind, payload ->
        if (!testBusy) {
            testBusy = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        LinkHub.postBlocking(context.applicationContext, ClipIntent(kind, payload))
                    }.getOrDefault(false)
                }
                testBusy = false
                notice.show(
                    if (ok) "已投递至星河岛，可前往星河岛查看卡片"
                    else "投递失败。请确认已安装星流、已在星河岛开启相关显示，并确认本插件未被系统限制后台运行。",
                )
            }
        }
    }

    AstraTabPage(
        title = "链接助手",
        trailing = { AstraGlassIconButton(AstraGlyphs.Question, "使用说明", { help = true }) },
    ) {
        astraSection(title = "识别内容") {
            astraSwitch(
                "链接上岛",
                checked = prefs.clipLink,
                onCheckedChange = { value -> prefs.prefs.edit().putBoolean(ModulePrefs.KEY_CLIP_LINK, value).apply() },
            )
            astraSwitch(
                "电话号码上岛",
                checked = prefs.clipPhone,
                onCheckedChange = { value -> prefs.prefs.edit().putBoolean(ModulePrefs.KEY_CLIP_PHONE, value).apply() },
            )
            astraSwitch(
                "地址上岛",
                checked = prefs.clipAddress,
                onCheckedChange = { value -> prefs.prefs.edit().putBoolean(ModulePrefs.KEY_CLIP_ADDRESS, value).apply() },
            )
        }
        astraSection(title = "打开方式") {
            astraPicker(
                title = "链接打开方式",
                options = listOf("跟随系统默认浏览器", "每次选择") + browsers.map { it.label },
                selectedIndex = run {
                    val pinned = browsers.indexOfFirst { it.packageName == prefs.browserPackage }
                    when (prefs.browserMode) {
                        ModulePrefs.BROWSER_PINNED -> if (pinned >= 0) pinned + 2 else 0
                        ModulePrefs.BROWSER_ASK -> 1
                        else -> 0
                    }
                },
                onSelect = { index ->
                    val editor = prefs.prefs.edit()
                    when {
                        index == 0 -> editor.putString(ModulePrefs.KEY_BROWSER_MODE, ModulePrefs.BROWSER_SYSTEM)
                        index == 1 -> editor.putString(ModulePrefs.KEY_BROWSER_MODE, ModulePrefs.BROWSER_ASK)
                        else -> {
                            editor.putString(ModulePrefs.KEY_BROWSER_MODE, ModulePrefs.BROWSER_PINNED)
                            editor.putString(ModulePrefs.KEY_BROWSER_PACKAGE, browsers[index - 2].packageName)
                        }
                    }
                    editor.apply()
                    notice.show("设置已保存")
                },
            )
            astraPicker(
                title = "地图打开方式",
                options = listOf("跟随系统默认", "每次询问") + maps.map { it.label },
                selectedIndex = run {
                    val pinned = maps.indexOfFirst { it.packageName == prefs.mapPackage }
                    when (prefs.mapMode) {
                        ModulePrefs.MAP_PINNED -> if (pinned >= 0) pinned + 2 else 0
                        ModulePrefs.MAP_ASK -> 1
                        else -> 0
                    }
                },
                onSelect = { index ->
                    val editor = prefs.prefs.edit()
                    when {
                        index == 0 -> editor.putString(ModulePrefs.KEY_MAP_MODE, ModulePrefs.MAP_SYSTEM)
                        index == 1 -> editor.putString(ModulePrefs.KEY_MAP_MODE, ModulePrefs.MAP_ASK)
                        else -> {
                            editor.putString(ModulePrefs.KEY_MAP_MODE, ModulePrefs.MAP_PINNED)
                            editor.putString(ModulePrefs.KEY_MAP_PACKAGE, maps[index - 2].packageName)
                        }
                    }
                    editor.apply()
                    notice.show("地图设置已保存")
                },
            )
            astraPicker(
                title = "卡片显示方式",
                options = listOf("展开提示", "仅显示胶囊"),
                selectedIndex = if (prefs.hintOnly) 1 else 0,
                onSelect = { index ->
                    prefs.prefs.edit().putBoolean(ModulePrefs.KEY_LINK_HINT_ONLY, index == 1).apply()
                    notice.show("设置已保存")
                },
            )
        }
        astraSection(title = "自检") {
            astraAction(
                "投递链接卡片",
                { postTest(ClipKind.LINK, SELF_TEST_URL) },
                value = if (testBusy) "投递中…" else null,
                enabled = !testBusy,
            )
            astraAction("投递电话卡片", { postTest(ClipKind.PHONE, SELF_TEST_PHONE) }, value = SELF_TEST_PHONE, enabled = !testBusy)
            astraAction("投递地址卡片", { postTest(ClipKind.ADDRESS, SELF_TEST_ADDRESS) }, value = SELF_TEST_ADDRESS, enabled = !testBusy)
        }
    }

    AstraSheet(visible = help, onDismiss = { help = false }, title = "链接助手使用说明") {
        HelpContent(
            listOf(
                HelpSection(
                    "使用前提",
                    "请在 LSPosed 中将本插件的「系统框架」勾选到作用域，并重启系统框架。作用域仅需勾选这一项。",
                    prerequisite = true,
                ),
                HelpSection(
                    "识别范围",
                    "仅处理可判定为链接、电话号码或地址的文本，其余复制内容不予处理。识别全程在本机完成，不联网、不上传，日志中的链接一律隐去。",
                ),
                HelpSection(
                    "卡片",
                    "卡片由星河岛绘制，复制后三分钟内出现。点击卡片由星河岛直接打开浏览器、拨号或地图，不依赖本插件进程存活。" +
                        "「卡片显示方式」选择「仅显示胶囊」时，卡片不会自动展开。「自检」中的三项可使用示例内容走一遍完整流程。",
                ),
            ),
        )
    }
}

/** 按当前设置决定保活服务的启停。 */
internal fun syncKeepAlive(context: android.content.Context, prefs: android.content.SharedPreferences) {
    val enabled = ModulePrefs.isEnabled(prefs)
    val want = enabled && (
        (ModulePrefs.isLyricEnabled(prefs) && ModulePrefs.isStandaloneLyric(prefs)) ||
            (ModulePrefs.isLinkEnabled(prefs) && ModulePrefs.isKeepAlive(prefs))
        )
    runCatching { if (want) KeepAliveService.start(context) else KeepAliveService.stop(context) }
}

private const val SELF_TEST_URL = "https://astraflow.cc/island"
private const val SELF_TEST_PHONE = "18888888888"
private const val SELF_TEST_ADDRESS = "浙江省杭州市西湖区文三路 100 号"
