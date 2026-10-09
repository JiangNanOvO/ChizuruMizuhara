package com.astraflow.Chizuru.settings

import android.content.Context
import android.content.SharedPreferences

object ModulePrefs {

    const val NAME = "mitian_settings"

    const val KEY_ENABLED = "module_enabled"
    const val KEY_HIDE_ICON = "hide_launcher_icon"
    const val KEY_LYRIC = "cap_lyric_enabled"
    const val KEY_LINK = "cap_link_enabled"
    const val KEY_BROWSER_MODE = "link_browser_mode"
    const val KEY_BROWSER_PACKAGE = "link_browser_package"
    const val KEY_LINK_HINT_ONLY = "cap_link_hint_only"
    const val KEY_KEEP_ALIVE = "link_keep_alive"
    const val KEY_LYRIC_STANDALONE = "lyric_standalone"
    const val KEY_NERI_ADAPT = "cap_neri_adapt"

    const val BROWSER_SYSTEM = "system"

    const val BROWSER_PINNED = "pinned"

    const val BROWSER_ASK = "ask"

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isEnabled(s: SharedPreferences): Boolean = s.getBoolean(KEY_ENABLED, true)

    fun isHideIcon(s: SharedPreferences): Boolean = s.getBoolean(KEY_HIDE_ICON, false)

    fun isLyricEnabled(s: SharedPreferences): Boolean = s.getBoolean(KEY_LYRIC, true)

    fun isLinkEnabled(s: SharedPreferences): Boolean = s.getBoolean(KEY_LINK, true)

    fun isLinkHintOnly(s: SharedPreferences): Boolean = s.getBoolean(KEY_LINK_HINT_ONLY, false)

    fun isKeepAlive(s: SharedPreferences): Boolean = s.getBoolean(KEY_KEEP_ALIVE, true)

    fun isStandaloneLyric(s: SharedPreferences): Boolean = s.getBoolean(KEY_LYRIC_STANDALONE, true)

    fun isNeriAdapt(s: SharedPreferences): Boolean = s.getBoolean(KEY_NERI_ADAPT, false)

    fun browserMode(s: SharedPreferences): String =
        s.getString(KEY_BROWSER_MODE, BROWSER_SYSTEM) ?: BROWSER_SYSTEM

    fun browserPackage(s: SharedPreferences): String =
        s.getString(KEY_BROWSER_PACKAGE, "").orEmpty()

    fun edit(context: Context): SharedPreferences.Editor = of(context).edit()
}
