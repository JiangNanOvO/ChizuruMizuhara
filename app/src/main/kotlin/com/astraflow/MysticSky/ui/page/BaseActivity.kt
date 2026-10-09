package com.astraflow.MysticSky.ui.page

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.astraflow.MysticSky.R
import com.astraflow.MysticSky.settings.ModulePrefs
import com.astraflow.MysticSky.ui.widget.Motion
import com.astraflow.MysticSky.ui.widget.UIKit

abstract class BaseActivity : Activity() {

    protected lateinit var ui: UIKit

    protected fun wrapPage(content: View, glow: Boolean = true): View {
        val root = FrameLayout(this)

        if (glow) {

            root.addView(
                View(this).apply {
                    background = getDrawable(R.drawable.bg_glow)
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, ui.dp(520)
                    ).apply { gravity = Gravity.TOP }
                }
            )
            Motion.breathe(root.getChildAt(0), minAlpha = 0.6f, periodMs = 4200L)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        container.setPadding(0, statusBarHeight(), 0, 0)
        container.addView(content)
        root.addView(container)
        return root
    }

    protected fun scrollable(inner: LinearLayout): ScrollView =
        ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(color(R.color.bg))
            addView(inner)
        }

    protected fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        val base = if (id > 0) resources.getDimensionPixelSize(id) else ui.dp(24)
        return base
    }

    protected fun color(res: Int): Int = getColor(res)

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
            ComponentName(manager, "org.lsposed.manager.ui.activity.MainActivity")
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
