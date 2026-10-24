package com.astraflow.Chizuru.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.widget.Toast

/**
 * 页面里要做的系统动作:打开 LSPosed、隐藏桌面图标、申请通知权限、打开外链。
 * 这些与界面部件无关,照原来的做法直接调系统的接口。
 */
object AppActions {

    /** 打开 LSPosed 管理器里本插件的模块页;没有管理器或打不开时返回假。 */
    fun openLsposed(context: Context): Boolean {
        val userId = Process.myUid() / 100_000
        for (manager in listOf("org.lsposed.manager", "io.github.lsposed.manager")) {
            if (runCatching { context.packageManager.getPackageInfo(manager, 0) }.isFailure) continue
            val modulePage = Intent(Intent.ACTION_VIEW)
                .setClassName(manager, "org.lsposed.manager.ui.activity.MainActivity")
                .setData(Uri.parse("module://${context.packageName}:$userId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(modulePage) }.isSuccess) return true
            val home = context.packageManager.getLaunchIntentForPackage(manager)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?: continue
            if (runCatching { context.startActivity(home) }.isSuccess) return true
        }
        return false
    }

    /** 隐藏或恢复桌面图标(桌面别名,隐藏后仍可从 LSPosed 进入本页)。 */
    fun setLauncherIconHidden(context: Context, hidden: Boolean) {
        val alias = ComponentName(context, "${context.packageName}.ui.LauncherAlias")
        val state = if (hidden) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        runCatching { context.packageManager.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP) }
    }

    /** 申请通知权限(保活要用一条静默通知)。 */
    fun requestNotifyPermission(context: Context) {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) Toast.makeText(context, "请在系统设置里允许本插件发送通知，保活才有用", Toast.LENGTH_LONG).show()
    }

    /** 打开外链;打不开(没有浏览器)时把地址抄进剪贴板并提示。 */
    fun openUri(context: Context, uri: String, fallbackCopy: Boolean = true) {
        val opened = runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (opened) return
        if (fallbackCopy) {
            runCatching {
                val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("link", uri))
            }
            Toast.makeText(context, "没找到可用的应用，地址已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    /** 当前安装包的版本名。 */
    fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
}
