package com.astraflow.MysticSky.ui.page

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import com.astraflow.MysticSky.R
import com.astraflow.MysticSky.ui.widget.Motion
import com.astraflow.MysticSky.ui.widget.UIKit

object PickerSheet {

    fun show(
        activity: Activity,
        title: String,
        options: List<Pair<String, String>>,
        onPick: (String) -> Unit
    ) {
        val ui = UIKit(activity)

        val card = ui.glassCard().apply {
            setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(22))
        }

        val handle = View(activity).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = ui.dp(3).toFloat()
                setColor(0x55FFFFFF)
            }
            layoutParams = LinearLayout.LayoutParams(ui.dp(38), ui.dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        card.addView(handle)
        card.addView(ui.gap(14))
        card.addView(
            ui.text(title, 15f, R.color.text, bold = true).apply {
                setPadding(ui.dp(8), 0, 0, 0)
            }
        )
        card.addView(ui.gap(10))

        val popup = PopupWindow(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        fun dismiss() {
            popup.dismiss()
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (options.isEmpty()) {
            content.addView(
                ui.text("没有可选项", 14f, R.color.text_hint).apply {
                    setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(14))
                }
            )
        }

        for ((key, label) in options) {
            val row = ui.row(null, label, null) {
                onPick(key)
                dismiss()
            }
            content.addView(row)
        }

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            addView(content)
        }

        card.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(360)
        ))

        popup.contentView = card
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))
        popup.animationStyle = 0

        popup.showAtLocation(activity.window.decorView, Gravity.BOTTOM, 0, 0)

        card.translationY = ui.dp(80).toFloat()
        card.animate().translationY(0f).setDuration(280)
            .setInterpolator(Motion.EMPHASIS).start()
    }

    fun showForBrowser(
        activity: Activity,
        options: List<Pair<String, String>>,
        onPick: (String) -> Unit
    ) = show(activity, activity.getString(R.string.link_browser_pick), options, onPick)
}
