package com.astraflow.Chizuru.ui.page

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.ui.ios.CupertinoShapes
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.ios.Surfaces
import com.astraflow.Chizuru.ui.ios.clipRounded
import com.astraflow.Chizuru.ui.widget.Motion
import com.astraflow.Chizuru.ui.widget.UIKit

class SheetActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()

        val root = FrameLayout(this).apply {
            setBackgroundColor(0x66000000)
            setOnClickListener { finish() }
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Surfaces.rounded(
                ui.colors.tertiarySystemBackground,
                CupertinoShapes.EXTRA_LARGE,
            )
            clipRounded(CupertinoShapes.EXTRA_LARGE)
            isClickable = true
            setOnClickListener { }
        }

        card.addView(
            View(this).apply {
                background = Surfaces.rounded(ui.colors.quaternaryLabel, 3f)
                layoutParams = LinearLayout.LayoutParams(ui.dp(38), ui.dp(5)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = ui.dp(10)
                }
            },
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                ui.dp(SectionTokens.HORIZONTAL_PADDING),
                ui.dp(14),
                ui.dp(12),
                ui.dp(10),
            )
        }
        header.addView(ui.rowTitle(title), ui.weight())
        header.addView(
            ui.text(getString(R.string.ui_done), 17f, R.color.accent).apply {
                setPadding(ui.dp(12), ui.dp(6), ui.dp(6), ui.dp(6))
                isClickable = true
                setOnClickListener { finish() }
            },
        )
        card.addView(header)

        card.addView(
            ScrollView(this).apply {
                isFillViewport = false
                addView(
                    ui.text(body, 15f, R.color.text).apply {
                        setPadding(
                            ui.dp(SectionTokens.HORIZONTAL_PADDING),
                            0,
                            ui.dp(SectionTokens.HORIZONTAL_PADDING),
                            ui.dp(26),
                        )
                    },
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        root.addView(
            card,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.BOTTOM
                marginStart = ui.dp(SectionTokens.INLINE_PADDING.toInt())
                marginEnd = ui.dp(SectionTokens.INLINE_PADDING.toInt())
                bottomMargin = ui.dp(SectionTokens.INLINE_PADDING.toInt())
            },
        )

        card.translationY = ui.dp(80).toFloat()
        card.alpha = 0f
        card.animate()
            .translationY(0f).alpha(1f)
            .setDuration(280)
            .setInterpolator(Motion.EMPHASIS)
            .start()

        return root
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.sheet_out)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }
}
