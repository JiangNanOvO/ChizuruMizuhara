package com.astraflow.Chizuru.ui.page

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.ui.ios.CupertinoShapes
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.ios.Surfaces
import com.astraflow.Chizuru.ui.ios.clipRounded
import com.astraflow.Chizuru.ui.widget.Motion
import com.astraflow.Chizuru.ui.widget.UIKit

object PickerSheet {

    fun show(
        activity: Activity,
        title: String,
        options: List<Pair<String, String>>,
        onPick: (String) -> Unit,
    ) {
        val ui = UIKit(activity)

        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = Surfaces.rounded(
                ui.colors.tertiarySystemBackground,
                CupertinoShapes.EXTRA_LARGE,
            )
            clipRounded(CupertinoShapes.EXTRA_LARGE)
        }

        card.addView(
            View(activity).apply {
                background = Surfaces.rounded(ui.colors.quaternaryLabel, 3f)
                layoutParams = LinearLayout.LayoutParams(ui.dp(38), ui.dp(5)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = ui.dp(10)
                }
            },
        )
        card.addView(
            ui.rowTitle(title).apply {
                setPadding(
                    ui.dp(SectionTokens.HORIZONTAL_PADDING),
                    ui.dp(14),
                    ui.dp(SectionTokens.HORIZONTAL_PADDING),
                    ui.dp(8),
                )
            },
        )

        val popup = PopupWindow(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (options.isEmpty()) {
            content.addView(
                ui.text(activity.getString(R.string.ui_empty_options), 15f, R.color.text_hint)
                    .apply {
                        setPadding(
                            ui.dp(SectionTokens.HORIZONTAL_PADDING),
                            ui.dp(14),
                            ui.dp(SectionTokens.HORIZONTAL_PADDING),
                            ui.dp(14),
                        )
                    },
            )
        }

        for ((key, label) in options) {
            content.addView(
                ui.row(
                    iconRes = null,
                    title = label,
                    chevron = false,
                    onClick = {
                        onPick(key)
                        popup.dismiss()
                    },
                ),
            )
        }

        card.addView(
            ScrollView(activity).apply {
                isFillViewport = false
                addView(content)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ui.dp(360),
            ),
        )
        card.addView(ui.gap(SectionTokens.INLINE_PADDING))

        popup.contentView = card
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.setBackgroundDrawable(ColorDrawable(0x66000000))
        popup.animationStyle = 0

        popup.showAtLocation(activity.window.decorView, Gravity.BOTTOM, 0, 0)

        card.translationY = ui.dp(80).toFloat()
        card.animate()
            .translationY(0f)
            .setDuration(280)
            .setInterpolator(Motion.EMPHASIS)
            .start()
    }

    fun showForBrowser(
        activity: Activity,
        options: List<Pair<String, String>>,
        onPick: (String) -> Unit,
    ) = show(activity, activity.getString(R.string.link_browser_pick), options, onPick)
}
