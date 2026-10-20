package com.astraflow.Chizuru.ui.page

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.ui.glass.GlassBottomBar
import com.astraflow.Chizuru.ui.widget.UIKit
import com.astraflow.Chizuru.ui.widget.WatermarkView

open class ChizuruShellActivity : BaseActivity() {

    protected open val initialTab: Int get() = 0

    private lateinit var bar: GlassBottomBar
    private lateinit var pager: PagePager
    private val views = arrayOfNulls<View>(TAB_COUNT)
    private var selected = 0
    private var resumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = UIKit(this)
        pageReloader = { reload(selected) }

        val root = FrameLayout(this)
        root.setBackgroundColor(theme.colors.systemGroupedBackground)

        pager = PagePager(this).apply {
            setBackgroundColor(theme.colors.systemGroupedBackground)
            onTarget = { index -> syncBar(index) }
            onSettled = { index -> selected = index }
        }
        root.addView(pager, fill())

        bar = GlassBottomBar(this, pager)
        bar.setTabs(
            arrayOf(
                getString(R.string.tab_home),
                getString(R.string.tab_link),
                getString(R.string.tab_lyric),
                getString(R.string.tab_settings),
            ),
            intArrayOf(
                R.drawable.ic_tab_home,
                R.drawable.ic_tab_link,
                R.drawable.ic_tab_lyric,
                R.drawable.ic_tab_settings,
            ),
        )
        bar.setOnTabSelected { index -> show(index) }

        val barParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        ).apply {
            bottomMargin = ui.dp(20) - bar.shadowPad() + bottomInset()
        }
        root.addView(bar.view(), barParams)
        root.addView(WatermarkView(this), fill())

        setContentView(root)

        syncKeepAlive()
        for (index in 0 until TAB_COUNT) {
            val page = build(index)
            views[index] = page
            pager.addPage(page)
        }
        selected = initialTab.coerceIn(0, TAB_COUNT - 1)
        pager.show(selected, false)
        bar.setSelected(selected)
    }

    override fun onResume() {
        super.onResume()
        syncKeepAlive()
        runCatching { LinkHub.warmUp(this) }
        if (resumed) reload(0) else resumed = true
    }

    private fun show(index: Int) {
        if (index < 0 || index >= TAB_COUNT) return
        selected = index
        pager.show(index, true)
        syncBar(index)
    }

    private fun syncBar(index: Int) {
        if (bar.selectedIndex() != index) bar.setSelected(index)
    }

    private fun reload(index: Int) {
        if (index < 0 || index >= TAB_COUNT) return
        val old = views[index] ?: return
        val offset = (old as? ScrollView)?.scrollY ?: 0
        val fresh = build(index)
        views[index] = fresh
        pager.replacePage(index, fresh)
        if (offset > 0 && fresh is ScrollView) {
            fresh.post { fresh.scrollTo(0, offset) }
        }
    }

    private fun build(index: Int): View = when (index) {
        0 -> HomePage().build(this) { tab -> show(tab) }
        1 -> LinkPage().build(this)
        2 -> LyricPage().build(this)
        else -> SettingsPage().build(this)
    }

    private fun fill(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )

    private companion object {
        const val TAB_COUNT = 4
    }
}
