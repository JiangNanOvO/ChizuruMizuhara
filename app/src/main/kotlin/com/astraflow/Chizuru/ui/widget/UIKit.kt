package com.astraflow.Chizuru.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.ui.ios.CupertinoColors
import com.astraflow.Chizuru.ui.ios.CupertinoShapes
import com.astraflow.Chizuru.ui.ios.CupertinoTypography
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.ios.Surfaces
import com.astraflow.Chizuru.ui.ios.Theme
import com.astraflow.Chizuru.ui.ios.applySpec
import com.astraflow.Chizuru.ui.ios.clipRounded

class UIKit(private val context: Context) {

    val theme: Theme = Theme.of(context)
    val colors: CupertinoColors get() = theme.colors
    val type: CupertinoTypography get() = theme.typography

    val density: Float get() = context.resources.displayMetrics.density

    fun dp(value: Number): Int = (value.toFloat() * density).toInt()

    fun color(res: Int): Int = when (res) {
        R.color.text -> colors.label
        R.color.text_sub -> colors.secondaryLabel
        R.color.text_hint -> colors.tertiaryLabel
        R.color.accent -> colors.accent
        R.color.accent_soft -> colors.quaternarySystemFill
        R.color.accent_glow -> colors.quaternarySystemFill
        R.color.ok -> colors.systemGreen
        R.color.warn -> colors.systemOrange
        R.color.off -> colors.tertiaryLabel
        R.color.purple -> colors.accent
        R.color.icon -> colors.tertiaryLabel
        R.color.divider -> colors.separator
        R.color.glass -> colors.secondarySystemGroupedBackground
        R.color.glass_strong -> colors.tertiarySystemGroupedBackground
        R.color.glass_inner -> colors.secondarySystemFill
        R.color.glass_border -> colors.separator
        R.color.bg -> colors.systemGroupedBackground
        R.color.bg_glow -> colors.systemGroupedBackground
        R.color.white -> 0xFFFFFFFF.toInt()
        else -> colors.label
    }

    fun text(
        value: CharSequence,
        sizeSp: Float,
        colorRes: Int,
        bold: Boolean = false,
    ): TextView = styled(
        value,
        CupertinoTypography.Spec(sizeSp, sizeSp * 1.3f, bold),
        color(colorRes),
    )

    private fun styled(
        value: CharSequence,
        spec: CupertinoTypography.Spec,
        color: Int,
    ): TextView = TextView(context).apply {
        applySpec(spec, color)
        text = value
    }

    fun largeTitle(value: CharSequence): TextView =
        styled(value, type.largeTitle, colors.label).apply {
            typeface = Typeface.DEFAULT_BOLD
        }

    fun rowTitle(value: CharSequence): TextView =
        styled(value, type.body, colors.label)

    fun rowNote(value: CharSequence): TextView =
        styled(value, type.footnote, colors.secondaryLabel)

    fun sectionHeader(title: CharSequence): TextView =
        styled(title, type.footnote, colors.secondaryLabel).apply {
            setPadding(dp(SectionTokens.HEADER_INSET), 0, dp(SectionTokens.HEADER_INSET), dp(7))
        }

    fun footerNote(body: CharSequence): TextView =
        styled(body, type.footnote, colors.tertiaryLabel).apply {
            setPadding(dp(SectionTokens.HEADER_INSET), 0, dp(SectionTokens.HEADER_INSET), 0)
        }

    fun glassCard(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = Surfaces.groupedCard(colors)
        clipRounded(CupertinoShapes.EXTRA_LARGE)
    }

    fun divider(startInsetDp: Int = SectionTokens.DIVIDER_PADDING.toInt()): View = View(context).apply {
        setBackgroundColor(colors.separator)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            density.toInt().coerceAtLeast(1),
        ).apply {
            marginStart = dp(startInsetDp)
        }
    }

    fun row(
        iconRes: Int?,
        title: CharSequence,
        subtitle: CharSequence? = null,
        value: CharSequence? = null,
        chevron: Boolean = false,
        onClick: (() -> Unit)?,
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(SectionTokens.MIN_HEIGHT)
            setPadding(
                dp(SectionTokens.HORIZONTAL_PADDING),
                dp(SectionTokens.VERTICAL_PADDING),
                dp(SectionTokens.HORIZONTAL_PADDING),
                dp(SectionTokens.VERTICAL_PADDING),
            )
        }

        if (iconRes != null) row.addView(icon(iconRes, R.color.text, 22))

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        column.addView(rowTitle(title))
        if (!subtitle.isNullOrBlank()) {
            column.addView(rowNote(subtitle).apply { setPadding(0, dp(2), 0, 0) })
        }
        row.addView(column, weight())

        if (!value.isNullOrBlank()) {
            row.addView(
                styled(value, type.body, colors.secondaryLabel),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = dp(8) },
            )
        }
        if (chevron) row.addView(chevron())

        if (onClick != null) {
            row.isClickable = true
            row.isFocusable = true
            row.background = Surfaces.ripple(colors, CupertinoShapes.EXTRA_LARGE)
            row.setOnClickListener { onClick() }
        }
        return row
    }

    fun valueRow(
        title: CharSequence,
        value: CharSequence? = null,
        onClick: (() -> Unit)? = null,
    ): LinearLayout = row(null, title, null, value, chevron = false, onClick = onClick)

    fun actionRow(title: CharSequence, onClick: () -> Unit): LinearLayout {
        val row = row(null, title, null, null, chevron = false, onClick = onClick)
        (row.getChildAt(0) as? TextView)?.applySpec(type.body, colors.accent)
        return row
    }

    fun switchRow(
        title: CharSequence,
        checked: Boolean,
        onToggle: (Boolean) -> Unit,
        onOpen: (() -> Unit)? = null,
        note: CharSequence? = null,
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(SectionTokens.MIN_HEIGHT)
            setPadding(
                dp(SectionTokens.HORIZONTAL_PADDING),
                dp(7),
                dp(SectionTokens.HORIZONTAL_PADDING),
                dp(7),
            )
        }

        if (note.isNullOrBlank()) {
            row.addView(styled(title, type.body, colors.label), weight())
        } else {
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            column.addView(styled(title, type.body, colors.label))
            column.addView(rowNote(note).apply { setPadding(0, dp(2), 0, 0) })
            row.addView(column, weight())
        }

        row.addView(
            ToggleSwitch(context).apply {
                setChecked(checked, animate = false)
                onCheckedChange = { value -> onToggle(value) }
            },
        )

        if (onOpen != null) {
            row.isClickable = true
            row.isFocusable = true
            row.background = Surfaces.ripple(colors, CupertinoShapes.EXTRA_LARGE)
            row.setOnClickListener { onOpen() }
        }
        return row
    }

    fun chevron(): ImageView = icon(R.drawable.ic_chevron, R.color.text_hint, 14).apply {
        layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply {
            marginStart = dp(SectionTokens.INLINE_PADDING.toInt())
        }
    }

    fun icon(res: Int, tintRes: Int, sizeDp: Int = 22): ImageView = ImageView(context).apply {
        setImageResource(res)
        imageTintList = ColorStateList.valueOf(color(tintRes))
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    fun helpButton(onClick: () -> Unit): TextView = TextView(context).apply {
        text = "?"
        applySpec(type.headline, colors.secondaryLabel)
        gravity = Gravity.CENTER
        background = Surfaces.circle(colors.tertiarySystemBackground)
        layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
        isClickable = true
        isFocusable = true
        contentDescription = "使用说明"
        setOnClickListener { onClick() }
    }

    fun statusDot(colorRes: Int = R.color.ok): View = View(context).apply {
        background = Surfaces.circle(color(colorRes))
        layoutParams = LinearLayout.LayoutParams(dp(8), dp(8))
    }

    fun gap(heightDp: Number): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    fun weight(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
}
