package com.astraflow.Chizuru.ui.kit

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import zone.ien.hig.theme.ColorScheme
import zone.ien.hig.theme.CupertinoColors
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.Typography
import zone.ien.hig.theme.darkColorScheme
import zone.ien.hig.theme.lightColorScheme
import zone.ien.hig.theme.systemGreen
import zone.ien.hig.theme.systemOrange
import zone.ien.hig.theme.systemRed

/**
 * 星流界面的取色:全部取上游 compose-hig 的 iOS 颜色(页面底、卡片、文字、分隔线、主题色等),不自己定数值。
 * 只有上游没有的几样(玻璃小块的淡色与细边、弹层后面的压暗、影子)在这里给。
 */
@Immutable
data class AstraColors(
    val isDark: Boolean,
    /** 页面底色。 */
    val background: Color,
    /** 分组卡片、列表行的底色。 */
    val card: Color,
    /** 行被按住时的底色。 */
    val cardPressed: Color,
    /** 浅色按钮、小标记的底色。 */
    val fill: Color,
    /** 进度条未走过的一段。 */
    val track: Color,
    val label: Color,
    val labelSecondary: Color,
    val labelTertiary: Color,
    val separator: Color,
    /** 文字和图标用的主题色。 */
    val accent: Color,
    /** 实心按钮用的主题色。 */
    val accentFill: Color,
    val onAccent: Color,
    /** 开关打开的颜色。 */
    val positive: Color,
    val warning: Color,
    val danger: Color,
    /** 玻璃小块的淡色、边缘和高光。 */
    val glassTint: Color,
    val glassEdge: Color,
    val glassHighlight: Color,
    /** 底栏里当前标签的底色。 */
    val tabSelected: Color,
    /** 弹层后面压暗的颜色。 */
    val scrim: Color,
    /** 底部弹层的底色。 */
    val sheet: Color,
    /** 顶部栏的底色。 */
    val bar: Color,
    val shadow: Color,
)

private fun colorsFrom(scheme: ColorScheme): AstraColors {
    val dark = scheme.isDark
    return AstraColors(
        isDark = dark,
        background = scheme.systemGroupedBackground,
        card = scheme.secondarySystemGroupedBackground,
        cardPressed = scheme.systemFill,
        fill = scheme.tertiarySystemFill,
        track = scheme.secondarySystemFill,
        label = scheme.label,
        labelSecondary = scheme.secondaryLabel,
        labelTertiary = scheme.tertiaryLabel,
        separator = scheme.separator,
        accent = scheme.accent,
        accentFill = scheme.accent,
        onAccent = Color.White,
        positive = CupertinoColors.systemGreen(dark),
        warning = CupertinoColors.systemOrange(dark),
        danger = CupertinoColors.systemRed(dark),
        glassTint = if (dark) Color(0x8C28282C) else Color(0x99FFFFFF),
        glassEdge = if (dark) Color(0x24FFFFFF) else Color(0x1F000000),
        glassHighlight = if (dark) Color(0x33FFFFFF) else Color(0xF2FFFFFF),
        tabSelected = scheme.tertiarySystemFill,
        scrim = Color.Black.copy(alpha = 0.4f),
        sheet = scheme.secondarySystemBackground,
        bar = scheme.systemBackground,
        shadow = Color.Black.copy(alpha = if (dark) 0.55f else 0.12f),
    )
}

/** 上游的配色;[accent] 是用户自己选的主题色,没选时用上游的系统蓝。 */
private fun scheme(dark: Boolean, accent: Color?): ColorScheme = when {
    dark && accent != null -> darkColorScheme(accent = accent)
    dark -> darkColorScheme()
    accent != null -> lightColorScheme(accent = accent)
    else -> lightColorScheme()
}

fun astraLightColors(accent: Color? = null): AstraColors = colorsFrom(scheme(false, accent))

fun astraDarkColors(accent: Color? = null): AstraColors = colorsFrom(scheme(true, accent))

/** 星流的字号档:上游的字号档(iOS 的字号表)按 [ASTRA_TEXT_SCALE] 整体缩小,字体用手机系统字体。 */
@Immutable
data class AstraType(
    val largeTitle: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subheadline: TextStyle,
    val footnote: TextStyle,
    val caption: TextStyle,
    val caption2: TextStyle,
)

/**
 * 全局字号比例:上游的字号表(iOS)每一档都乘这个数,正文从 17 变 16
 * (用户 2026-10-04:「app中全局字体字号可以稍微小一些，现在太大看起来不舒服，但是不要太大」)。
 */
internal const val ASTRA_TEXT_SCALE = 16f / 17f

private fun TextStyle.scaled() = copy(
    fontSize = if (fontSize.isSpecified) fontSize * ASTRA_TEXT_SCALE else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * ASTRA_TEXT_SCALE else lineHeight,
)

private val HigTypography = Typography().let { t ->
    Typography(
        largeTitle = t.largeTitle.scaled(),
        title1 = t.title1.scaled(),
        title2 = t.title2.scaled(),
        title3 = t.title3.scaled(),
        headline = t.headline.scaled(),
        body = t.body.scaled(),
        callout = t.callout.scaled(),
        subhead = t.subhead.scaled(),
        footnote = t.footnote.scaled(),
        caption1 = t.caption1.scaled(),
        caption2 = t.caption2.scaled(),
    )
}

fun astraType(typography: Typography = HigTypography): AstraType = AstraType(
    largeTitle = typography.largeTitle,
    title1 = typography.title1,
    title2 = typography.title2,
    title3 = typography.title3,
    headline = typography.headline,
    body = typography.body,
    callout = typography.callout,
    subheadline = typography.subhead,
    footnote = typography.footnote,
    caption = typography.caption1,
    caption2 = typography.caption2,
)

val LocalAstraColors = staticCompositionLocalOf { astraLightColors() }
val LocalAstraType = staticCompositionLocalOf { astraType() }

/**
 * 星流界面的主题。[darkMode] 沿用设置里的取值:0 跟随系统、1 浅色、2/3 深色;[accent] 为空时用上游的系统蓝。
 */
@Composable
fun AstraTheme(
    darkMode: Int = 0,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val dark = when (darkMode) {
        1 -> false
        2, 3 -> true
        else -> isSystemInDarkTheme()
    }
    HigTheme(dark, accent, sheetHost = true, content)
}

/** 系统悬浮窗口内使用相同部件与配色，不创建页面和弹层骨架。 */
@Composable
fun AstraOverlayTheme(dark: Boolean, content: @Composable () -> Unit) =
    HigTheme(dark, accent = null, sheetHost = false, content)

/**
 * 交给上游 compose-hig 的主题:上游的配色(主题色按用户的选择)、上游的字号档。
 * 上游有几处部件按手机系统的深浅色取色,不看星流设置里选的深浅色;这里把界面读到的「系统深浅色」换成星流设置的那一种。
 * [sheetHost] 为真时在最外层放上游的弹层骨架(只在整个界面的最外层放一次)。
 */
@Composable
private fun HigTheme(dark: Boolean, accent: Color?, sheetHost: Boolean, content: @Composable () -> Unit) {
    val base = LocalConfiguration.current
    val configuration = remember(base, dark) {
        Configuration(base).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
    }
    val higScheme = remember(dark, accent) { scheme(dark, accent) }
    val colors = remember(higScheme) { colorsFrom(higScheme) }
    val typography = HigTypography
    val type = remember(typography) { astraType(typography) }
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalAstraColors provides colors,
        LocalAstraType provides type,
    ) {
        CupertinoTheme(colorScheme = higScheme, typography = typography) {
            if (sheetHost) AstraSheetHost(content) else content()
        }
    }
}

/**
 * 弹层里的配色(照上游示例):弹层底色是 secondarySystemBackground(浅色浅灰、深色深灰),
 * 弹层里的分组与卡片换成 tertiarySystemBackground(浅色白、深色稍亮的灰),卡片才看得出边界。
 */
@Composable
internal fun AstraSheetColors(content: @Composable () -> Unit) {
    val base = CupertinoTheme.colorScheme
    val scheme = remember(base) { base.copy(secondarySystemGroupedBackground = base.tertiarySystemBackground) }
    val colors = remember(scheme) { colorsFrom(scheme) }
    CompositionLocalProvider(LocalAstraColors provides colors) {
        CupertinoTheme(colorScheme = scheme, shapes = CupertinoTheme.shapes, typography = CupertinoTheme.typography, content = content)
    }
}

/**
 * 黑底沉浸页(图片、视频裁剪)里的部件一律用深色取色,主题色沿用用户的设置。
 */
@Composable
fun AstraForceDark(content: @Composable () -> Unit) {
    val current = Astra.colors
    if (current.isDark) {
        content()
        return
    }
    val custom = current.accent.takeIf { it != lightColorScheme().accent }
    HigTheme(true, custom, sheetHost = false, content)
}

/** 取当前主题的颜色和字号:`Astra.colors.card`、`Astra.type.body`。 */
object Astra {
    val colors: AstraColors
        @Composable @ReadOnlyComposable get() = LocalAstraColors.current
    val type: AstraType
        @Composable @ReadOnlyComposable get() = LocalAstraType.current
}