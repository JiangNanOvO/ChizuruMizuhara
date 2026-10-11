package com.astraflow.Chizuru.ui.page

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.widget.UIKit

class SettingsActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        prefs = ModulePrefs.of(this)
        syncLauncherIcon()
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
                ui.dp(20),
                ui.dp(SectionTokens.PAGE_INSET),
                ui.dp(40),
            )
        }

        root.addView(
            android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.END
                addView(
                    ui.helpButton {
                        showSheet(getString(R.string.ui_title_about), getString(R.string.ui_about_body))
                    },
                )
            },
        )
        root.addView(ui.gap(6))

        val title = ui.largeTitle(getString(R.string.home_settings))
        root.addView(title)
        root.addView(ui.gap(18))

        root.addView(ui.sectionHeader(getString(R.string.sec_status)))
        val status = ui.glassCard()
        status.addView(
            ui.valueRow(
                getString(R.string.sec_module),
                onOff(ModuleEnabledState.isModuleEnabled(this)),
            ),
        )
        status.addView(ui.divider())
        status.addView(
            ui.valueRow(
                getString(R.string.cap_lyric),
                onOff(ModulePrefs.isEnabled(prefs) && ModulePrefs.isLyricEnabled(prefs)),
            ),
        )
        status.addView(ui.divider())
        status.addView(
            ui.valueRow(
                getString(R.string.cap_link),
                onOff(ModulePrefs.isEnabled(prefs) && ModulePrefs.isLinkEnabled(prefs)),
            ),
        )
        status.addView(ui.divider())
        status.addView(ui.valueRow(getString(R.string.sec_island), islandLabel()))
        root.addView(status)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_features)))
        val features = ui.glassCard()
        features.addView(
            ui.switchRow(
                title = getString(R.string.ui_switch_enable),
                checked = ModulePrefs.isEnabled(prefs),
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_ENABLED, value).apply()
                    if (!value) {
                        runCatching { KeepAliveService.stop(this) }
                    }
                    toast(getString(if (value) R.string.ui_enable_on else R.string.ui_enable_off))
                },
            ),
        )
        features.addView(ui.divider())
        features.addView(
            ui.switchRow(
                title = getString(R.string.ui_switch_hide),
                checked = ModulePrefs.isHideIcon(prefs),
                onToggle = { value ->
                    setLauncherIconHidden(value)
                    prefs.edit().putBoolean(ModulePrefs.KEY_HIDE_ICON, value).apply()
                    toast(getString(if (value) R.string.ui_hide_on else R.string.ui_hide_off))
                },
                note = getString(R.string.ui_switch_hide_hint),
            ),
        )
        features.addView(ui.divider())
        features.addView(
            ui.actionRow(getString(R.string.ui_btn_lsposed)) { openLsposed() },
        )
        root.addView(features)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_about)))
        val about = ui.glassCard()
        about.addView(
            ui.row(
                iconRes = null,
                title = getString(R.string.ui_title_author),
                value = getString(R.string.ui_sub_author),
                onClick = {
                    showSheet(getString(R.string.ui_title_author), getString(R.string.ui_author_body))
                },
            ),
        )
        about.addView(ui.divider())
        about.addView(
            ui.row(
                iconRes = null,
                title = getString(R.string.ui_title_feedback),
                value = getString(R.string.ui_sub_feedback),
                onClick = { copyFeedbackEmail() },
            ),
        )
        about.addView(ui.divider())
        about.addView(
            ui.row(
                iconRes = null,
                title = getString(R.string.ui_title_free),
                onClick = {
                    showSheet(getString(R.string.ui_title_free), getString(R.string.ui_free_body))
                },
            ),
        )
        about.addView(ui.divider())
        about.addView(
            ui.row(
                iconRes = null,
                title = getString(R.string.ui_title_update),
                onClick = { openProjectPage() },
            ),
        )
        root.addView(about)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.footerNote(getString(R.string.ui_sub_update)))

        return root to title
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

    private fun showSheet(title: String, body: String) {
        startActivity(
            Intent(this, SheetActivity::class.java)
                .putExtra(SheetActivity.EXTRA_TITLE, title)
                .putExtra(SheetActivity.EXTRA_BODY, body),
        )
    }

    private fun copyFeedbackEmail() {
        runCatching {
            val cm = getSystemService(android.content.ClipboardManager::class.java)
            cm?.setPrimaryClip(android.content.ClipData.newPlainText("email", FEEDBACK_EMAIL))
        }
        toast(getString(R.string.ui_feedback_copied))
    }

    private fun openProjectPage() {
        val opened = runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL)))
        }.isSuccess
        if (!opened) {
            runCatching {
                val cm = getSystemService(android.content.ClipboardManager::class.java)
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("project", PROJECT_URL))
            }
            toast(getString(R.string.ui_msg_copied))
        }
    }

    private fun syncLauncherIcon() {
        setLauncherIconHidden(ModulePrefs.isHideIcon(prefs))
    }

    private companion object {
        private const val PROJECT_URL = "https://github.com/JiangNanOvO/Mystic-Sky"
        private const val FEEDBACK_EMAIL = "271400001@qq.com"
    }
}
