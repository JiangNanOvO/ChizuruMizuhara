package com.astraflow.Chizuru.ui.page

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.widget.Motion
import com.astraflow.Chizuru.ui.widget.UIKit

class HomeActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        prefs = ModulePrefs.of(this)
        setContentView(wrapPage(scrollable(buildContent())))
    }

    override fun onResume() {
        super.onResume()
        runCatching { LinkHub.warmUp(this) }
        setContentView(wrapPage(scrollable(buildContent())))
    }

    private fun buildContent(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(18), ui.dp(26), ui.dp(18), ui.dp(44))
        }

        root.addView(ui.pageHeader(getString(R.string.app_name_launcher)) { showAboutSheet() })
        root.addView(ui.gap(22))

        val lyricOn = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLyricEnabled(prefs)
        val linkOn = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLinkEnabled(prefs)

        root.addView(ui.sectionHeader(getString(R.string.sec_status)))
        root.addView(ui.gap(8))
        val status = ui.glassCard()
        status.addView(ui.valueRow(getString(R.string.cap_lyric), onOff(lyricOn)))
        status.addView(ui.divider(18))
        status.addView(ui.valueRow(getString(R.string.cap_link), onOff(linkOn)))
        status.addView(ui.divider(18))
        status.addView(ui.valueRow(getString(R.string.sec_island), islandLabel()))
        root.addView(status)
        Motion.fadeInUp(status, 0L)

        if (!ModuleEnabledState.isModuleEnabled(this)) {
            root.addView(ui.gap(10))
            root.addView(ui.noteText(getString(R.string.home_not_active)))
        }

        root.addView(ui.gap(22))
        root.addView(ui.sectionHeader(getString(R.string.sec_features)))
        root.addView(ui.gap(8))
        val features = ui.glassCard()
        features.addView(
            ui.switchRow(getString(R.string.cap_lyric), lyricOn, { value ->
                prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC, value).apply()
                toast(getString(if (value) R.string.ui_enable_on else R.string.ui_enable_off))
                rerender()
            }, onOpen = { openPage(LyricDetailActivity::class.java) })
        )
        features.addView(ui.divider(18))
        features.addView(
            ui.switchRow(getString(R.string.cap_link), linkOn, { value ->
                prefs.edit().putBoolean(ModulePrefs.KEY_LINK, value).apply()
                toast(getString(if (value) R.string.ui_link_on else R.string.ui_link_off))
                rerender()
            }, onOpen = { openPage(LinkDetailActivity::class.java) })
        )
        root.addView(features)
        Motion.fadeInUp(features, 60L)

        root.addView(ui.gap(22))
        root.addView(ui.sectionHeader(getString(R.string.sec_more)))
        root.addView(ui.gap(8))
        val bottom = ui.glassCard()
        bottom.addView(ui.valueRow(getString(R.string.home_settings)) {
            openPage(SettingsActivity::class.java)
        })
        bottom.addView(ui.divider(18))
        bottom.addView(ui.valueRow(getString(R.string.home_sponsor)) {
            openPage(SponsorActivity::class.java)
        })
        bottom.addView(ui.divider(18))
        bottom.addView(ui.valueRow(getString(R.string.home_about), "v" + versionName()) {
            showAboutSheet()
        })
        root.addView(bottom)
        Motion.fadeInUp(bottom, 120L)

        return root
    }

    private fun rerender() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            setContentView(wrapPage(scrollable(buildContent())))
        }
    }

    private fun onOff(value: Boolean): String =
        getString(if (value) R.string.value_on else R.string.value_off)

    private fun islandLabel(): String {
        val state = LinkHub.stateName()
        return when (state) {
            "NOT_INSTALLED" -> getString(R.string.value_island_missing)
            "WAITING" -> getString(R.string.value_island_waiting)
            "REJECTED" -> getString(R.string.value_island_rejected)
            "READY" -> getString(R.string.value_island_ready)
            else -> state
        }
    }

    private fun showAboutSheet() {
        startActivity(
            android.content.Intent(this, SheetActivity::class.java)
                .putExtra(SheetActivity.EXTRA_TITLE, getString(R.string.ui_title_about))
                .putExtra(SheetActivity.EXTRA_BODY, getString(R.string.ui_about_body))
        )
    }
}
