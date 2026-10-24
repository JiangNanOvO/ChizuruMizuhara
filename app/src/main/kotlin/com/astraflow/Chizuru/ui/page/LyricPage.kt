@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.LocalNoticeState
import com.astraflow.Chizuru.ui.kit.AstraGlassIconButton
import com.astraflow.Chizuru.ui.kit.AstraSheet
import com.astraflow.Chizuru.ui.kit.AstraTabPage
import com.astraflow.Chizuru.ui.kit.astraLink
import com.astraflow.Chizuru.ui.kit.astraSection
import com.astraflow.Chizuru.ui.kit.astraSwitch
import com.astraflow.Chizuru.ui.kit.icons.AstraGlyphs
import com.astraflow.Chizuru.ui.rememberPluginPrefs
import zone.ien.hig.ExperimentalCupertinoApi

/**
 * 歌词胶囊:只放这一项能力自己的设置(独立提供者、音理音理适配、适配清单)。
 * 运行状态与总开关都在首页,这一页不重复显示。
 */
@Composable
fun LyricPage() {
    val prefs = rememberPluginPrefs()
    val context = LocalContext.current
    val notice = LocalNoticeState.current
    var help by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf(false) }

    AstraTabPage(
        title = "歌词胶囊",
        trailing = { AstraGlassIconButton(AstraGlyphs.Question, "使用说明", { help = true }) },
    ) {
        astraSection(title = "功能") {
            astraSwitch(
                "独立提供者（root）",
                checked = prefs.lyricStandalone,
                onCheckedChange = { value ->
                    prefs.prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC_STANDALONE, value).apply()
                    syncKeepAlive(context, prefs.prefs)
                    notice.show(if (value) "已开启独立提供者，正在通过 root 读取播放状态与歌词缓存" else "已关闭独立提供者")
                },
                summary = "由本插件作为歌词提供者，播放器进程结束亦可继续供词",
            )
            astraSwitch(
                "音理音理适配（root）",
                checked = prefs.neriAdapt,
                onCheckedChange = { value ->
                    prefs.prefs.edit().putBoolean(ModulePrefs.KEY_NERI_ADAPT, value).apply()
                    notice.show(if (value) "已开启音理音理适配" else "已关闭音理音理适配")
                },
                summary = "通过 root 自动开启音理音理的外部歌词输出",
            )
        }
        astraSection(title = "适配的应用") {
            astraLink("适配应用清单", { apps = true })
        }
    }

    AstraSheet(visible = help, onDismiss = { help = false }, title = "歌词胶囊使用说明") {
        HelpContent(
            listOf(
                HelpSection(
                    "使用前提",
                    "请在 LSPosed 中勾选需要放歌的播放器，并将该播放器停止运行一次；随后在星流中开启「胶囊歌词」。",
                    prerequisite = true,
                ),
                HelpSection(
                    "歌词来源",
                    "播放器获取的歌词按 LyricON 的「歌词提供者」协议交付星流，由星河岛歌词胶囊显示，逐字歌词与翻译均已适配。" +
                        "酷狗音乐与酷狗概念版直接读取歌词文件；酷我音乐自行解析缓存并结合播放器回调；其余播放器优先读取本地缓存，无法读取时进行一次只读的网络旁路嗅探。",
                ),
                HelpSection(
                    "独立提供者（root）",
                    "播放器进程被系统结束后，开启此项可由本插件继续供词：读取播放状态与歌词缓存，仅作读取，不修改播放器设置。未开启 root 亦可正常使用，歌词胶囊与链接助手均不依赖此项。",
                ),
                HelpSection(
                    "音理音理适配（root）",
                    "音理音理的「外部歌词输出」默认关闭，未开启时不会输出歌词。开启本项后，插件将通过 root 自动将其打开，仅修改这一项设置。",
                ),
            ),
        )
    }
    AstraSheet(visible = apps, onDismiss = { apps = false }, title = "适配应用清单") {
        HelpContent(
            listOf(
                HelpSection(
                    "已适配",
                    "酷狗音乐、酷狗概念版、酷我音乐、QQ 音乐、汽水音乐、网易云音乐、OPPO 音乐（欢太音乐）、波点音乐、MeloYou、洛雪音乐助手、音理音理。",
                ),
                HelpSection(
                    "音理音理",
                    "无需手动开启：启用「音理音理适配」后，插件将自动开启其「外部歌词输出」。",
                ),
                HelpSection(
                    "其它应用",
                    "在 LSPosed 中勾选其作用域后，插件将优先读取本地歌词缓存；无法读取时，仅查看网络上的歌词响应，不处理其它内容。",
                ),
            ),
        )
    }
}
