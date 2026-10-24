@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.astraflow.Chizuru.ui.kit.Astra
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.section.CupertinoSection
import zone.ien.hig.section.SectionItem
import zone.ien.hig.theme.CupertinoTheme

/** 使用说明的一段:小标题与正文;[prerequisite] 为真时是不满足就无法使用的前提,用橙色。 */
class HelpSection(val heading: String, val body: String, val prerequisite: Boolean = false)

/**
 * 使用说明的弹层内容:一组上游分组,每段一行带编号(和星流的使用说明同一种写法)。
 * 页面底色上不写说明文字,设置的作用、连带关系与例外都写在右上角问号打开的这一份里。
 */
@Composable
fun HelpContent(sections: List<HelpSection>) {
    val dark = CupertinoTheme.colorScheme.isDark
    CupertinoSection {
        sections.forEachIndexed { index, section ->
            SectionItem {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val color = if (section.prerequisite) Astra.colors.warning else CupertinoTheme.colorScheme.label
                    CupertinoText(
                        "${index + 1}. ${section.heading}",
                        style = CupertinoTheme.typography.headline,
                        color = color,
                    )
                    CupertinoText(section.body, style = CupertinoTheme.typography.subhead, color = color)
                }
            }
        }
    }
}
