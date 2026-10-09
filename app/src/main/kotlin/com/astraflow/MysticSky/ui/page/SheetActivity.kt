package com.astraflow.MysticSky.ui.page

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.astraflow.MysticSky.R
import com.astraflow.MysticSky.ui.widget.Motion
import com.astraflow.MysticSky.ui.widget.UIKit

class SheetActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()

        val root = FrameLayout(this)
        root.setBackgroundColor(0x99000000.toInt())

        root.setOnClickListener { finish() }

        val card = ui.glassCard().apply {
            background = getDrawable(R.drawable.bg_glass_card)
            setPadding(ui.dp(20), ui.dp(10), ui.dp(20), ui.dp(26))
            isClickable = true
            setOnClickListener {  }
        }

        val handle = View(this).apply {
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
        card.addView(ui.gap(18))

        card.addView(ui.text(title, 20f, R.color.text, bold = true))
        card.addView(ui.gap(12))

        val text = ui.text(body, 14f, R.color.text_sub).apply {
            setLineSpacing(ui.dp(7).toFloat(), 1.15f)
        }
        card.addView(text)

        val scroller = ScrollView(this).apply {
            addView(card)
        }

        root.addView(
            scroller,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.BOTTOM }
        )

        card.translationY = ui.dp(60).toFloat()
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
