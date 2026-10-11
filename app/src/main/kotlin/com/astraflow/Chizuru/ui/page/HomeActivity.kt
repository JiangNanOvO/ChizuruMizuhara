package com.astraflow.Chizuru.ui.page

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.widget.UIKit

class HomeActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        prefs = ModulePrefs.of(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        runCatching { LinkHub.warmUp(this) }
        render()
    }

    private fun render() {
        val (content, title) = buildContent()
        val scroll = scrollable(content)
        bindLargeTitle(scroll, title)
        setContentView(wrapPage(scroll))
    }

    private fun buildContent(): Pair<LinearLayout, android.widget.TextView> {
        val root = pageRoot()
        root.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.END
                addView(ui.helpButton { showAboutSheet() })
            },
        )
        root.addView(ui.gap(6))

        val title = ui.largeTitle(getString(R.string.app_name_launcher))
        root.addView(title)
        root.addView(ui.gap(18))

        val lyricOn = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLyricEnabled(prefs)
        val linkOn = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLinkEnabled(prefs)

        root.addView(ui.sectionHeader(getString(R.string.sec_status)))
        val status = ui.glassCard()
        status.addView(ui.valueRow(getString(R.string.cap_lyric), onOff(lyricOn)))
        status.addView(ui.divider())
        status.addView(ui.valueRow(getString(R.string.cap_link), onOff(linkOn)))
        status.addView(ui.divider())
        status.addView(ui.valueRow(getString(R.string.sec_island), islandLabel()))
        root.addView(status)

        if (!ModuleEnabledState.isModuleEnabled(this)) {
            root.addView(ui.gap(8))
            root.addView(
                ui.footerNote(getString(R.string.home_not_active)).apply {
                    setPadding(0, 0, 0, 0)
                },
            )
        }

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_features)))
        val features = ui.glassCard()
        features.addView(
            ui.switchRow(
                getString(R.string.cap_lyric),
                lyricOn,
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC, value).apply()
                    toast(getString(if (value) R.string.ui_enable_on else R.string.ui_enable_off))
                    render()
                },
                onOpen = { openPage(LyricDetailActivity::class.java) },
            ),
        )
        features.addView(ui.divider())
        features.addView(
            ui.switchRow(
                getString(R.string.cap_link),
                linkOn,
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LINK, value).apply()
                    toast(getString(if (value) R.string.ui_link_on else R.string.ui_link_off))
                    render()
                },
                onOpen = { openPage(LinkDetailActivity::class.java) },
            ),
        )
        root.addView(features)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_more)))
        val more = ui.glassCard()
        more.addView(
            ui.actionRow(getString(R.string.home_settings)) {
                openPage(SettingsActivity::class.java)
            },
        )
        more.addView(ui.divider())
        more.addView(
            ui.actionRow(getString(R.string.home_sponsor)) {
                openPage(SponsorActivity::class.java)
            },
        )
        more.addView(ui.divider())
        more.addView(
            ui.valueRow(
                title = getString(R.string.home_about),
                value = "v" + versionName(),
                onClick = { showAboutSheet() },
            ),
        )
        root.addView(more)

        return root to title
    }

    private fun pageRoot(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            ui.dp(SectionTokens.PAGE_INSET),
            ui.dp(20),
            ui.dp(SectionTokens.PAGE_INSET),
            ui.dp(40),
        )
    }

    private fun onOff(value: Boolean): String =
        getString(if (value) R.string.value_on else R.string.value_off)

    private fun islandLabel(): String = when (LinkHub.stateName()) {
        "NOT_INSTALLED" -> getString(R.string.value_island_missing)
        "WAITING" -> getString(R.string.value_island_waiting)
        "REJECTED" -> getString(R.string.value_island_rejected)
        "READY" -> getString(R.string.value_island_ready)
        else -> LinkHub.stateName()
    }

    private fun showAboutSheet() {
        startActivity(
            android.content.Intent(this, SheetActivity::class.java)
                .putExtra(SheetActivity.EXTRA_TITLE, getString(R.string.ui_title_about))
                .putExtra(SheetActivity.EXTRA_BODY, getString(R.string.ui_about_body)),
        )
    }
}
