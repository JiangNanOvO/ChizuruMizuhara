package com.astraflow.Chizuru.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.astraflow.Chizuru.R

class UIKit(private val context: Context) {

    val density: Float get() = context.resources.displayMetrics.density

    fun dp(value: Number): Int = (value.toFloat() * density).toInt()

    fun color(res: Int): Int = context.getColor(res)

    fun text(
        value: CharSequence,
        sizeSp: Float,
        colorRes: Int,
        bold: Boolean = false
    ): TextView = TextView(context).apply {
        text = value
        textSize = sizeSp
        setTextColor(color(colorRes))
        if (bold) typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
        setLineSpacing(dp(3).toFloat(), 1.0f)
    }

    fun glassCard(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = context.getDrawable(R.drawable.bg_glass_card)
        setPadding(dp(6), dp(6), dp(6), dp(6))
        clipToOutline = true
        outlineProvider = ViewOutlineProvider.BACKGROUND
        elevation = dp(1.5f).toFloat()
    }

    fun innerBox(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = context.getDrawable(R.drawable.bg_glass_inner)
        clipToOutline = true
    }

    fun row(
        iconRes: Int?,
        title: CharSequence,
        subtitle: CharSequence? = null,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(11), dp(16), dp(11))
            minimumHeight = dp(48)
        }
        if (iconRes != null) row.addView(icon(iconRes, R.color.text))
        row.addView(column(title, subtitle), weight())
        if (onClick != null) {
            row.isClickable = true
            row.background = context.getDrawable(R.drawable.bg_row_press)
            row.setOnClickListener { onClick() }
            Motion.pressFeedback(row)
        }
        return row
    }

    fun column(title: CharSequence, subtitle: CharSequence?): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
            addView(text(title, 15.5f, R.color.text))
            if (!subtitle.isNullOrBlank()) {
                addView(text(subtitle, 12f, R.color.text_sub).apply { setPadding(0, dp(3), 0, 0) })
            }
        }

    fun chevron(): ImageView = icon(R.drawable.ic_chevron, R.color.text_hint)

    fun icon(res: Int, tintRes: Int, sizeDp: Int = 22): ImageView =
        ImageView(context).apply {
            setImageResource(res)
            imageTintList = ColorStateList.valueOf(color(tintRes))
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

    fun iconBadge(iconRes: Int, tintRes: Int): FrameLayout {
        val badge = FrameLayout(context).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(9).toFloat()
                setColor(color(tintRes))
            }
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
            clipToOutline = true
        }
        badge.addView(
            icon(iconRes, R.color.white, 18).apply {
                layoutParams = FrameLayout.LayoutParams(dp(18), dp(18)).apply {
                    gravity = Gravity.CENTER
                }
            }
        )
        return badge
    }

    fun sectionHeader(title: CharSequence): TextView = text(title, 12f, R.color.text_sub).apply {
        setPadding(dp(16), dp(20), dp(16), dp(7))
    }

    fun divider(startInsetDp: Int = 62): View = View(context).apply {
        setBackgroundColor(color(R.color.divider))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ).apply {
            marginStart = dp(startInsetDp)
            marginEnd = dp(0)
        }
    }


    fun pageHeader(title: CharSequence, onHelp: (() -> Unit)? = null): LinearLayout {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            text(title, 26f, R.color.text, bold = true),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        if (onHelp != null) header.addView(helpButton(onHelp))
        return header
    }

    fun helpButton(onClick: () -> Unit): TextView = TextView(context).apply {
        text = "?"
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(color(R.color.text_sub))
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(0xFFFFFFFF.toInt())
            setStroke(dp(1), color(R.color.divider))
        }
        layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
        isClickable = true
        setOnClickListener { onClick() }
    }

    fun valueRow(
        title: CharSequence,
        value: CharSequence? = null,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(13), dp(18), dp(13))
        }
        row.addView(text(title, 15.5f, R.color.text), weight())
        if (!value.isNullOrBlank()) {
            row.addView(text(value, 15.5f, R.color.text_sub))
        }
        if (onClick != null) {
            row.isClickable = true
            row.background = context.getDrawable(R.drawable.bg_row_press)
            row.setOnClickListener { onClick() }
        }
        return row
    }

    fun actionRow(title: CharSequence, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(13), dp(18), dp(13))
            isClickable = true
            background = context.getDrawable(R.drawable.bg_row_press)
            setOnClickListener { onClick() }
        }
        row.addView(text(title, 15.5f, R.color.accent), weight())
        return row
    }

    fun switchRow(
        title: CharSequence,
        checked: Boolean,
        onToggle: (Boolean) -> Unit,
        onOpen: (() -> Unit)? = null
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(11), dp(18), dp(11))
        }
        row.addView(text(title, 15.5f, R.color.text), weight())
        row.addView(GlassSwitch(context).apply {
            setChecked(checked, animate = false)
            onCheckedChange = { value -> onToggle(value) }
        })
        if (onOpen != null) {
            row.isClickable = true
            row.background = context.getDrawable(R.drawable.bg_row_press)
            row.setOnClickListener { onOpen() }
        }
        return row
    }

    fun noteText(body: CharSequence): TextView =
        text(body, 11.5f, R.color.text_hint).apply {
            setLineSpacing(dp(5).toFloat(), 1.15f)
        }

    fun gap(heightDp: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    fun weight(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    fun pill(label: CharSequence, colorRes: Int = R.color.accent): TextView =
        text(label, 12f, colorRes).apply {
            background = context.getDrawable(R.drawable.bg_pill)
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }

    fun statusDot(colorRes: Int = R.color.ok): View {
        val dot = View(context).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(color(colorRes))
            }
            layoutParams = LinearLayout.LayoutParams(dp(8), dp(8))
        }
        Motion.breathe(dot)
        return dot
    }
}
