@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.astraflow.Chizuru.ui.AppActions
import com.astraflow.Chizuru.ui.LocalNoticeState
import com.astraflow.Chizuru.ui.kit.AstraGlassIconButton
import com.astraflow.Chizuru.ui.kit.AstraSheet
import com.astraflow.Chizuru.ui.kit.AstraTabPage
import com.astraflow.Chizuru.ui.kit.astraAction
import com.astraflow.Chizuru.ui.kit.astraNote
import com.astraflow.Chizuru.ui.kit.astraSection
import com.astraflow.Chizuru.ui.kit.astraValue
import com.astraflow.Chizuru.ui.kit.icons.AstraGlyphs
import zone.ien.hig.ExperimentalCupertinoApi

/**
 * 关于:版本、作者与说明,下面是贡献者头像墙。
 * 插件的启停与桌面图标在首页;使用前提、隐私与许可写在本页右上角问号打开的说明里。
 */
@Composable
fun AboutPage() {
    val context = LocalContext.current
    val notice = LocalNoticeState.current
    var help by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    // 详情按"第几位"记:名单里有三位同名「开发人员」,记成对象没法回头认人
    var pickedIndex by remember { mutableIntStateOf(-1) }
    val picked = CONTRIBUTORS.getOrNull(pickedIndex)

    AstraTabPage(
        title = "关于",
        trailing = { AstraGlassIconButton(AstraGlyphs.Question, "使用说明", { help = true }) },
    ) {
        astraSection(title = "关于") {
            astraValue("版本", "v" + AppActions.versionName(context))
            astraValue("作者", "江南の千愁")
            astraAction("关于 Chizuru", { about = true })
        }
        astraSection(title = "贡献者") {
            item(key = "contributor_wall") { padding ->
                Box(Modifier.fillMaxWidth().padding(padding)) {
                    ContributorWall(
                        contributors = CONTRIBUTORS,
                        // 详情开着时对应的头像留在放大位置,收起后再落回
                        selectedIndex = pickedIndex,
                        onPick = { index ->
                            // 点头像只开详情:先把可能还挂着的提示收掉,免得盖在详情上
                            notice.dismiss()
                            pickedIndex = index
                        },
                    )
                }
            }
            astraNote("双指缩放可以放大查看，放大后能拖动，轻点头像看详情。")
        }
    }

    ContributorDetailSheet(contributor = picked, onDismiss = { pickedIndex = -1 })

    AstraSheet(visible = help, onDismiss = { help = false }, title = "Chizuru 使用说明") {
        HelpContent(
            listOf(
                HelpSection(
                    "使用前提",
                    "本插件为星流的第三方插件，需先安装星流，并在 LSPosed 中启用本插件、勾选作用域。" +
                        "使用歌词胶囊时请勾选需要放歌的播放器；使用链接助手时请勾选「系统框架」。勾选完成后，请将目标应用停止运行一次。",
                    prerequisite = true,
                ),
                HelpSection(
                    "隐私",
                    "歌词与剪贴板内容均在本机处理。本插件未申请网络权限，不会上传或统计任何内容。",
                ),
                HelpSection(
                    "许可",
                    "Chizuru 的代码以 MIT 许可免费开源，请勿用于商业用途或二次销售。随包附带的星流接入库 astraisland-sdk 采用 PolyForm 非商业许可，详见 NOTICE 文件。",
                ),
            ),
        )
    }
    AstraSheet(visible = about, onDismiss = { about = false }, title = "关于 Chizuru") {
        HelpContent(
            listOf(
                HelpSection(
                    "功能概要",
                    "将正在播放的歌曲歌词送至星河岛，并将剪贴板中的链接、电话号码与地址送至星河岛。",
                ),
                HelpSection(
                    "歌词",
                    "按 LyricON 的「歌词提供者」协议交付星流。能够读取播放器自身缓存的歌词时不访问网络，且不改变播放器的任何行为。",
                ),
                HelpSection(
                    "链接",
                    "使用星河岛官方接入库 astraisland-sdk 0.1.0 将跳转目标作为待发意图交给星河岛，由星河岛直接打开，不依赖本插件进程存活。",
                ),
                HelpSection(
                    "全程本机",
                    "不联网、不上传。安装完成后，请在 LSPosed 中勾选作用域，并重启目标应用一次。",
                ),
            ),
        )
    }
}