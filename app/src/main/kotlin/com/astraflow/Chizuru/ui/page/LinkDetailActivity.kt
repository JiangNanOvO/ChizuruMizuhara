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
import android.widget.Toast
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.capability.link.BrowserChoice
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.capability.link.LinkLauncher
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.widget.GlassSwitch
import com.astraflow.Chizuru.ui.widget.Motion
import com.astraflow.Chizuru.ui.widget.UIKit

class LinkDetailActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    private val main = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        prefs = ModulePrefs.of(this)
        setTheme(R.style.Theme_Mitian_Detail)

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
        setContentView(wrapPage(scrollable(buildContent())))
    }

    private fun rerender() {
        main.post { render() }
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

        body.addView(ui.text(getString(R.string.link_detail_title), 26f, R.color.text, bold = true))
        body.addView(ui.gap(18))
        body.addView(ui.sectionHeader("运行状态"))
        body.addView(ui.gap(8))
        val switchCard = buildSwitchCard()
        body.addView(switchCard)
        Motion.fadeInUp(switchCard, 0L)

        body.addView(ui.gap(14))
        body.addView(ui.sectionHeader("保活"))
        body.addView(ui.gap(8))
        val keepAliveCard = buildKeepAliveCard()
        body.addView(keepAliveCard)
        Motion.fadeInUp(keepAliveCard, 40L)

        body.addView(ui.gap(14))
        body.addView(ui.sectionHeader("打开方式"))
        body.addView(ui.gap(8))
        val browserCard = buildBrowserCard()
        body.addView(browserCard)
        Motion.fadeInUp(browserCard, 60L)

        body.addView(ui.gap(14))
        body.addView(ui.sectionHeader("卡片"))
        body.addView(ui.gap(8))
        val cardMode = buildCardModeCard()
        body.addView(cardMode)
        Motion.fadeInUp(cardMode, 90L)

        body.addView(ui.gap(14))
        body.addView(ui.sectionHeader("自检"))
        body.addView(ui.gap(8))
        val selftest = buildSelftestCard()
        body.addView(selftest)
        Motion.fadeInUp(selftest, 120L)

        body.addView(ui.gap(18))
        val fix = buildFixCard()
        body.addView(fix)
        Motion.fadeInUp(fix, 150L)

        body.addView(ui.gap(14))
        body.addView(
            ui.text(getString(R.string.link_scope_hint), 11f, R.color.text_hint).apply {
                setLineSpacing(ui.dp(5).toFloat(), 1.12f)
            }
        )

        body.addView(ui.gap(12))
        body.addView(
            ui.text(getString(R.string.link_privacy_body), 11f, R.color.text_hint).apply {
                setLineSpacing(ui.dp(5).toFloat(), 1.12f)
            }
        )

        return root
    }

    private fun buildSwitchCard(): LinearLayout {
        val card = ui.glassCard()
        val on = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLinkEnabled(prefs)
        card.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            ui.statusDot(if (on) R.color.ok else R.color.off),
            LinearLayout.LayoutParams(ui.dp(8), ui.dp(8))
        )
        row.addView(
            ui.text(
                getString(if (on) R.string.ui_link_on else R.string.ui_link_off),
                14f, if (on) R.color.ok else R.color.text_hint
            ).apply { setPadding(ui.dp(10), 0, 0, 0) },
            ui.weight()
        )
        row.addView(
            GlassSwitch(this).apply {
                setChecked(on, animate = false)
                onCheckedChange = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LINK, value).apply()
                    toast(getString(if (value) R.string.ui_link_on else R.string.ui_link_off))
                    rerender()
                }
            }
        )
        card.addView(row)
        card.addView(ui.gap(12))
        card.addView(
            ui.text(
                "${getString(R.string.link_island_state)}：${islandStateLabel()}",
                12f,
                if (LinkHub.isReady()) R.color.ok else R.color.text_hint
            )
        )
        card.addView(ui.gap(4))
        card.addView(
            ui.text(
                "${getString(R.string.link_sdk)}：${LinkHub.sdkVersion()}",
                12f, R.color.text_hint
            )
        )
        if (!ModuleEnabledState.isModuleEnabled(this)) {
            card.addView(ui.gap(6))
            card.addView(
                ui.text(getString(R.string.link_module_off), 11f, R.color.off).apply {
                    setLineSpacing(ui.dp(4).toFloat(), 1.1f)
                }
            )
        }
        return card
    }

    private fun buildKeepAliveCard(): LinearLayout {
        val card = ui.glassCard()
        card.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16))
        val on = ModulePrefs.isKeepAlive(prefs)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            ui.column(
                getString(R.string.link_keepalive_switch),
                getString(R.string.link_keepalive_desc)
            ),
            ui.weight()
        )
        row.addView(
            GlassSwitch(this).apply {
                setChecked(on, animate = false)
                onCheckedChange = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_KEEP_ALIVE, value).apply()
                    if (value) {
                        requestNotifyPermission()
                        toast(getString(R.string.link_keepalive_on))
                    } else {
                        Toast.makeText(
                            this@LinkDetailActivity,
                            getString(R.string.link_keepalive_off),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    syncKeepAlive()
                    rerender()
                }
            }
        )
        card.addView(row)
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

    private fun showSheet(title: String, body: String) {
        startActivity(
            Intent(this, SheetActivity::class.java)
                .putExtra(SheetActivity.EXTRA_TITLE, title)
                .putExtra(SheetActivity.EXTRA_BODY, body)
        )
    }

    private fun islandStateLabel(): String = when (LinkHub.stateName()) {
        "NOT_INSTALLED" -> getString(R.string.island_not_installed)
        "WAITING" -> getString(R.string.island_waiting)
        "REJECTED" -> getString(R.string.island_rejected)
        "READY" -> getString(R.string.island_ready)
        else -> LinkHub.stateName()
    }

    private fun buildBrowserCard(): LinearLayout {
        val card = ui.glassCard()
        val mode = ModulePrefs.browserMode(prefs)
        val pinned = ModulePrefs.browserPackage(prefs)
        card.addView(
            ui.row(null, getString(R.string.link_browser), currentBrowserLabel(mode, pinned)) {
                showBrowserPicker()
            }
        )
        return card
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

    private fun buildCardModeCard(): LinearLayout {
        val card = ui.glassCard()
        val quiet = ModulePrefs.isLinkHintOnly(prefs)
        card.addView(
            ui.row(
                null,
                getString(R.string.link_card_mode),
                getString(if (quiet) R.string.link_card_quiet else R.string.link_card_expand)
            ) {
                val options = listOf(
                    QUIET_EXPAND to getString(R.string.link_card_expand),
                    QUIET_QUIET to getString(R.string.link_card_quiet)
                )
                PickerSheet.show(this, getString(R.string.link_card_pick), options) { key ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LINK_HINT_ONLY, key == QUIET_QUIET).apply()
                    toast(getString(R.string.ui_browser_saved))
                    rerender()
                }
            }
        )
        return card
    }

    private fun buildSelftestCard(): LinearLayout {
        val card = ui.glassCard()
        card.addView(
            ui.valueRow(getString(R.string.link_test), getString(R.string.link_test_short)) {
                postTestCard()
            }
        )
        card.addView(ui.divider())
        card.addView(
            ui.valueRow(getString(R.string.link_test_open), SELF_TEST_URL) {
                if (!LinkLauncher.open(this, SELF_TEST_URL)) {
                    toast(getString(R.string.link_test_fail))
                }
            }
        )
        return card
    }

    private fun postTestCard() {
        Thread {
            val ok = LinkHub.postBlocking(applicationContext, SELF_TEST_URL)
            val msg = getString(if (ok) R.string.link_test_posted else R.string.link_test_fail)
            main.post {
                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
            }
        }.apply { isDaemon = true }.start()
    }

    private fun buildFixCard(): LinearLayout {
        val card = ui.glassCard()
        card.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16))
        card.addView(
            ui.text(getString(R.string.link_fix_title), 13f, R.color.accent, bold = true)
        )
        card.addView(ui.gap(8))
        card.addView(
            ui.text(getString(R.string.link_fix_body), 11f, R.color.text_sub).apply {
                setLineSpacing(ui.dp(5).toFloat(), 1.12f)
            }
        )
        return card
    }

    private companion object {

        const val SELF_TEST_URL = "https://astraflow.cc/island"

        const val QUIET_EXPAND = "expand"
        const val QUIET_QUIET = "quiet"
    }
}
