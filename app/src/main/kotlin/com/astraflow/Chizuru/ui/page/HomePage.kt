package com.astraflow.Chizuru.ui.page

import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.capability.lyricprovider.StandaloneLyric
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.ios.CupertinoShapes
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.ios.Surfaces
import com.astraflow.Chizuru.ui.ios.clipRounded
import com.astraflow.Chizuru.ui.widget.UIKit

internal class HomePage {

    fun build(host: BaseActivity, onSelectTab: (Int) -> Unit): View {
        val ui = host.ui
        val prefs = ModulePrefs.of(host)
        val injected = ModuleEnabledState.isModuleEnabled(host)
        val lyricOn = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLyricEnabled(prefs)
        val linkOn = ModulePrefs.isEnabled(prefs) && ModulePrefs.isLinkEnabled(prefs)
        val standalone = ModulePrefs.isStandaloneLyric(prefs)
        val providers = runCatching { StandaloneLyric.providerCount() }.getOrDefault(0)

        val root = host.pageRoot()

        root.addView(
            LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                addView(
                    ui.helpButton {
                        host.sheet(
                            host.getString(R.string.ui_title_about),
                            host.getString(R.string.ui_about_body),
                        )
                    },
                )
            },
        )
        root.addView(ui.gap(6))

        val title = ui.largeTitle(host.getString(R.string.app_name_launcher))
        root.addView(title)
        root.addView(ui.gap(18))

        root.addView(heroCard(host, injected))
        if (!injected) {
            root.addView(ui.gap(8))
            root.addView(ui.footerNote(host.getString(R.string.home_not_active)))
        }

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.sec_caps)))
        val lyricText = tileValue(host, lyricState(host, lyricOn, standalone, providers), lyricOn)
        val linkText = tileValue(host, onOff(host, linkOn), linkOn)
        val islandText = tileValue(host, islandLabel(host), LinkHub.isReady())
        val keepText = tileValue(
            host,
            host.getString(
                if (KeepAliveService.running) R.string.state_running else R.string.state_stopped,
            ),
            KeepAliveService.running,
        )
        root.addView(
            tileRow(
                host,
                tile(host, R.drawable.ic_tab_lyric, host.getString(R.string.cap_lyric), lyricText, lyricOn) {
                    onSelectTab(2)
                },
                tile(host, R.drawable.ic_tab_link, host.getString(R.string.cap_link), linkText, linkOn) {
                    onSelectTab(1)
                },
            ),
        )
        root.addView(ui.gap(12))
        root.addView(
            tileRow(
                host,
                tile(host, R.drawable.ic_star, host.getString(R.string.sec_island), islandText, LinkHub.isReady()) {
                    onSelectTab(1)
                },
                tile(
                    host,
                    R.drawable.ic_tab_settings,
                    host.getString(R.string.link_keepalive_switch),
                    keepText,
                    KeepAliveService.running,
                ) { onSelectTab(3) },
            ),
        )

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.sec_quick)))
        val quick = ui.glassCard()
        quick.addView(
            ui.switchRow(
                host.getString(R.string.cap_lyric),
                lyricOn,
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LYRIC, value).apply()
                    host.toast(host.getString(if (value) R.string.ui_enable_on else R.string.ui_enable_off))
                    host.reloadPage()
                },
                onOpen = { onSelectTab(2) },
            ),
        )
        quick.addView(ui.divider())
        quick.addView(
            ui.switchRow(
                host.getString(R.string.cap_link),
                linkOn,
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_LINK, value).apply()
                    host.toast(host.getString(if (value) R.string.ui_link_on else R.string.ui_link_off))
                    host.reloadPage()
                },
                onOpen = { onSelectTab(1) },
            ),
        )
        root.addView(quick)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.sec_more)))
        val more = ui.glassCard()
        more.addView(
            ui.actionRow(host.getString(R.string.home_settings)) { onSelectTab(3) },
        )
        root.addView(more)

        val scroll = ScrollView(host).apply {
            isFillViewport = true
            setBackgroundColor(host.theme.colors.systemGroupedBackground)
            addView(root)
        }
        host.bindLargeTitle(scroll, title)
        watchStatus(host, islandText, keepText)
        return scroll
    }

    private fun heroCard(host: BaseActivity, injected: Boolean): View {
        val ui = host.ui
        val card = ui.glassCard()
        val inner = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(18), ui.dp(16), ui.dp(18), ui.dp(18))
        }
        val top = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(ui.statusDot(if (injected) R.color.ok else R.color.warn))
        top.addView(
            ui.text(
                host.getString(
                    if (injected) R.string.home_state_injected else R.string.home_state_missing,
                ),
                20f,
                R.color.text,
                bold = true,
            ).apply { setPadding(ui.dp(10), 0, 0, 0) },
            ui.weight(),
        )
        top.addView(ui.text("v" + host.versionName(), 13f, R.color.text_sub))
        inner.addView(top)
        inner.addView(ui.gap(10))
        inner.addView(
            ui.rowNote(
                host.getString(
                    if (injected) R.string.home_hint_injected else R.string.home_hint_missing,
                ),
            ),
        )
        card.addView(inner)
        return card
    }

    private fun tileValue(host: BaseActivity, text: String, on: Boolean): TextView =
        host.ui.text(text, 16f, if (on) R.color.text else R.color.text_hint, bold = true).apply {
            setPadding(0, host.ui.dp(4), 0, 0)
        }

    private fun tile(
        host: BaseActivity,
        icon: Int,
        label: String,
        valueText: TextView,
        on: Boolean,
        onClick: () -> Unit,
    ): View {
        val ui = host.ui
        val card = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = Surfaces.groupedCard(ui.colors)
            clipRounded(CupertinoShapes.EXTRA_LARGE)
            setPadding(ui.dp(14), ui.dp(13), ui.dp(14), ui.dp(14))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        card.addView(ui.icon(icon, if (on) R.color.accent else R.color.icon, 20))
        card.addView(ui.gap(12))
        card.addView(ui.text(label, 13f, R.color.text_sub))
        card.addView(valueText)
        return card
    }

    private fun tileRow(host: BaseActivity, left: View, right: View): LinearLayout {
        val ui = host.ui
        val row = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(
            left,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = ui.dp(6) },
        )
        row.addView(
            right,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = ui.dp(6) },
        )
        return row
    }

    private fun watchStatus(host: BaseActivity, islandText: TextView, keepText: TextView) {
        val handler = Handler(Looper.getMainLooper())
        val tick = object : Runnable {
            private var left = 12

            override fun run() {
                if (!islandText.isAttachedToWindow || left-- <= 0) return
                val island = islandLabel(host)
                if (islandText.text != island) islandText.text = island
                val keep = host.getString(
                    if (KeepAliveService.running) R.string.state_running else R.string.state_stopped,
                )
                if (keepText.text != keep) keepText.text = keep
                handler.postDelayed(this, 400)
            }
        }
        handler.postDelayed(tick, 400)
    }

    private fun lyricState(
        host: BaseActivity,
        on: Boolean,
        standalone: Boolean,
        providers: Int,
    ): String = when {
        !on -> host.getString(R.string.state_off)
        standalone && providers > 0 -> host.getString(R.string.home_lyric_providers, providers)
        else -> host.getString(R.string.state_on)
    }

    private fun onOff(host: BaseActivity, value: Boolean): String =
        host.getString(if (value) R.string.state_on else R.string.state_off)

    private fun islandLabel(host: BaseActivity): String = when (LinkHub.stateName()) {
        "NOT_INSTALLED" -> host.getString(R.string.island_not_installed)
        "WAITING" -> host.getString(R.string.island_waiting)
        "REJECTED" -> host.getString(R.string.island_rejected)
        "READY" -> host.getString(R.string.island_ready)
        "NOT_CONNECTED" -> host.getString(R.string.island_waiting)
        else -> LinkHub.stateName()
    }
}
