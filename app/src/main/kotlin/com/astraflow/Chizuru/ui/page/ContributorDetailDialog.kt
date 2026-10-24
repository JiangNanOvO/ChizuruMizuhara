package com.astraflow.Chizuru.ui.page

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.ui.ios.CupertinoShapes
import com.astraflow.Chizuru.ui.ios.Surfaces
import com.astraflow.Chizuru.ui.ios.clipRounded
import com.astraflow.Chizuru.ui.widget.AvatarView
import com.astraflow.Chizuru.ui.widget.UIKit

internal class ContributorDetailDialog(
    private val host: BaseActivity,
    private val item: Contributor,
) : Dialog(host) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = UIKit(host)
        val pad = ui.dp(24)

        val card = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = Surfaces.rounded(ui.colors.tertiarySystemBackground, CupertinoShapes.EXTRA_LARGE)
            clipRounded(CupertinoShapes.EXTRA_LARGE)
            setPadding(pad, pad, pad, pad + host.bottomInset())
        }

        if (item.name.isNotBlank()) {
            card.addView(
                ui.text(item.name, 20f, R.color.text, bold = true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, ui.dp(2), 0, 0)
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            card.addView(ui.gap(18))
        }

        card.addView(
            AvatarView(host).apply {
                layoutParams = LinearLayout.LayoutParams(ui.dp(96), ui.dp(96))
                    .apply { gravity = Gravity.CENTER_HORIZONTAL }
                load("avatars/${item.avatar}")
            },
        )
        card.addView(ui.gap(24))

        if (item.summary.isNotBlank()) {
            card.addView(
                ui.text(item.summary, 15f, R.color.text_sub).apply {
                    setLineSpacing(ui.dp(4).toFloat(), 1f)
                },
            )
            card.addView(ui.gap(16))
        }

        if (item.chips.isNotEmpty()) {
            for (row in item.chips.chunked(3)) {
                val line = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
                for ((index, chip) in row.withIndex()) {
                    line.addView(
                        chipView(host, chip),
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply { if (index > 0) marginStart = ui.dp(8) },
                    )
                }
                card.addView(line)
                card.addView(ui.gap(8))
            }
            card.addView(ui.gap(10))
        }

        card.addView(
            pill(
                host,
                actionLabel(),
                filled = true,
                enabled = item.link.isNotBlank(),
            ) { openGithub() },
        )
        card.addView(ui.gap(10))
        card.addView(
            pill(
                host,
                host.getString(R.string.contributors_close),
                filled = false,
            ) { dismiss() },
        )

        setContentView(card)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            attributes = attributes.apply {
                dimAmount = 0.45f
            }
        }
        setCanceledOnTouchOutside(true)
    }

    private fun actionLabel(): String {
        return if (item.link.contains("github.com", ignoreCase = true)) {
            host.getString(R.string.contributors_view_github)
        } else {
            host.getString(R.string.contributors_open_link)
        }
    }

    private fun chipView(host: BaseActivity, label: String): View {
        val ui = UIKit(host)
        return ui.text(label, 13f, R.color.text).apply {
            gravity = Gravity.CENTER
            background = Surfaces.rounded(ui.colors.secondarySystemFill, 12f)
            setPadding(ui.dp(14), ui.dp(9), ui.dp(14), ui.dp(9))
        }
    }

    private fun pill(
        host: BaseActivity,
        label: String,
        filled: Boolean,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ): View {
        val ui = UIKit(host)
        val colours = ui.colors
        val fill = if (filled) colours.accent else colours.secondarySystemFill
        val textColour = when {
            !enabled -> R.color.text_hint
            filled -> R.color.white
            else -> R.color.text
        }
        val press = if (colours.isDark) 0x33FFFFFF else 0x33000000
        val background = RippleDrawable(
            ColorStateList.valueOf(press),
            Surfaces.rounded(fill, 26f),
            Surfaces.rounded(Color.WHITE, 26f),
        )
        return ui.text(label, 17f, textColour, bold = filled).apply {
            gravity = Gravity.CENTER
            setPadding(0, ui.dp(16), 0, ui.dp(16))
            if (enabled) {
                setBackground(background)
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            } else {
                setBackground(Surfaces.rounded(fill, 26f))
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun openGithub() {
        if (item.link.isBlank()) {
            host.toast(host.getString(R.string.contributors_no_link))
            return
        }
        val opened = runCatching {
            host.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(item.link),
                ),
            )
        }.isSuccess
        if (opened) {
            dismiss()
        } else {
            host.toast(host.getString(R.string.ui_msg_copied))
        }
    }
}
