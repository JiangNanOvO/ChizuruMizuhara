package com.astraflow.Chizuru.ui.page

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.astraflow.Chizuru.R
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.ios.Theme
import com.astraflow.Chizuru.ui.widget.Motion
import com.astraflow.Chizuru.ui.widget.UIKit

abstract class BaseActivity : Activity() {

    internal lateinit var ui: UIKit

    internal val theme: Theme get() = Theme.of(this)

    protected fun wrapPage(content: View, glow: Boolean = false): View {
        val root = FrameLayout(this)
        root.setBackgroundColor(theme.colors.systemGroupedBackground)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, statusBarHeight(), 0, 0)
        }
        container.addView(content)
        root.addView(container)
        return root
    }

    internal fun scrollable(inner: LinearLayout): ScrollView =
        ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(theme.colors.systemGroupedBackground)
            addView(inner)
        }

    internal fun bindLargeTitle(scroll: ScrollView, title: View) {
        Motion.bindLargeTitle(scroll, title)
    }

    internal fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else ui.dp(24)
    }

    internal fun pagePadding(): Int = ui.dp(SectionTokens.PAGE_INSET)

    internal fun color(res: Int): Int = ui.color(res)

    override fun onResume() {
        super.onResume()
        syncSystemBars()
    }

    protected fun syncSystemBars() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        val light = !theme.colors.isDark
        var flags = window.decorView.systemUiVisibility
        flags = if (light) {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        window.decorView.systemUiVisibility = flags

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    internal fun bottomInset(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return 0
        val insets = window.decorView.rootWindowInsets ?: return 0
        return insets.getInsets(WindowInsets.Type.navigationBars()).bottom
    }

    internal fun topInset(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return statusBarHeight()
        val insets = window.decorView.rootWindowInsets ?: return statusBarHeight()
        return insets.getInsets(WindowInsets.Type.statusBars()).top
    }

    internal fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    internal var pageReloader: (() -> Unit)? = null

    private var reloadPending = false

    internal fun reloadPage() {
        if (reloadPending) return
        reloadPending = true
        window.decorView.postDelayed({
            reloadPending = false
            pageReloader?.invoke()
        }, 240L)
    }

    internal fun pageRoot(topDp: Int = 20): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            ui.dp(SectionTokens.PAGE_INSET),
            topInset() + ui.dp(topDp),
            ui.dp(SectionTokens.PAGE_INSET),
            ui.dp(96) + bottomInset(),
        )
    }

    internal fun sheet(title: String, body: String) {
        startActivity(
            Intent(this, SheetActivity::class.java)
                .putExtra(SheetActivity.EXTRA_TITLE, title)
                .putExtra(SheetActivity.EXTRA_BODY, body),
        )
    }

    internal fun syncKeepAlive() {
        val prefs = ModulePrefs.of(this)
        val want = ModulePrefs.isEnabled(prefs) && (
            (ModulePrefs.isLyricEnabled(prefs) && ModulePrefs.isStandaloneLyric(prefs)) ||
                (ModulePrefs.isLinkEnabled(prefs) && ModulePrefs.isKeepAlive(prefs))
            )
        if (want) KeepAliveService.start(this) else KeepAliveService.stop(this)
    }

    internal fun askNotifyPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            runCatching {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 9001)
            }
        }
    }

    internal fun openPage(cls: Class<*>) {
        startActivity(Intent(this, cls))
    }

    internal fun openLsposed() {
        val manager = "org.lsposed.manager"
        val launcher = packageManager.getLaunchIntentForPackage(manager)
        if (launcher != null) {
            runCatching { startActivity(launcher) }
                .onFailure { toast(getString(R.string.ui_need_lsposed)) }
            return
        }
        val explicit = Intent().setComponent(
            ComponentName(manager, "org.lsposed.manager.ui.activity.MainActivity"),
        )
        runCatching { startActivity(explicit) }
            .onFailure { toast(getString(R.string.ui_need_lsposed)) }
    }

    internal fun setLauncherIconHidden(hidden: Boolean) {
        val alias = ComponentName(this, "$packageName.ui.LauncherAlias")
        val state = if (hidden) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        runCatching {
            packageManager.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP)
        }
    }

    internal fun versionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "?"
}
