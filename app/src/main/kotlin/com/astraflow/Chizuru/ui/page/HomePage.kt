@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.astraflow.Chizuru.capability.lyricprovider.StandaloneLyric
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.AppActions
import com.astraflow.Chizuru.ui.LocalNoticeState
import com.astraflow.Chizuru.ui.islandStateLabel
import com.astraflow.Chizuru.ui.kit.AstraGlassIconButton
import com.astraflow.Chizuru.ui.kit.AstraSheet
import com.astraflow.Chizuru.ui.kit.AstraTabPage
import com.astraflow.Chizuru.ui.kit.astraSection
import com.astraflow.Chizuru.ui.kit.astraSwitch
import com.astraflow.Chizuru.ui.kit.astraValue
import com.astraflow.Chizuru.ui.kit.icons.AstraGlyphs
import com.astraflow.Chizuru.ui.rememberLiveStatus
import com.astraflow.Chizuru.ui.rememberPluginPrefs
import zone.ien.hig.ExperimentalCupertinoApi

/**
 * 首页:插件此刻的运行状态、两个能力的总开关,以及插件本身的启停与桌面图标。
 * 运行状态只放在这一页,链接助手与歌词胶囊页不再重复显示。
 */
@Composable
fun HomePage() {
    val prefs = rememberPluginPrefs()
    val status = rememberLiveStatus()
    val context = LocalContext.current
    val notice = LocalNoticeState.current
    var help by remember { mutableStateOf(false) }
    val providers = remember { runCatching { StandaloneLyric.providerCount() }.getOrDefault(0) }

    AstraTabPage(
        title = "首页",
        trailing = { AstraGlassIconButton(AstraGlyphs.Question, "使用说明", { help = true }) },
    ) {
        astraSection(title = "运行状态") {
            astraValue(
                "模块注入",
                if (prefs.moduleLoaded) "已注入" else "未注入",
                summary = if (prefs.moduleLoaded) null else "请在 LSPosed 中启用本插件并勾选作用域",
            )
            astraValue("星河岛连接", islandStateLabel(status.islandState))
            astraValue("接入库版本", status.sdkVersion)
        }
        astraSection(title = "能力") {
            astraSwitch(
                "歌词胶囊",
                checked = prefs.lyricOn,
                onCheckedChange = { value ->
                    prefs.prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC, value).apply()
                    syncKeepAlive(context, prefs.prefs)
                    notice.show(if (value) "歌词胶囊已开启，重启播放器后生效" else "歌词胶囊已关闭")
                },
                summary = when {
                    !prefs.lyricOn -> "未开启"
                    prefs.lyricStandalone && providers > 0 -> "独立提供者 $providers 个"
                    else -> "正在推送歌词"
                },
            )
            astraSwitch(
                "链接助手",
                checked = prefs.linkOn,
                onCheckedChange = { value ->
                    prefs.prefs.edit().putBoolean(ModulePrefs.KEY_LINK, value).apply()
                    syncKeepAlive(context, prefs.prefs)
                    notice.show(if (value) "链接助手已开启" else "链接助手已关闭")
                },
                summary = if (prefs.linkOn) "识别剪贴板中的链接与号码" else "未开启",
            )
            astraSwitch(
                "常驻保活",
                checked = prefs.keepAlive,
                onCheckedChange = { value ->
                    prefs.prefs.edit().putBoolean(ModulePrefs.KEY_KEEP_ALIVE, value).apply()
                    if (value) AppActions.requestNotifyPermission(context)
                    syncKeepAlive(context, prefs.prefs)
                    notice.show(if (value) "已开启常驻保活，通知栏将出现一条静默通知" else "已关闭常驻保活")
                },
                summary = if (prefs.keepAlive && status.keepAliveRunning) "运行中" else "以一条静默通知维持进程运行",
            )
        }
        astraSection(title = "插件") {
            astraSwitch(
                "启用插件",
                checked = prefs.enabled,
                onCheckedChange = { value ->
                    prefs.prefs.edit().putBoolean(ModulePrefs.KEY_ENABLED, value).apply()
                    if (!value) runCatching { KeepAliveService.stop(context) } else syncKeepAlive(context, prefs.prefs)
                    notice.show(if (value) "插件已启用，重启目标应用后生效" else "插件已停用")
                },
            )
            astraSwitch(
                "隐藏桌面图标",
                checked = prefs.hideIcon,
                onCheckedChange = { value ->
                    AppActions.setLauncherIconHidden(context, value)
                    prefs.prefs.edit().putBoolean(ModulePrefs.KEY_HIDE_ICON, value).apply()
                    notice.show(if (value) "桌面图标已隐藏" else "桌面图标已恢复")
                },
                summary = "隐藏后仍可从 LSPosed 进入本应用",
            )
        }
    }

    AstraSheet(visible = help, onDismiss = { help = false }, title = "Chizuru 使用说明") {
        HelpContent(
            listOf(
                HelpSection(
                    "使用前提",
                    "本插件为星流的第三方插件，适用于 OPPO、一加与真我手机。使用前请先安装星流，并在 LSPosed 中启用本插件。" +
                        "使用歌词胶囊时，请勾选需要放歌的播放器；使用链接助手时，请勾选「系统框架」。勾选完成后，请将目标应用停止运行一次。",
                    prerequisite = true,
                ),
                HelpSection(
                    "运行状态",
                    "「模块注入」表示目标应用已加载本插件；「星河岛连接」表示星流已接受本插件；「接入库版本」为星河岛接入库的版本号。" +
                        "状态异常时，请按对应行下方的提示处理。",
                ),
                HelpSection(
                    "歌词胶囊",
                    "播放器获取的歌词按 LyricON 的「歌词提供者」协议交付星流，由星河岛歌词胶囊显示，逐字歌词与翻译均已适配。" +
                        "酷狗音乐与酷狗概念版直接读取歌词文件；酷我音乐自行解析缓存并结合播放器回调；其余播放器优先读取本地缓存，无法读取时进行一次只读的网络旁路嗅探。",
                ),
                HelpSection(
                    "链接助手",
                    "复制链接、电话号码或地址后，卡片将出现在星河岛，点击卡片可直接打开浏览器、拨号、短信或地图。" +
                        "识别仅针对可判定为链接、号码或地址的文本，全程在本机完成，不记录、不联网，日志中的链接一律隐去。",
                ),
                HelpSection(
                    "常驻保活",
                    "开启后，后台服务将以一条静默通知维持进程运行，复制链接后可立即上岛；关闭后服务停止。关闭「链接助手」时，保活服务同时停止。",
                ),
                HelpSection(
                    "插件",
                    "关闭「启用插件」后，歌词推送、链接识别与后台保活将同时停止；重新开启后，需重启目标应用方可生效。" +
                        "「隐藏桌面图标」隐藏后桌面不再显示图标，仍可从 LSPosed 的模块页面进入本应用。",
                ),
            ),
        )
    }
}
