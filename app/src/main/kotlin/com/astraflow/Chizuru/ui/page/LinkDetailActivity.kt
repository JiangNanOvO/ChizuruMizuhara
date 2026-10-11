package com.astraflow.Chizuru.ui.page

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.capability.link.BrowserChoice
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.capability.link.LinkLauncher
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.widget.ToggleSwitch
import com.astraflow.Chizuru.ui.widget.UIKit

class LinkDetailActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        prefs = ModulePrefs.of(this)
        runCatching { LinkHub.warmUp(this) }
        render()
    }

    override fun onResume() {
        super.onResume()
        syncKeepAlive()
    }

    private fun syncKeepAlive() {
        val want = ModulePrefs.isEnabled(prefs) &&
            ModulePrefs.isLinkEnabled(prefs) &&
            ModulePrefs.isKeepAlive(prefs)
        if (want) KeepAliveService.start(this) else KeepAliveService.stop(this)
    }

    private fun render() {
        val (content, title) = buildContent()
        val scroll = scrollable(content)
        bindLargeTitle(scroll, title)
        setContentView(wrapPage(scroll))
    }

    private fun rerender() = main.post { render() }

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
                        .putExtra(SheetActivity.EXTRA_TITLE, getString(R.string.link_detail_title))
                        .putExtra(
                            SheetActivity.EXTRA_BODY,
                            getString(R.string.link_privacy_body) + "\n\n" +
                                getString(R.string.link_scope_hint) + "\n\n" +
                                getString(R.string.link_fix_body),
                        ),
                )
            },
        )

        val title = ui.largeTitle(getString(R.string.link_detail_title))
        root.addView(title)
        root.addView(ui.gap(18))

        root.addView(ui.sectionHeader(getString(R.string.sec_status)))
        root.addView(buildStatusCard())

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_keepalive)))
        root.addView(buildKeepAliveCard())

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_browser)))
        root.addView(buildBrowserCard())

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_card)))
        root.addView(buildCardModeCard())

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(getString(R.string.sec_selftest)))
        root.addView(buildSelftestCard())

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(buildFixCard())

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.footerNote(getString(R.string.link_scope_hint)))
        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.footerNote(getString(R.string.link_privacy_body)))

        return root to title
    }

    private fun buildStatusCard(): LinearLayout {
        val card = ui.glassCard()
        val on = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLinkEnabled(prefs)

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
                getString(if (on) R.string.ui_link_on else R.string.ui_link_off),
                15f,
                if (on) R.color.ok else R.color.text_hint,
            ).apply { setPadding(ui.dp(10), 0, 0, 0) },
            ui.weight(),
        )
        row.addView(
            ToggleSwitch(this).apply {
                setChecked(on, animate = false)
                onCheckedChange = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LINK, value).apply()
                    toast(getString(if (value) R.string.ui_link_on else R.string.ui_link_off))
                    rerender()
                }
            },
        )
        card.addView(row)
        card.addView(ui.divider(0))
        card.addView(
            ui.footerNote(
                getString(R.string.link_island_state) + "：" + islandStateLabel() +
                    "\n" + getString(R.string.link_sdk) + "：" + LinkHub.sdkVersion(),
            ).apply {
                setPadding(
                    ui.dp(SectionTokens.HORIZONTAL_PADDING),
                    ui.dp(12),
                    ui.dp(SectionTokens.HORIZONTAL_PADDING),
                    ui.dp(14),
                )
                setTextColor(ui.colors.secondaryLabel)
            },
        )

        if (!ModuleEnabledState.isModuleEnabled(this)) {
            card.addView(ui.divider(0))
            card.addView(
                ui.footerNote(getString(R.string.link_module_off)).apply {
                    setPadding(
                        ui.dp(SectionTokens.HORIZONTAL_PADDING),
                        ui.dp(12),
                        ui.dp(SectionTokens.HORIZONTAL_PADDING),
                        ui.dp(14),
                    )
                    setTextColor(ui.colors.systemOrange)
                },
            )
        }
        return card
    }

    private fun buildKeepAliveCard(): LinearLayout {
        val card = ui.glassCard()
        val on = ModulePrefs.isKeepAlive(prefs)
        card.addView(
            ui.switchRow(
                getString(R.string.link_keepalive_switch),
                on,
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_KEEP_ALIVE, value).apply()
                    if (value) {
                        requestNotifyPermission()
                        toast(getString(R.string.link_keepalive_on))
                    } else {
                        Toast.makeText(
                            this@LinkDetailActivity,
                            getString(R.string.link_keepalive_off),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    syncKeepAlive()
                    rerender()
                },
            ),
        )
        card.addView(ui.divider(0))
        card.addView(
            ui.footerNote(getString(R.string.link_keepalive_desc)).apply {
                setPadding(
                    ui.dp(SectionTokens.HORIZONTAL_PADDING),
                    0,
                    ui.dp(SectionTokens.HORIZONTAL_PADDING),
                    ui.dp(14),
                )
            },
        )
        return card
    }

    private fun buildBrowserCard(): LinearLayout {
        val card = ui.glassCard()
        val mode = ModulePrefs.browserMode(prefs)
        val pinned = ModulePrefs.browserPackage(prefs)
        card.addView(
            ui.row(
                iconRes = null,
                title = getString(R.string.link_browser),
                value = currentBrowserLabel(mode, pinned),
                onClick = { showBrowserPicker() },
            ),
        )
        return card
    }

    private fun buildCardModeCard(): LinearLayout {
        val card = ui.glassCard()
        val quiet = ModulePrefs.isLinkHintOnly(prefs)
        card.addView(
            ui.row(
                iconRes = null,
                title = getString(R.string.link_card_mode),
                value = getString(
                    if (quiet) R.string.link_card_quiet else R.string.link_card_expand,
                ),
                onClick = {
                    val options = listOf(
                        QUIET_EXPAND to getString(R.string.link_card_expand),
                        QUIET_QUIET to getString(R.string.link_card_quiet),
                    )
                    PickerSheet.show(this, getString(R.string.link_card_pick), options) { key ->
                        prefs.edit()
                            .putBoolean(ModulePrefs.KEY_LINK_HINT_ONLY, key == QUIET_QUIET)
                            .apply()
                        toast(getString(R.string.ui_browser_saved))
                        rerender()
                    }
                },
            ),
        )
        return card
    }

    private fun buildSelftestCard(): LinearLayout {
        val card = ui.glassCard()
        card.addView(
            ui.row(
                iconRes = null,
                title = getString(R.string.link_test),
                subtitle = getString(R.string.link_test_short),
                onClick = { postTestCard() },
            ),
        )
        card.addView(ui.divider())
        card.addView(
            ui.row(
                iconRes = null,
                title = getString(R.string.link_test_open),
                value = SELF_TEST_URL,
                onClick = {
                    if (!LinkLauncher.open(this, SELF_TEST_URL)) {
                        toast(getString(R.string.link_test_fail))
                    }
                },
            ),
        )
        return card
    }

    private fun buildFixCard(): LinearLayout {
        val card = ui.glassCard()
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ui.dp(SectionTokens.HORIZONTAL_PADDING),
                ui.dp(14),
                ui.dp(SectionTokens.HORIZONTAL_PADDING),
                ui.dp(16),
            )
        }
        body.addView(
            ui.text(getString(R.string.link_fix_title), 15f, R.color.accent, bold = true),
        )
        body.addView(ui.gap(8))
        body.addView(
            ui.footerNote(getString(R.string.link_fix_body)).apply { setPadding(0, 0, 0, 0) },
        )
        card.addView(body)
        return card
    }

    private fun requestNotifyPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            runCatching {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 9001)
            }
        }
    }

    private fun islandStateLabel(): String = when (LinkHub.stateName()) {
        "NOT_INSTALLED" -> getString(R.string.island_not_installed)
        "WAITING" -> getString(R.string.island_waiting)
        "REJECTED" -> getString(R.string.island_rejected)
        "READY" -> getString(R.string.island_ready)
        else -> LinkHub.stateName()
    }

    private fun currentBrowserLabel(mode: String, pinned: String): String = when (mode) {
        ModulePrefs.BROWSER_PINNED ->
            BrowserChoice.installed(this)
                .firstOrNull { it.packageName == pinned }?.label
                ?: getString(R.string.link_browser_system)

        ModulePrefs.BROWSER_ASK -> getString(R.string.link_browser_ask)
        else -> getString(R.string.link_browser_system)
    }

    private fun showBrowserPicker() {
        val options = ArrayList<Pair<String, String>>()
        options.add(ModulePrefs.BROWSER_SYSTEM to getString(R.string.link_browser_system))
        options.add(ModulePrefs.BROWSER_ASK to getString(R.string.link_browser_ask))
        for (app in BrowserChoice.installed(this)) {
            options.add(app.packageName to app.label)
        }
        PickerSheet.show(this, getString(R.string.link_browser_pick), options) { key ->
            val editor = prefs.edit()
            when (key) {
                ModulePrefs.BROWSER_SYSTEM ->
                    editor.putString(ModulePrefs.KEY_BROWSER_MODE, ModulePrefs.BROWSER_SYSTEM)

                ModulePrefs.BROWSER_ASK ->
                    editor.putString(ModulePrefs.KEY_BROWSER_MODE, ModulePrefs.BROWSER_ASK)

                else -> {
                    editor.putString(ModulePrefs.KEY_BROWSER_MODE, ModulePrefs.BROWSER_PINNED)
                    editor.putString(ModulePrefs.KEY_BROWSER_PACKAGE, key)
                }
            }
            editor.apply()
            toast(getString(R.string.ui_browser_saved))
            rerender()
        }
    }

    private fun postTestCard() {
        Thread {
            val ok = LinkHub.postBlocking(applicationContext, SELF_TEST_URL)
            val msg = getString(if (ok) R.string.link_test_posted else R.string.link_test_fail)
            main.post { Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show() }
        }.apply { isDaemon = true }.start()
    }

    private companion object {
        const val SELF_TEST_URL = "https://astraflow.cc/island"
        const val QUIET_EXPAND = "expand"
        const val QUIET_QUIET = "quiet"
    }
}
