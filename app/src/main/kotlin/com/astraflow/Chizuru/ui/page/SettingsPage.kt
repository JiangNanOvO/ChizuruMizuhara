package com.astraflow.Chizuru.ui.page

import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.ios.CupertinoShapes
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.ios.Surfaces
import com.astraflow.Chizuru.ui.ios.clipRounded
import com.astraflow.Chizuru.ui.widget.ContributorWallView
import com.astraflow.Chizuru.ui.widget.Motion

internal class SettingsPage {

    fun build(host: BaseActivity): View {
        val ui = host.ui
        val prefs = ModulePrefs.of(host)

        val root = host.pageRoot(topDp = 14)
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

        val title = ui.largeTitle(host.getString(R.string.home_settings))
        root.addView(title)
        root.addView(ui.gap(18))

        root.addView(ui.sectionHeader(host.getString(R.string.sec_features)))
        val features = ui.glassCard()
        features.addView(
            ui.switchRow(
                title = host.getString(R.string.ui_switch_enable),
                checked = ModulePrefs.isEnabled(prefs),
                onToggle = { value ->
                    prefs.edit().putBoolean(ModulePrefs.KEY_ENABLED, value).apply()
                    if (!value) {
                        runCatching { KeepAliveService.stop(host) }
                    } else {
                        host.syncKeepAlive()
                    }
                    host.toast(host.getString(if (value) R.string.ui_enable_on else R.string.ui_enable_off))
                },
            ),
        )
        features.addView(ui.divider())
        features.addView(
            ui.switchRow(
                title = host.getString(R.string.ui_switch_hide),
                checked = ModulePrefs.isHideIcon(prefs),
                onToggle = { value ->
                    host.setLauncherIconHidden(value)
                    prefs.edit().putBoolean(ModulePrefs.KEY_HIDE_ICON, value).apply()
                    host.toast(host.getString(if (value) R.string.ui_hide_on else R.string.ui_hide_off))
                },
                note = host.getString(R.string.ui_switch_hide_hint),
            ),
        )
        features.addView(ui.divider())
        features.addView(
            ui.actionRow(host.getString(R.string.ui_btn_lsposed)) { host.openLsposed() },
        )
        root.addView(features)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(ui.sectionHeader(host.getString(R.string.sec_about)))
        val about = ui.glassCard()
        about.addView(
            ui.row(
                iconRes = null,
                title = host.getString(R.string.home_about),
                value = "v" + host.versionName(),
                onClick = {
                    host.sheet(
                        host.getString(R.string.ui_title_about),
                        host.getString(R.string.ui_about_body),
                    )
                },
            ),
        )
        about.addView(ui.divider())
        about.addView(
            ui.valueRow(
                host.getString(R.string.ui_title_author),
                host.getString(R.string.ui_sub_author),
            ),
        )

        about.addView(ui.divider())
        about.addView(
            ui.row(
                iconRes = null,
                title = host.getString(R.string.ui_title_free),
                onClick = {
                    host.sheet(
                        host.getString(R.string.ui_title_free),
                        host.getString(R.string.ui_free_body),
                    )
                },
            ),
        )

        root.addView(about)

        root.addView(ui.gap(SectionTokens.SPLIT_PADDING))
        root.addView(contributorsBlock(host))


        val scroll = ScrollView(host).apply {
            isFillViewport = true
            setBackgroundColor(host.theme.colors.systemGroupedBackground)
            addView(root)
        }
        host.bindLargeTitle(scroll, title)
        return scroll
    }

    private fun contributorsBlock(host: BaseActivity): View {
        val ui = host.ui
        val column = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }

        val icons = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        icons.addView(iconButton(host, R.drawable.ic_globe) { openUri(host, CONTRIBUTOR_WEBSITE) })
        icons.addView(
            iconButton(host, R.drawable.ic_mail) { openUri(host, "mailto:$CONTRIBUTOR_MAIL") },
            LinearLayout.LayoutParams(ui.dp(46), ui.dp(46)).apply { marginStart = ui.dp(16) },
        )
        icons.addView(
            iconButton(host, R.drawable.ic_github) { openUri(host, CONTRIBUTOR_GITHUB) },
            LinearLayout.LayoutParams(ui.dp(46), ui.dp(46)).apply { marginStart = ui.dp(16) },
        )
        column.addView(icons)
        column.addView(ui.gap(SectionTokens.SPLIT_PADDING))

        val header = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                ui.dp(SectionTokens.HEADER_INSET),
                0,
                ui.dp(SectionTokens.HEADER_INSET),
                ui.dp(7),
            )
        }
        header.addView(ui.text(host.getString(R.string.contributors), 13f, R.color.text_sub))
        header.addView(
            ui.text(CONTRIBUTORS.size.toString(), 11f, R.color.text_hint).apply {
                background = Surfaces.rounded(ui.colors.secondarySystemFill, 9f)
                clipRounded(9f)
                setPadding(ui.dp(7), ui.dp(1), ui.dp(7), ui.dp(2))
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { marginStart = ui.dp(8) },
        )
        column.addView(header)

        val card = ui.glassCard()
        val cloud = ContributorWallView(host).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ui.dp(240),
            ).apply { setMargins(ui.dp(12), ui.dp(14), ui.dp(12), ui.dp(14)) }
            setEntries(CONTRIBUTORS.map { ContributorWallView.Entry(it.avatar) })
            onPick = { index ->
                CONTRIBUTORS.getOrNull(index)?.let { openContributor(host, it) }
            }
        }
        card.addView(cloud)
        column.addView(card)

        column.addView(ui.gap(8))
        column.addView(ui.footerNote(host.getString(R.string.contributors_hint)))
        return column
    }

    private fun iconButton(host: BaseActivity, iconRes: Int, onClick: () -> Unit): View {
        val ui = host.ui
        val button = FrameLayout(host).apply {
            background = Surfaces.circle(ui.colors.secondarySystemFill)
            layoutParams = LinearLayout.LayoutParams(ui.dp(46), ui.dp(46))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        button.addView(
            ui.icon(iconRes, R.color.text, 20).apply {
                layoutParams = FrameLayout.LayoutParams(ui.dp(20), ui.dp(20)).apply {
                    gravity = Gravity.CENTER
                }
            },
        )
        Motion.pressFeedback(button)
        return button
    }

    private fun openContributor(host: BaseActivity, item: Contributor) {
        ContributorDetailDialog(host, item).show()
    }

    private fun openUri(host: BaseActivity, uri: String) {
        val opened = runCatching {
            host.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        }.isSuccess
        if (!opened) {
            runCatching {
                val cm = host.getSystemService(android.content.ClipboardManager::class.java)
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("link", uri))
            }
            host.toast(host.getString(R.string.ui_msg_copied))
        }
    }

}
