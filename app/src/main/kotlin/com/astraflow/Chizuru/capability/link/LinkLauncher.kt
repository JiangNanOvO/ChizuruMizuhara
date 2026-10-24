package com.astraflow.Chizuru.capability.link

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.astraflow.Chizuru.settings.ModulePrefs

object LinkLauncher {

    private const val TAG = "Mitian-Link"

    fun openIntent(context: Context, url: String): PendingIntent? {
        val app = context.applicationContext
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val prefs = ModulePrefs.of(app)

        val target: Intent = when (ModulePrefs.browserMode(prefs)) {
            ModulePrefs.BROWSER_PINNED -> {
                val pkg = ModulePrefs.browserPackage(prefs)
                val view = viewIntent(uri)
                if (pkg.isNotBlank() && canHandle(app, view, pkg)) view.setPackage(pkg) else view
            }
            ModulePrefs.BROWSER_ASK ->
                Intent.createChooser(viewIntent(uri), CHOOSER_TITLE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            else -> viewIntent(uri)
        }

        return runCatching {
            PendingIntent.getActivity(
                app,
                url.hashCode(),
                target,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }.onFailure { Log.w(TAG, "openIntent build failed: ${it.message}") }.getOrNull()
    }

    fun open(context: Context, url: String): Boolean {
        val app = context.applicationContext
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val prefs = ModulePrefs.of(app)

        val intent: Intent = when (ModulePrefs.browserMode(prefs)) {
            ModulePrefs.BROWSER_PINNED -> {
                val pkg = ModulePrefs.browserPackage(prefs)
                val view = viewIntent(uri)
                if (pkg.isNotBlank() && canHandle(app, view, pkg)) view.setPackage(pkg) else view
            }
            ModulePrefs.BROWSER_ASK ->
                Intent.createChooser(viewIntent(uri), CHOOSER_TITLE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            else -> viewIntent(uri)
        }

        return runCatching {
            app.startActivity(intent)
            true
        }.onFailure { Log.w(TAG, "startActivity failed: ${it.message}") }.getOrDefault(false)
    }

    private fun viewIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun canHandle(context: Context, view: Intent, pkg: String): Boolean = runCatching {
        context.packageManager.resolveActivity(Intent(view).setPackage(pkg), 0) != null
    }.getOrDefault(false)

    private const val CHOOSER_TITLE = "选择浏览器"
}
