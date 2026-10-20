package com.astraflow.Chizuru.capability.link

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.astraflow.Chizuru.settings.ModulePrefs
import java.net.URLEncoder

object MapChoice {

    private const val TAG = "Mitian-Link"

    data class MapApp(val label: String, val packageName: String)

    fun installed(context: Context): List<MapApp> {
        val pm = context.packageManager
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=test"))
        val resolved = runCatching {
            pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrDefault(emptyList())
        return resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
                MapApp(label, pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label }
    }

    fun openIntent(context: Context, address: String): PendingIntent? {
        val app = context.applicationContext
        val prefs = ModulePrefs.of(app)
        val view = geoIntent(address)
        val target: Intent = when (ModulePrefs.mapMode(prefs)) {
            ModulePrefs.MAP_PINNED -> {
                val pkg = ModulePrefs.mapPackage(prefs)
                if (pkg.isNotBlank() && canHandle(app, view, pkg)) geoIntent(address).setPackage(pkg)
                else view
            }
            ModulePrefs.MAP_ASK -> Intent.createChooser(geoIntent(address), CHOOSER_TITLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            else -> view
        }
        return runCatching {
            PendingIntent.getActivity(
                app,
                address.hashCode(),
                target,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }.onFailure { Log.w(TAG, "map intent build failed: ${it.message}") }.getOrNull()
    }

    fun open(context: Context, address: String): Boolean {
        val app = context.applicationContext
        val prefs = ModulePrefs.of(app)
        val target: Intent = when (ModulePrefs.mapMode(prefs)) {
            ModulePrefs.MAP_PINNED -> {
                val pkg = ModulePrefs.mapPackage(prefs)
                if (pkg.isNotBlank() && canHandle(app, geoIntent(address), pkg)) {
                    geoIntent(address).setPackage(pkg)
                } else geoIntent(address)
            }
            ModulePrefs.MAP_ASK -> Intent.createChooser(geoIntent(address), CHOOSER_TITLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            else -> geoIntent(address)
        }
        return runCatching {
            app.startActivity(target)
            true
        }.onFailure { Log.w(TAG, "map startActivity failed: ${it.message}") }.getOrDefault(false)
    }

    private fun geoIntent(address: String): Intent {
        val query = runCatching { URLEncoder.encode(address, "UTF-8") }.getOrDefault(address)
        val uri = Uri.parse("geo:0,0?q=" + query)
        return Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun canHandle(context: Context, view: Intent, pkg: String): Boolean = runCatching {
        context.packageManager.resolveActivity(Intent(view).setPackage(pkg), 0) != null
    }.getOrDefault(false)

    private const val CHOOSER_TITLE = "选择地图"
}
