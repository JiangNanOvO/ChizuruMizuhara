package com.astraflow.Chizuru.capability.link

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object BrowserChoice {

    data class BrowserApp(val label: String, val packageName: String)

    fun installed(context: Context): List<BrowserApp> {
        val pm = context.packageManager
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val resolved = runCatching {
            pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrDefault(emptyList())
        return resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
                BrowserApp(label, pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label }
    }
}
