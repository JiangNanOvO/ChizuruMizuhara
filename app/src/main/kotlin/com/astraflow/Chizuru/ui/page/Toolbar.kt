package com.astraflow.Chizuru.ui.page

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.ui.ios.Surfaces
import com.astraflow.Chizuru.ui.widget.Motion
import com.astraflow.Chizuru.ui.widget.UIKit

object Toolbar {

    fun build(
        activity: BaseActivity,
        ui: UIKit,
        title: String?,
        onBack: () -> Unit,
        onHelp: (() -> Unit)? = null,
    ): View {
        val bar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(4))
        }

        bar.addView(backButton(activity, ui, onBack))

        if (!title.isNullOrBlank()) {
            bar.addView(
                ui.text(title, 17f, R.color.text, bold = true).apply {
                    setPadding(ui.dp(10), 0, 0, 0)
                },
                ui.weight(),
            )
        } else if (onHelp != null) {
            bar.addView(android.view.View(activity), ui.weight())
        }
        if (onHelp != null) {
            bar.addView(ui.helpButton(onHelp))
        }
        return bar
    }

    private fun backButton(activity: BaseActivity, ui: UIKit, onBack: () -> Unit): FrameLayout {
        val button = FrameLayout(activity).apply {
            background = Surfaces.circle(ui.colors.tertiarySystemBackground)
            layoutParams = LinearLayout.LayoutParams(ui.dp(36), ui.dp(36))
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.ui_back)
            setOnClickListener { onBack() }
        }
        button.addView(
            ui.icon(R.drawable.ic_chevron, R.color.text, 18).apply {
                rotation = 180f
                layoutParams = FrameLayout.LayoutParams(ui.dp(18), ui.dp(18)).apply {
                    gravity = Gravity.CENTER
                }
            },
        )
        Motion.pressFeedback(button)
        return button
    }
}
