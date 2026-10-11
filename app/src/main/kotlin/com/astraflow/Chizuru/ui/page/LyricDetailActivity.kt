package com.astraflow.Chizuru.ui.page

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.widget.ToggleSwitch
import com.astraflow.Chizuru.ui.widget.UIKit

class LyricDetailActivity : BaseActivity() {

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

    private fun rerender() = Handler(Looper.getMainLooper()).post { render() }

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
                        .putExtra(SheetActivity.EXTRA_TITLE, getString(R.string.lyric_detail_title))
                        .putExtra(
                            SheetActivity.EXTRA_BODY,
                            getString(R.string.lyric_howto_body) + "\n\n" +
                                getString(R.string.ui_apps_body),
                        ),
                )
            },
        )

        val title = ui.largeTitle(getString(R.string.lyric_detail_title))
        root.addView(title)
        root.addView(ui.gap(18))

        val prefs = ModulePrefs.of(this)
        val on = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLyricEnabled(prefs)

        root.addView(ui.sectionHeader(getString(R.string.sec_status)))
        root.addView(buildStatusCard(prefs, on))

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_features)))
        val features = ui.glassCard()
        features.addView(
            switchNoteRow(
                getString(R.string.lyric_standalone),
                getString(R.string.lyric_standalone_sub),
                ModulePrefs.isStandaloneLyric(prefs),
            ) { value ->
                prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC_STANDALONE, value).apply()
                syncService(prefs)
                toast(
                    getString(
                        if (value) R.string.lyric_standalone_on else R.string.lyric_standalone_off,
                    ),
                )
                rerender()
            },
        )
        features.addView(ui.divider())
        features.addView(
            switchNoteRow(
                getString(R.string.neri_adapt),
                getString(R.string.neri_adapt_sub),
                ModulePrefs.isNeriAdapt(prefs),
            ) { value ->
                prefs.edit().putBoolean(ModulePrefs.KEY_NERI_ADAPT, value).apply()
                toast(getString(if (value) R.string.neri_adapt_on else R.string.neri_adapt_off))
                rerender()
            },
        )
        root.addView(features)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.lyric_supported)))
        val apps = ui.glassCard()
        apps.addView(
            ui.row(
                iconRes = null,
                title = getString(R.string.ui_title_apps),
                onClick = {
                    startActivity(
                        android.content.Intent(this, SheetActivity::class.java)
                            .putExtra(SheetActivity.EXTRA_TITLE, getString(R.string.ui_title_apps))
                            .putExtra(SheetActivity.EXTRA_BODY, getString(R.string.ui_apps_body)),
                    )
                },
            ),
        )
        root.addView(apps)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.footerNote(getString(R.string.lyric_howto_body)))

        return root to title
    }

    private fun buildStatusCard(prefs: android.content.SharedPreferences, on: Boolean): LinearLayout {
        val card = ui.glassCard()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = ui.dp(SectionTokens.MIN_HEIGHT)
            setPadding(
                ui.dp(SectionTokens.HORIZONTAL_PADDING),
                ui.dp(11),
                ui.dp(SectionTokens.HORIZONTAL_PADDING),
                ui.dp(11),
            )
        }
        row.addView(
            ui.statusDot(if (on) R.color.ok else R.color.off),
            LinearLayout.LayoutParams(ui.dp(8), ui.dp(8)),
        )
        row.addView(
            ui.text(
                getString(if (on) R.string.lyric_status_ready else R.string.lyric_status_off),
                15f,
                if (on) R.color.ok else R.color.text_hint,
            ).apply { setPadding(ui.dp(10), 0, 0, 0) },
            ui.weight(),
        )
        row.addView(
            ToggleSwitch(this).apply {
                setChecked(on, animate = false)
                onCheckedChange = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC, value).apply()
                    syncService(prefs)
                    rerender()
                }
            },
        )
        card.addView(row)
        return card
    }

    private fun switchNoteRow(
        title: String,
        note: String,
        checked: Boolean,
        onToggle: (Boolean) -> Unit,
    ): LinearLayout = ui.switchRow(
        title = title,
        checked = checked,
        onToggle = onToggle,
        note = note,
    )

    private fun syncService(prefs: android.content.SharedPreferences) {
        val want = ModulePrefs.isEnabled(prefs) && (
            (ModulePrefs.isLyricEnabled(prefs) && ModulePrefs.isStandaloneLyric(prefs)) ||
                (ModulePrefs.isLinkEnabled(prefs) && ModulePrefs.isKeepAlive(prefs))
            )
        if (want) KeepAliveService.start(this) else KeepAliveService.stop(this)
    }
}
