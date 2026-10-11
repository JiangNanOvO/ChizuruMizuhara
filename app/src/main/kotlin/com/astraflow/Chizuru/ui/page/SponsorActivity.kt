package com.astraflow.Chizuru.ui.page

import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.widget.UIKit

class SponsorActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        setTheme(R.style.Theme_Mitian_Detail)
        render()
    }

    private fun render() {
        val (content, title) = buildContent()
        val scroll = scrollable(content)
        bindLargeTitle(scroll, title)
        setContentView(wrapPage(scroll))
    }

    private fun buildContent(): Pair<LinearLayout, TextView> {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ui.dp(SectionTokens.PAGE_INSET),
                ui.dp(8),
                ui.dp(SectionTokens.PAGE_INSET),
                ui.dp(40),
            )
        }
        root.addView(
            Toolbar.build(this, ui, null, { finish() }) {
                startActivity(
                    android.content.Intent(this, SheetActivity::class.java)
                        .putExtra(SheetActivity.EXTRA_TITLE, getString(R.string.sponsor_title))
                        .putExtra(SheetActivity.EXTRA_BODY, getString(R.string.sponsor_statement)),
                )
            },
        )

        val title = ui.largeTitle(getString(R.string.sponsor_title))
        root.addView(title)
        root.addView(ui.gap(18))

        val qrCard = ui.glassCard()
        val qrBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(ui.dp(16), ui.dp(20), ui.dp(16), ui.dp(20))
        }
        qrBox.addView(ui.rowNote(getString(R.string.sponsor_scan)))
        qrBox.addView(ui.gap(14))
        qrBox.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.sponsor_qr)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            },
            LinearLayout.LayoutParams(ui.dp(220), ui.dp(220)),
        )
        qrBox.addView(ui.gap(12))
        qrBox.addView(
            ui.footerNote(getString(R.string.sponsor_note)).apply {
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
            },
        )
        qrCard.addView(qrBox)
        root.addView(qrCard)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_about)))
        val textCard = ui.glassCard()
        textCard.addView(
            ui.text(getString(R.string.sponsor_statement), 15f, R.color.text).apply {
                setPadding(
                    ui.dp(SectionTokens.HORIZONTAL_PADDING),
                    ui.dp(16),
                    ui.dp(SectionTokens.HORIZONTAL_PADDING),
                    ui.dp(18),
                )
            },
        )
        root.addView(textCard)

        return root to title
    }
}
