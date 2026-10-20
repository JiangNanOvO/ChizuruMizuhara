package com.astraflow.Chizuru.ui.page

import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.capability.link.BrowserChoice
import com.astraflow.Chizuru.capability.link.ClipIntent
import com.astraflow.Chizuru.capability.link.ClipKind
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.capability.link.MapChoice
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.widget.ToggleSwitch

internal class LinkPage {

    private val main = Handler(Looper.getMainLooper())

    fun build(host: BaseActivity): View {
        val ui = host.ui
        val prefs = ModulePrefs.of(host)
        runCatching { LinkHub.warmUp(host) }

        val root = host.pageRoot(topDp = 14)
        root.addView(
            LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                addView(
                    ui.helpButton {
                        host.sheet(
                            host.getString(R.string.link_detail_title),
                            host.getString(R.string.link_privacy_body) + "\n\n" +
                                host.getString(R.string.link_scope_hint) + "\n\n" +
                                host.getString(R.string.link_fix_body),
                        )
                    },
                )
            },
        )
        root.addView(ui.gap(6))

        val title = ui.largeTitle(host.getString(R.string.link_detail_title))
        root.addView(title)
        root.addView(ui.gap(18))

        root.addView(ui.sectionHeader(host.getString(R.string.sec_status)))
        root.addView(statusCard(host, prefs))

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.sec_keepalive)))
        root.addView(keepAliveCard(host, prefs))

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.sec_clipboard)))
        root.addView(clipboardCard(host, prefs))

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.sec_browser)))
        root.addView(browserCard(host, prefs))

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.sec_selftest)))
        root.addView(selftestCard(host))

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(fixCard(host))

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.footerNote(host.getString(R.string.link_scope_hint)))
        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.footerNote(host.getString(R.string.link_privacy_body)))

        val scroll = ScrollView(host).apply {
            isFillViewport = true
            setBackgroundColor(host.theme.colors.systemGroupedBackground)
            addView(root)
        }
        host.bindLargeTitle(scroll, title)
        return scroll
    }

    private fun statusCard(host: BaseActivity, prefs: SharedPreferences): LinearLayout {
        val ui = host.ui
        val card = ui.glassCard()
        val on = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLinkEnabled(prefs)

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
                host.getString(if (on) R.string.ui_link_on else R.string.ui_link_off),
                15f,
                if (on) R.color.ok else R.color.text_hint,
            ).apply { setPadding(ui.dp(10), 0, 0, 0) },
            ui.weight(),
        )
        row.addView(
            ToggleSwitch(host).apply {
                setChecked(on, animate = false)
                onCheckedChange = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LINK, value).apply()
                    host.toast(host.getString(if (value) R.string.ui_link_on else R.string.ui_link_off))
                    host.reloadPage()
                }
            },
        )
        card.addView(row)
        card.addView(ui.divider(0))
        card.addView(
            ui.footerNote(
                host.getString(R.string.link_island_state) + "：" + islandStateLabel(host) +
                    "\n" + host.getString(R.string.link_sdk) + "：" + LinkHub.sdkVersion(),
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

        if (!ModuleEnabledState.isModuleEnabled(host)) {
            card.addView(ui.divider(0))
            card.addView(
                ui.footerNote(host.getString(R.string.link_module_off)).apply {
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

    private fun keepAliveCard(host: BaseActivity, prefs: SharedPreferences): LinearLayout {
        val ui = host.ui
        val card = ui.glassCard()
        card.addView(
            ui.switchRow(
                host.getString(R.string.link_keepalive_switch),
                ModulePrefs.isKeepAlive(prefs),
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_KEEP_ALIVE, value).apply()
                    if (value) {
                        host.askNotifyPermission()
                        host.toast(host.getString(R.string.link_keepalive_on))
                    } else {
                        Toast.makeText(host, host.getString(R.string.link_keepalive_off), Toast.LENGTH_SHORT)
                            .show()
                    }
                    host.syncKeepAlive()
                    host.reloadPage()
                },
            ),
        )
        card.addView(ui.divider(0))
        card.addView(
            ui.footerNote(host.getString(R.string.link_keepalive_desc)).apply {
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

    private fun clipboardCard(host: BaseActivity, prefs: SharedPreferences): LinearLayout {
        val ui = host.ui
        val card = ui.glassCard()
        card.addView(
            ui.switchRow(
                host.getString(R.string.link_clip_link),
                ModulePrefs.isLinkClipEnabled(prefs),
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_CLIP_LINK, value).apply()
                    host.reloadPage()
                },
            ),
        )
        card.addView(ui.divider())
        card.addView(
            ui.switchRow(
                host.getString(R.string.link_phone),
                ModulePrefs.isPhoneEnabled(prefs),
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_CLIP_PHONE, value).apply()
                    host.reloadPage()
                },
            ),
        )
        card.addView(ui.divider())
        card.addView(
            ui.switchRow(
                host.getString(R.string.link_address),
                ModulePrefs.isAddressEnabled(prefs),
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_CLIP_ADDRESS, value).apply()
                    host.reloadPage()
                },
            ),
        )
        return card
    }

    private fun browserCard(host: BaseActivity, prefs: SharedPreferences): LinearLayout {
        val ui = host.ui
        val card = ui.glassCard()
        card.addView(
            ui.row(
                iconRes = null,
                title = host.getString(R.string.link_browser),
                value = currentBrowserLabel(host, prefs),
                onClick = { showBrowserPicker(host, prefs) },
            ),
        )
        card.addView(ui.divider())
        card.addView(
            ui.row(
                iconRes = null,
                title = host.getString(R.string.link_map),
                value = currentMapLabel(host, prefs),
                onClick = { showMapPicker(host, prefs) },
            ),
        )
        card.addView(ui.divider())
        card.addView(
            ui.row(
                iconRes = null,
                title = host.getString(R.string.link_card_pick),
                value = if (ModulePrefs.isLinkHintOnly(prefs)) {
                    host.getString(R.string.link_card_quiet)
                } else {
                    host.getString(R.string.link_card_expand)
                },
                onClick = { showCardModePicker(host, prefs) },
            ),
        )
        return card
    }

    private fun selftestCard(host: BaseActivity): LinearLayout {
        val ui = host.ui
        val card = ui.glassCard()
        card.addView(
            ui.row(
                iconRes = null,
                title = host.getString(R.string.link_test),
                subtitle = host.getString(R.string.link_test_short),
                onClick = { postTestCard(host, ClipKind.LINK, SELF_TEST_URL) },
            ),
        )
        card.addView(ui.divider())
        card.addView(
            ui.row(
                iconRes = null,
                title = host.getString(R.string.link_test_phone),
                value = SELF_TEST_PHONE,
                onClick = { postTestCard(host, ClipKind.PHONE, SELF_TEST_PHONE) },
            ),
        )
        card.addView(ui.divider())
        card.addView(
            ui.row(
                iconRes = null,
                title = host.getString(R.string.link_test_address),
                value = SELF_TEST_ADDRESS,
                onClick = { postTestCard(host, ClipKind.ADDRESS, SELF_TEST_ADDRESS) },
            ),
        )
        return card
    }

    private fun fixCard(host: BaseActivity): LinearLayout {
        val ui = host.ui
        val card = ui.glassCard()
        val body = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                ui.dp(SectionTokens.HORIZONTAL_PADDING),
                ui.dp(14),
                ui.dp(SectionTokens.HORIZONTAL_PADDING),
                ui.dp(16),
            )
        }
        body.addView(ui.rowTitle(host.getString(R.string.link_fix_title)))
        body.addView(ui.gap(8))
        body.addView(ui.rowNote(host.getString(R.string.link_fix_body)).apply { setPadding(0, 0, 0, 0) })
        card.addView(body)
        return card
    }

    private fun showCardModePicker(host: BaseActivity, prefs: SharedPreferences) {
        val options = ArrayList<Pair<String, String>>()
        options.add("expand" to host.getString(R.string.link_card_expand))
        options.add("quiet" to host.getString(R.string.link_card_quiet))
        PickerSheet.show(host, host.getString(R.string.link_card_pick), options) { key ->
            prefs.edit().putBoolean(ModulePrefs.KEY_LINK_HINT_ONLY, key == "quiet").apply()
            host.reloadPage()
        }
    }

    private fun showMapPicker(host: BaseActivity, prefs: SharedPreferences) {
        val options = ArrayList<Pair<String, String>>()
        options.add(ModulePrefs.MAP_SYSTEM to host.getString(R.string.link_map_system))
        options.add(ModulePrefs.MAP_ASK to host.getString(R.string.link_map_ask))
        for (app in MapChoice.installed(host)) {
            options.add(app.packageName to app.label)
        }
        PickerSheet.show(host, host.getString(R.string.link_map_pick), options) { key ->
            val editor = prefs.edit()
            when (key) {
                ModulePrefs.MAP_SYSTEM ->
                    editor.putString(ModulePrefs.KEY_MAP_MODE, ModulePrefs.MAP_SYSTEM)

                ModulePrefs.MAP_ASK ->
                    editor.putString(ModulePrefs.KEY_MAP_MODE, ModulePrefs.MAP_ASK)

                else -> {
                    editor.putString(ModulePrefs.KEY_MAP_MODE, ModulePrefs.MAP_PINNED)
                    editor.putString(ModulePrefs.KEY_MAP_PACKAGE, key)
                }
            }
            editor.apply()
            host.toast(host.getString(R.string.ui_map_saved))
            host.reloadPage()
        }
    }

    private fun showBrowserPicker(host: BaseActivity, prefs: SharedPreferences) {
        val options = ArrayList<Pair<String, String>>()
        options.add(ModulePrefs.BROWSER_SYSTEM to host.getString(R.string.link_browser_system))
        options.add(ModulePrefs.BROWSER_ASK to host.getString(R.string.link_browser_ask))
        for (app in BrowserChoice.installed(host)) {
            options.add(app.packageName to app.label)
        }
        PickerSheet.show(host, host.getString(R.string.link_browser_pick), options) { key ->
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
            host.toast(host.getString(R.string.ui_browser_saved))
            host.reloadPage()
        }
    }

    private fun postTestCard(host: BaseActivity, kind: ClipKind, payload: String) {
        Thread {
            val ok = LinkHub.postBlocking(host.applicationContext, ClipIntent(kind, payload))
            val msg = host.getString(if (ok) R.string.link_test_posted else R.string.link_test_fail)
            main.post {
                Toast.makeText(host.applicationContext, msg, Toast.LENGTH_SHORT).show()
            }
        }.apply { isDaemon = true }.start()
    }

    private fun islandStateLabel(host: BaseActivity): String = when (LinkHub.stateName()) {
        "NOT_INSTALLED" -> host.getString(R.string.island_not_installed)
        "WAITING" -> host.getString(R.string.island_waiting)
        "REJECTED" -> host.getString(R.string.island_rejected)
        "READY" -> host.getString(R.string.island_ready)
        "NOT_CONNECTED" -> host.getString(R.string.island_waiting)
        else -> LinkHub.stateName()
    }

    private fun currentBrowserLabel(host: BaseActivity, prefs: SharedPreferences): String =
        when (ModulePrefs.browserMode(prefs)) {
            ModulePrefs.BROWSER_PINNED ->
                BrowserChoice.installed(host)
                    .firstOrNull { it.packageName == ModulePrefs.browserPackage(prefs) }?.label
                    ?: host.getString(R.string.link_browser_system)

            ModulePrefs.BROWSER_ASK -> host.getString(R.string.link_browser_ask)
            else -> host.getString(R.string.link_browser_system)
        }

    private fun currentMapLabel(host: BaseActivity, prefs: SharedPreferences): String =
        when (ModulePrefs.mapMode(prefs)) {
            ModulePrefs.MAP_PINNED ->
                MapChoice.installed(host)
                    .firstOrNull { it.packageName == ModulePrefs.mapPackage(prefs) }?.label
                    ?: host.getString(R.string.link_map_system)

            ModulePrefs.MAP_ASK -> host.getString(R.string.link_map_ask)
            else -> host.getString(R.string.link_map_system)
        }

    private companion object {
        const val SELF_TEST_PHONE = "18888888888"
        const val SELF_TEST_ADDRESS = "浙江省杭州市西湖区文三路 100 号"
        const val SELF_TEST_URL = "https://astraflow.cc/island"
    }
}
