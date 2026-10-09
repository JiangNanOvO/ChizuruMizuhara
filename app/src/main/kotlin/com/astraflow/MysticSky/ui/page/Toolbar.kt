package com.astraflow.MysticSky.ui.page

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.astraflow.MysticSky.R
import com.astraflow.MysticSky.ui.widget.Motion
import com.astraflow.MysticSky.ui.widget.UIKit

object Toolbar {

    fun build(
        activity: BaseActivity,
        ui: UIKit,
        title: String?,
        onBack: () -> Unit
    ): View {
        val bar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ui.dp(12), ui.dp(10), ui.dp(18), ui.dp(6))
        }

        val back = android.widget.FrameLayout(activity).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFFFFFFFF.toInt())
                setStroke(ui.dp(1), ui.color(R.color.divider))
            }
            layoutParams = LinearLayout.LayoutParams(ui.dp(36), ui.dp(36))
            isClickable = true
            setOnClickListener { onBack() }
            clipToOutline = true
        }
        back.addView(
            ui.icon(R.drawable.ic_chevron, R.color.text, 22).apply {
                rotation = 180f
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    ui.dp(22), ui.dp(22)
                ).apply { gravity = Gravity.CENTER }
            }
        )
        Motion.pressFeedback(back)
        bar.addView(back)

        if (!title.isNullOrBlank()) {
            val label: TextView = ui.text(title, 16f, R.color.text, bold = true).apply {
                setPadding(ui.dp(12), 0, 0, 0)
            }
            bar.addView(label, ui.weight())
        }

        return bar
    }
}
