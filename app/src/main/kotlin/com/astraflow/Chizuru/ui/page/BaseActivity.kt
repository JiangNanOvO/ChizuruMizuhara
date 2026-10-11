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
import com.astraflow.Chizuru.ui.ios.SectionTokens
import com.astraflow.Chizuru.ui.ios.Theme
import com.astraflow.Chizuru.ui.widget.Motion
import com.astraflow.Chizuru.ui.widget.UIKit

abstract class BaseActivity : Activity() {

    protected lateinit var ui: UIKit

    protected val theme: Theme get() = Theme.of(this)

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

    protected fun scrollable(inner: LinearLayout): ScrollView =
        ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(theme.colors.systemGroupedBackground)
            addView(inner)
        }

    protected fun bindLargeTitle(scroll: ScrollView, title: View) {
        Motion.bindLargeTitle(scroll, title)
    }

    protected fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else ui.dp(24)
    }

    protected fun pagePadding(): Int = ui.dp(SectionTokens.PAGE_INSET)

    protected fun color(res: Int): Int = ui.color(res)

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

    protected fun topInset(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return statusBarHeight()
        val insets = window.decorView.rootWindowInsets ?: return statusBarHeight()
        return insets.getInsets(WindowInsets.Type.statusBars()).top
    }

    protected fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    protected fun openPage(cls: Class<*>) {
        startActivity(Intent(this, cls))
    }

    protected fun openLsposed() {
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

    protected fun setLauncherIconHidden(hidden: Boolean) {
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

    protected fun versionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "?"
}
