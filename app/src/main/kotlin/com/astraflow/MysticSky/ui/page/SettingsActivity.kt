package com.astraflow.MysticSky.ui.page

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import com.astraflow.MysticSky.R
import com.astraflow.MysticSky.capability.link.LinkHub
import com.astraflow.MysticSky.keepalive.KeepAliveService
import com.astraflow.MysticSky.settings.ModuleEnabledState
import com.astraflow.MysticSky.settings.ModulePrefs
import com.astraflow.MysticSky.ui.widget.Motion
import com.astraflow.MysticSky.ui.widget.UIKit

class SettingsActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        prefs = ModulePrefs.of(this)
        syncLauncherIcon()
        setContentView(wrapPage(scrollable(buildContent())))
    }

    private fun buildContent(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(18), ui.dp(26), ui.dp(18), ui.dp(44))
        }

        root.addView(ui.pageHeader(getString(R.string.home_settings)) {
            showSheet(getString(R.string.ui_title_about), getString(R.string.ui_about_body))
        })
        root.addView(ui.gap(22))

        root.addView(ui.sectionHeader(getString(R.string.sec_status)))
        root.addView(ui.gap(8))
        val status = ui.glassCard()
        status.addView(
            ui.valueRow(getString(R.string.sec_module), onOff(ModuleEnabledState.isModuleEnabled(this)))
        )
        status.addView(ui.divider(18))
        status.addView(
            ui.valueRow(
                getString(R.string.cap_lyric),
                onOff(ModulePrefs.isEnabled(prefs) && ModulePrefs.isLyricEnabled(prefs))
            )
        )
        status.addView(ui.divider(18))
        status.addView(
            ui.valueRow(
                getString(R.string.cap_link),
                onOff(ModulePrefs.isEnabled(prefs) && ModulePrefs.isLinkEnabled(prefs))
            )
        )
        status.addView(ui.divider(18))
        status.addView(ui.valueRow(getString(R.string.sec_island), islandLabel()))
        root.addView(status)
        Motion.fadeInUp(status, 0L)

        root.addView(ui.gap(22))
        root.addView(ui.sectionHeader(getString(R.string.sec_features)))
        root.addView(ui.gap(8))
        val features = ui.glassCard()
        features.addView(
            ui.switchRow(
                getString(R.string.ui_switch_enable),
                ModulePrefs.isEnabled(prefs),
                onToggle = { value ->
                prefs.edit().putBoolean(ModulePrefs.KEY_ENABLED, value).apply()
                if (!value) {
                    runCatching { KeepAliveService.stop(this) }
                }
                toast(getString(if (value) R.string.ui_enable_on else R.string.ui_enable_off))
            })
        )
        features.addView(ui.divider(18))
        features.addView(
            ui.switchRow(
                getString(R.string.ui_switch_hide),
                ModulePrefs.isHideIcon(prefs),
                onToggle = { value ->
                setLauncherIconHidden(value)
                prefs.edit().putBoolean(ModulePrefs.KEY_HIDE_ICON, value).apply()
                toast(getString(if (value) R.string.ui_hide_on else R.string.ui_hide_off))
            })
        )
        features.addView(ui.divider(18))
        features.addView(ui.actionRow(getString(R.string.ui_btn_lsposed)) { openLsposed() })
        root.addView(features)
        Motion.fadeInUp(features, 60L)

        root.addView(ui.gap(22))
        root.addView(ui.sectionHeader(getString(R.string.sec_about)))
        root.addView(ui.gap(8))
        val about = ui.glassCard()
        about.addView(
            ui.valueRow(getString(R.string.ui_title_author), getString(R.string.ui_sub_author)) {
                showSheet(getString(R.string.ui_title_author), getString(R.string.ui_author_body))
            }
        )
        about.addView(ui.divider(18))
        about.addView(
            ui.valueRow(getString(R.string.ui_title_feedback), getString(R.string.ui_sub_feedback)) {
                copyFeedbackEmail()
            }
        )
        about.addView(ui.divider(18))
        about.addView(
            ui.valueRow(getString(R.string.ui_title_free)) {
                showSheet(getString(R.string.ui_title_free), getString(R.string.ui_free_body))
            }
        )
        about.addView(ui.divider(18))
        about.addView(ui.actionRow(getString(R.string.ui_title_update)) { openProjectPage() })
        root.addView(about)
        Motion.fadeInUp(about, 120L)

        return root
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

    private fun showSheet(title: String, body: String) {
        startActivity(
            Intent(this, SheetActivity::class.java)
                .putExtra(SheetActivity.EXTRA_TITLE, title)
                .putExtra(SheetActivity.EXTRA_BODY, body)
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
