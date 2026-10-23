package com.astraflow.Chizuru.ui.page

internal class Contributor(
    val name: String,
    val avatar: String,
    val link: String,
    val summary: String = "",
    val chips: List<String> = emptyList(),
    val action: String = "",
)

internal val CONTRIBUTORS = listOf(
    Contributor(
        "星流",
        "xingliu.png",
        "https://github.com/Xposed-Modules-Repo/com.astraflow.tool",
        summary = "开发提供平台接入库",
        chips = listOf("真神", "伟大的星河岛"),
    ),
    Contributor(
        "星穹",
        "galaxy.png",
        "https://github.com/2979208016/AstraGalaxy",
        summary = "歌词能力/链接上岛雏形提供",
        chips = listOf("作者:依依", "歌词能力起源", "链接上岛起源"),
    ),
    Contributor("开发人员", "p1.jpg", "", chips = listOf("开发"), action = "暂无链接"),
    Contributor("开发人员", "p2.jpg", "", chips = listOf("开发", "测试"), action = "暂无链接"),
    Contributor(
        "穆远星",
        "p3.jpg",
        "https://www.coolapk.com/u/28719807",
        action = "在酷安上查看",
        summary = "星流作者",
        chips = listOf("星流创始人", "钻研"),
    ),
    Contributor("开发人员", "p4.jpg", "", chips = listOf("测试", "反馈"), action = "暂无链接"),
    Contributor(
        "江南",
        "p5.jpg",
        "https://github.com/JiangNanOvO/ChizuruMizuhara",
        summary = "作者之一",
        chips = listOf("作者", "维护"),
    ),
    Contributor(
        "千愁",
        "p6.png",
        "",
        summary = "作者之一",
        chips = listOf("作者", "设计", "开发"),
        action = "暂无链接",
    ),
    Contributor(
        "Lil sakee",
        "p7.jpg",
        "",
        summary = "用户反馈者先锋",
        chips = listOf("用户", "反馈", "测试"),
        action = "保护用户隐私无开放链接",
    ),
)

internal const val CONTRIBUTOR_WEBSITE = "https://astraflow.cc/island/"

internal const val CONTRIBUTOR_MAIL = "271400001@qq.com"

internal const val CONTRIBUTOR_GITHUB = "https://github.com/JiangNanOvO/ChizuruMizuhara"
