package com.astraflow.Chizuru.settings

import android.content.Context

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
