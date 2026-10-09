package com.astraflow.MysticSky.ui.page

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import com.astraflow.MysticSky.R
import com.astraflow.MysticSky.settings.ModulePrefs
import com.astraflow.MysticSky.ui.widget.GlassSwitch
import com.astraflow.MysticSky.ui.widget.Motion
import com.astraflow.MysticSky.ui.widget.UIKit
import android.os.Handler
import android.os.Looper
import com.astraflow.MysticSky.keepalive.KeepAliveService

class LyricDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        setTheme(R.style.Theme_Mitian_Detail)
        setContentView(wrapPage(scrollable(buildContent())))
    }

    private fun buildStandaloneCard(): LinearLayout {
        val prefs = ModulePrefs.of(this)
        val card = ui.glassCard()
        card.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16))
        val on = ModulePrefs.isStandaloneLyric(prefs)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            ui.column(
                getString(R.string.lyric_standalone),
                getString(R.string.lyric_standalone_sub)
            ),
            ui.weight()
        )
        row.addView(
            GlassSwitch(this).apply {
                setChecked(on, animate = false)
                onCheckedChange = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC_STANDALONE, value).apply()
                    syncService(prefs)
                    toast(getString(if (value) R.string.lyric_standalone_on else R.string.lyric_standalone_off))
                    rerender()
                }
            }
        )
        card.addView(row)
        return card
    }

    private fun syncService(prefs: android.content.SharedPreferences) {
        val want = ModulePrefs.isEnabled(prefs) && (
            (ModulePrefs.isLyricEnabled(prefs) && ModulePrefs.isStandaloneLyric(prefs)) ||
                (ModulePrefs.isLinkEnabled(prefs) && ModulePrefs.isKeepAlive(prefs))
            )
        if (want) KeepAliveService.start(this) else KeepAliveService.stop(this)
    }

    private fun buildNeriCard(): LinearLayout {
        val prefs = ModulePrefs.of(this)
        val card = ui.glassCard()
        card.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16))
        val on = ModulePrefs.isNeriAdapt(prefs)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            ui.column(
                getString(R.string.neri_adapt),
                getString(R.string.neri_adapt_sub)
            ),
            ui.weight()
        )
        row.addView(
            GlassSwitch(this).apply {
                setChecked(on, animate = false)
                onCheckedChange = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_NERI_ADAPT, value).apply()
                    toast(getString(if (value) R.string.neri_adapt_on else R.string.neri_adapt_off))
                    rerender()
                }
            }
        )
        card.addView(row)
        return card
    }

    private fun rerender() {
        Handler(Looper.getMainLooper()).post {
            setContentView(wrapPage(scrollable(buildContent())))
        }
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

        val prefs = ModulePrefs.of(this)
        val on = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLyricEnabled(prefs)

        body.addView(ui.text(getString(R.string.lyric_detail_title), 26f, R.color.text, bold = true))
        body.addView(ui.gap(18))
        body.addView(ui.sectionHeader(getString(R.string.sec_status)))
        body.addView(ui.gap(8))
        val statusCard = ui.glassCard()
        statusCard.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16))

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusRow.addView(
            ui.statusDot(if (on) R.color.ok else R.color.off),
            LinearLayout.LayoutParams(ui.dp(8), ui.dp(8))
        )
        statusRow.addView(
            ui.text(
                getString(if (on) R.string.lyric_status_ready else R.string.lyric_status_off),
                14f, if (on) R.color.ok else R.color.text_hint
            ).apply { setPadding(ui.dp(10), 0, 0, 0) },
            ui.weight()
        )
        val toggle = GlassSwitch(this).apply {
            setChecked(on, animate = false)
            onCheckedChange = { value ->
                prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC, value).apply()
                syncService(prefs)
            }
        }
        statusRow.addView(toggle)
        statusCard.addView(statusRow)
        body.addView(statusCard)
        Motion.fadeInUp(statusCard, 0L)

        body.addView(ui.gap(14))
        body.addView(ui.sectionHeader(getString(R.string.sec_features)))
        body.addView(ui.gap(8))
        val standaloneCard = buildStandaloneCard()
        body.addView(standaloneCard)
        Motion.fadeInUp(standaloneCard, 30L)

        body.addView(ui.gap(8))
        val neriCard = buildNeriCard()
        body.addView(neriCard)
        Motion.fadeInUp(neriCard, 45L)

        body.addView(ui.gap(14))

        body.addView(ui.sectionHeader(getString(R.string.lyric_supported)))
        body.addView(ui.gap(8))
        val appsCard = ui.glassCard()
        appsCard.addView(
            ui.actionRow(getString(R.string.ui_title_apps)) {
                startActivity(
                    android.content.Intent(this, SheetActivity::class.java)
                        .putExtra(SheetActivity.EXTRA_TITLE, getString(R.string.ui_title_apps))
                        .putExtra(SheetActivity.EXTRA_BODY, getString(R.string.ui_apps_body))
                )
            }
        )
        body.addView(appsCard)
        Motion.fadeInUp(appsCard, 60L)

        body.addView(ui.gap(14))

        val howto = ui.text(getString(R.string.lyric_howto_body), 11f, R.color.text_hint).apply {
            setLineSpacing(ui.dp(5).toFloat(), 1.12f)
        }
        body.addView(howto)
        Motion.fadeInUp(howto, 120L)

        return root
    }
}
