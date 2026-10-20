package com.astraflow.Chizuru.ui.page

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.widget.ToggleSwitch

internal class LyricPage {

    fun build(host: BaseActivity): View {
        val ui = host.ui
        val prefs = ModulePrefs.of(host)
        val on = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLyricEnabled(prefs)

        val root = host.pageRoot(topDp = 14)
        root.addView(
            LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                addView(
                    ui.helpButton {
                        host.sheet(
                            host.getString(R.string.lyric_detail_title),
                            host.getString(R.string.lyric_howto_body) + "\n\n" +
                                host.getString(R.string.ui_apps_body),
                        )
                    },
                )
            },
        )
        root.addView(ui.gap(6))

        val title = ui.largeTitle(host.getString(R.string.lyric_detail_title))
        root.addView(title)
        root.addView(ui.gap(18))

        root.addView(ui.sectionHeader(host.getString(R.string.sec_status)))
        root.addView(statusCard(host, on))

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.sec_features)))
        val features = ui.glassCard()
        features.addView(
            ui.switchRow(
                title = host.getString(R.string.lyric_standalone),
                checked = ModulePrefs.isStandaloneLyric(prefs),
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC_STANDALONE, value).apply()
                    host.syncKeepAlive()
                    host.toast(
                        host.getString(
                            if (value) R.string.lyric_standalone_on else R.string.lyric_standalone_off,
                        ),
                    )
                    host.reloadPage()
                },
                note = host.getString(R.string.lyric_standalone_sub),
            ),
        )
        features.addView(ui.divider())
        features.addView(
            ui.switchRow(
                title = host.getString(R.string.neri_adapt),
                checked = ModulePrefs.isNeriAdapt(prefs),
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_NERI_ADAPT, value).apply()
                    host.toast(
                        host.getString(
                            if (value) R.string.neri_adapt_on else R.string.neri_adapt_off,
                        ),
                    )
                    host.reloadPage()
                },
                note = host.getString(R.string.neri_adapt_sub),
            ),
        )
        root.addView(features)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.lyric_supported)))
        val apps = ui.glassCard()
        apps.addView(
            ui.row(
                iconRes = null,
                title = host.getString(R.string.ui_title_apps),
                onClick = {
                    host.sheet(
                        host.getString(R.string.ui_title_apps),
                        host.getString(R.string.ui_apps_body),
                    )
                },
            ),
        )
        root.addView(apps)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.footerNote(host.getString(R.string.lyric_howto_body)))

        val scroll = ScrollView(host).apply {
            isFillViewport = true
            setBackgroundColor(host.theme.colors.systemGroupedBackground)
            addView(root)
        }
        host.bindLargeTitle(scroll, title)
        return scroll
    }

    private fun statusCard(host: BaseActivity, on: Boolean): LinearLayout {
        val ui = host.ui
        val prefs = ModulePrefs.of(host)
        val card = ui.glassCard()
        val row = LinearLayout(host).apply {
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
                host.getString(if (on) R.string.lyric_status_ready else R.string.lyric_status_off),
                15f,
                if (on) R.color.ok else R.color.text_hint,
            ).apply { setPadding(ui.dp(10), 0, 0, 0) },
            ui.weight(),
        )
        row.addView(
            ToggleSwitch(host).apply {
                setChecked(on, animate = false)
                onCheckedChange = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC, value).apply()
                    host.syncKeepAlive()
                    host.reloadPage()
                }
            },
        )
        card.addView(row)
        return card
    }
}
