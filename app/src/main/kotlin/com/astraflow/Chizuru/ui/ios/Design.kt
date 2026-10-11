package com.astraflow.Chizuru.ui.ios

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.TextView
import kotlin.math.abs

class CupertinoColors(
    val isDark: Boolean,
    val accent: Int,
    val label: Int,
    val secondaryLabel: Int,
    val tertiaryLabel: Int,
    val quaternaryLabel: Int,
    val link: Int,
    val separator: Int,
    val opaqueSeparator: Int,
    val systemBackground: Int,
    val secondarySystemBackground: Int,
    val tertiarySystemBackground: Int,
    val systemGroupedBackground: Int,
    val secondarySystemGroupedBackground: Int,
    val tertiarySystemGroupedBackground: Int,
    val systemFill: Int,
    val secondarySystemFill: Int,
    val tertiarySystemFill: Int,
    val quaternarySystemFill: Int,
    val systemGreen: Int,
    val systemOrange: Int,
    val systemRed: Int,
) {
    val switchTrackOff: Int get() = if (isDark) 0xFF39393D.toInt() else 0xFFE9E9EB.toInt()
    val switchTrackEdge: Int get() = if (isDark) 0x33FFFFFF else 0x14000000
    val switchKnob: Int get() = 0xFFFFFFFF.toInt()
    val switchKnobShadow: Int get() = 0x33000000

    companion object {

        val light = CupertinoColors(
            isDark = false,
            accent = 0xFF0088FF.toInt(),
            label = 0xFF000000.toInt(),
            secondaryLabel = 0x993C3C43.toInt(),
            tertiaryLabel = 0x4C3C3C43.toInt(),
            quaternaryLabel = 0x2D3C3C43.toInt(),
            link = 0xFF007AFF.toInt(),
            separator = 0x1E3C3C43.toInt(),
            opaqueSeparator = 0xFFC6C6C8.toInt(),
            systemBackground = 0xFFFFFFFF.toInt(),
            secondarySystemBackground = 0xFFF2F2F7.toInt(),
            tertiarySystemBackground = 0xFFFFFFFF.toInt(),
            systemGroupedBackground = 0xFFF2F2F7.toInt(),
            secondarySystemGroupedBackground = 0xFFFFFFFF.toInt(),
            tertiarySystemGroupedBackground = 0xFFF2F2F7.toInt(),
            systemFill = 0x33787880,
            secondarySystemFill = 0x28787880,
            tertiarySystemFill = 0x1E767680,
            quaternarySystemFill = 0x14747480,
            systemGreen = 0xFF34C759.toInt(),
            systemOrange = 0xFFFF8D28.toInt(),
            systemRed = 0xFFFF383C.toInt(),
        )

        val dark = CupertinoColors(
            isDark = true,
            accent = 0xFF0091FF.toInt(),
            label = 0xFFFFFFFF.toInt(),
            secondaryLabel = 0x99EBEBF5.toInt(),
            tertiaryLabel = 0x4CEBEBF5.toInt(),
            quaternaryLabel = 0x28EBEBF5.toInt(),
            link = 0xFF0984FF.toInt(),
            separator = 0x7F545458.toInt(),
            opaqueSeparator = 0xFF38383A.toInt(),
            systemBackground = 0xFF000000.toInt(),
            secondarySystemBackground = 0xFF1C1C1E.toInt(),
            tertiarySystemBackground = 0xFF2C2C2E.toInt(),
            systemGroupedBackground = 0xFF000000.toInt(),
            secondarySystemGroupedBackground = 0xFF1C1C1E.toInt(),
            tertiarySystemGroupedBackground = 0xFF2C2C2E.toInt(),
            systemFill = 0x5C787880,
            secondarySystemFill = 0x52787880,
            tertiarySystemFill = 0x3D767680,
            quaternarySystemFill = 0x2D747480,
            systemGreen = 0xFF30D158.toInt(),
            systemOrange = 0xFFFF9230.toInt(),
            systemRed = 0xFFFF4245.toInt(),
        )

        fun systemGreen(dark: Boolean): Int = if (dark) 0xFF30D158.toInt() else 0xFF34C759.toInt()
        fun systemRed(dark: Boolean): Int = if (dark) 0xFFFF4245.toInt() else 0xFFFF383C.toInt()
    }
}

class CupertinoTypography {
    class Spec(val sizeSp: Float, val lineHeightSp: Float, val bold: Boolean = false)

    val largeTitle = Spec(34f, 41f)
    val headline = Spec(17f, 22f, bold = true)
    val body = Spec(17f, 22f)
    val subhead = Spec(15f, 20f)
    val footnote = Spec(13f, 18f)
}

object CupertinoShapes {
    const val MEDIUM = 12f

    const val EXTRA_LARGE = 24f
}

object SectionTokens {

    const val SPLIT_PADDING = 12f
    const val INLINE_PADDING = 6f

    const val HORIZONTAL_PADDING = 18f
    const val VERTICAL_PADDING = 8f

    const val MIN_HEIGHT = 45f

    const val PAGE_INSET = SPLIT_PADDING

    const val HEADER_INSET = HORIZONTAL_PADDING

    const val DIVIDER_PADDING = HORIZONTAL_PADDING
}

object SwitchTokens {
    const val WIDTH = 51
    const val HEIGHT = 31
    const val THUMB_PADDING = 2
}

class Theme(
    val colors: CupertinoColors,
    val typography: CupertinoTypography,
) {
    companion object {
        @Volatile
        private var light: Theme? = null

        @Volatile
        private var dark: Theme? = null

        fun of(context: Context): Theme = if (isDarkMode(context)) {
            dark ?: Theme(CupertinoColors.dark, CupertinoTypography()).also { dark = it }
        } else {
            light ?: Theme(CupertinoColors.light, CupertinoTypography()).also { light = it }
        }

        fun isDarkMode(context: Context): Boolean =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }
}

object ThemeTag {
    private val KEY = "chizuru:theme".hashCode()

    fun attach(view: View) {
        view.setTag(KEY, Theme.of(view.context))
    }

}

object Surfaces {

    fun groupedCard(colors: CupertinoColors): Drawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = CupertinoShapes.EXTRA_LARGE
            setColor(colors.secondarySystemGroupedBackground)
        }

    fun divider(colors: CupertinoColors): Drawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(colors.separator)
        }

    fun ripple(colors: CupertinoColors, radiusDp: Float): Drawable {
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp
            setColor(Color.WHITE)
        }
        val content = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp
            setColor(Color.TRANSPARENT)
        }
        val press = if (colors.isDark) 0x1FFFFFFF else 0x14000000
        return RippleDrawable(ColorStateList.valueOf(press), content, mask)
    }

    fun circle(color: Int): Drawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }

    fun rounded(color: Int, radiusDp: Float): Drawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp
            setColor(color)
        }
}

fun TextView.applySpec(spec: CupertinoTypography.Spec, color: Int): TextView {
    textSize = spec.sizeSp
    setTextColor(color)
    typeface = if (spec.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    includeFontPadding = false
    val extra = ((spec.lineHeightSp - spec.sizeSp) * resources.displayMetrics.scaledDensity)
        .coerceAtLeast(0f)
    setLineSpacing(extra, 1f)
    return this
}

fun View.clipRounded(radiusDp: Float) {
    val radius = radiusDp * resources.displayMetrics.density
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(v: View, outline: Outline) {
            outline.setRoundRect(0, 0, v.width, v.height, radius)
        }
    }
    clipToOutline = true
}
