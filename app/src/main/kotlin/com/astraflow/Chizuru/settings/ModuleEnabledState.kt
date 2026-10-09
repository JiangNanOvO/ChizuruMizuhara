package com.astraflow.Chizuru.settings

import android.content.Context
import android.content.Intent

object ModuleEnabledState {

    const val KEY_FRAMEWORK_LOADED = "framework_loaded"

    const val KEY_FRAMEWORK_NAME = "framework_name"

    const val KEY_FRAMEWORK_AT = "framework_loaded_at"

    const val KEY_MODULE_VERSION = "module_version"

    fun isModuleEnabled(context: Context): Boolean = runCatching {
        ModulePrefs.of(context).getBoolean(KEY_FRAMEWORK_LOADED, false)
    }.getOrDefault(false)

    fun frameworkName(context: Context): String? = runCatching {
        ModulePrefs.of(context).getString(KEY_FRAMEWORK_NAME, null)
    }.getOrNull()

    private const val ACTION_ALIVE = "com.astraflow.Chizuru.action.ALIVE"
    private const val EXTRA_FRAMEWORK = "framework"

    @Volatile
    private var listening = false

    fun startListening(context: Context) {
        if (listening) return
        listening = true
        val app = context.applicationContext
        runCatching {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action != ACTION_ALIVE) return
                    markLoaded(app, intent.getStringExtra(EXTRA_FRAMEWORK))
                }
            }

            app.registerReceiver(
                receiver,
                android.content.IntentFilter(ACTION_ALIVE),
                Context.RECEIVER_EXPORTED
            )
        }
    }

    fun markLoaded(context: Context, framework: String?, moduleVersion: Int = 0) {
        runCatching {
            ModulePrefs.of(context).edit()
                .putBoolean(KEY_FRAMEWORK_LOADED, true)
                .putString(KEY_FRAMEWORK_NAME, framework)
                .putLong(KEY_FRAMEWORK_AT, System.currentTimeMillis())
                .putInt(KEY_MODULE_VERSION, moduleVersion)
                .apply()
        }
    }

    fun moduleVersion(context: Context): Int = runCatching {
        ModulePrefs.of(context).getInt(KEY_MODULE_VERSION, 0)
    }.getOrDefault(0)

}
