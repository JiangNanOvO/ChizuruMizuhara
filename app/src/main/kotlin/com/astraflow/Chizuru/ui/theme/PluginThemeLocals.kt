package com.astraflow.Chizuru.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit

/**
 * 插件界面用的全局开关。星流里这几样跟着「设置 → 主题与显示」走;插件没有那几页,取默认值:
 * 玻璃取页面画面做模糊、界面缩放 100%、字体跟随系统。留着同样的名字,抄来的部件不用改写法。
 */
val LocalEnableBlur = staticCompositionLocalOf { true }

val LocalUiScale = staticCompositionLocalOf { 1f }

/** 插件不内置字体,这一下什么也不换(只为与星流那边的写法一致)。 */
fun TextStyle.withAstraFont(): TextStyle = this

/** 界面缩放的换算:1.0 是 100%。 */
fun scaledSp(size: TextUnit, scale: Float): TextUnit = size * scale
