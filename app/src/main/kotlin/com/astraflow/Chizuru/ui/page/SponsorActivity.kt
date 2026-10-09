package com.astraflow.Chizuru.ui.page

import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.ui.widget.Motion
import com.astraflow.Chizuru.ui.widget.UIKit

class SponsorActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        setTheme(R.style.Theme_Mitian_Detail)
        setContentView(wrapPage(scrollable(buildContent())))
    }

    private fun buildContent(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, ui.dp(40))
        }
        root.addView(Toolbar.build(this, ui, null) { finish() })


        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(18), 0, ui.dp(18), 0)
        }
        root.addView(body)
        body.addView(ui.text(getString(R.string.sponsor_title), 26f, R.color.text, bold = true))
        body.addView(ui.gap(18))

        val qrCard = ui.glassCard()
        qrCard.setPadding(ui.dp(16), ui.dp(18), ui.dp(16), ui.dp(18))
        val qrBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        qrBox.addView(
            ui.text(getString(R.string.sponsor_scan), 13f, R.color.text_sub)
        )
        qrBox.addView(ui.gap(12))
        qrBox.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.sponsor_qr)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            },
            LinearLayout.LayoutParams(ui.dp(220), ui.dp(220))
        )
        qrBox.addView(ui.gap(10))
        qrBox.addView(
            ui.text(getString(R.string.sponsor_note), 11f, R.color.text_hint).apply {
                gravity = Gravity.CENTER
                setLineSpacing(ui.dp(4).toFloat(), 1.12f)
            }
        )
        qrCard.addView(qrBox)
        body.addView(qrCard)
        Motion.fadeInUp(qrCard, 0L)

        body.addView(ui.gap(16))
        val textCard = ui.glassCard()
        textCard.setPadding(ui.dp(18), ui.dp(18), ui.dp(18), ui.dp(18))
        textCard.addView(
            ui.text(getString(R.string.sponsor_statement), 12.5f, R.color.text).apply {
                setLineSpacing(ui.dp(6).toFloat(), 1.2f)
            }
        )
        body.addView(textCard)
        Motion.fadeInUp(textCard, 60L)

        return root
    }
}
