package com.astraflow.Chizuru.ui.page

/**
 * 贡献者一位:名字、头像文件（assets/avatars 下）、可选外链、简介与说明标签。
 * 顺序沿用原来的排法：星流、星穹，随后是开发与测试，再是穆远星、江南、千愁与最早反馈的用户。
 */
class Contributor(
    val name: String,
    val avatar: String,
    val link: String = "",
    val summary: String = "",
    val chips: List<String> = emptyList(),
)

internal val CONTRIBUTORS = listOf(
    Contributor(
        name = "星流",
        avatar = "xingliu.png",
        link = "https://github.com/Xposed-Modules-Repo/com.astraflow.tool",
        summary = "提供星河岛接入平台与接入库",
        chips = listOf("星河岛", "接入库"),
    ),
    Contributor(
        name = "星穹",
        avatar = "galaxy.png",
        link = "https://github.com/2979208016/AstraGalaxy",
        summary = "歌词能力与链接上岛的雏形",
        chips = listOf("作者：依依", "歌词能力", "链接上岛"),
    ),
    Contributor(
        name = "开发人员",
        avatar = "p1.jpg",
        summary = "参与插件开发",
        chips = listOf("开发"),
    ),
    Contributor(
        name = "开发人员",
        avatar = "p2.jpg",
        summary = "参与插件开发与测试",
        chips = listOf("开发", "测试"),
    ),
    Contributor(
        name = "穆远星",
        avatar = "p3.jpg",
        link = "https://www.coolapk.com/u/28719807",
        summary = "星流作者",
        chips = listOf("星流", "星河岛"),
    ),
    Contributor(
        name = "开发人员",
        avatar = "p4.jpg",
        summary = "参与测试与问题反馈",
        chips = listOf("测试", "反馈"),
    ),
    Contributor(
        name = "江南",
        avatar = "p5.jpg",
        link = "https://github.com/JiangNanOvO/ChizuruMizuhara",
        summary = "插件作者之一，负责维护",
        chips = listOf("作者", "维护"),
    ),
    Contributor(
        name = "千愁",
        avatar = "p6.png",
        summary = "插件作者之一，负责设计与开发",
        chips = listOf("作者", "设计", "开发"),
    ),
    Contributor(
        name = "Lil sakee",
        avatar = "p7.jpg",
        summary = "早期用户反馈与测试",
        chips = listOf("用户", "反馈", "测试"),
    ),
)


internal const val CONTRIBUTOR_WEBSITE = "https://astraflow.cc/island/"

internal const val CONTRIBUTOR_MAIL = "271400001@qq.com"

internal const val CONTRIBUTOR_GITHUB = "https://github.com/JiangNanOvO/ChizuruMizuhara"
