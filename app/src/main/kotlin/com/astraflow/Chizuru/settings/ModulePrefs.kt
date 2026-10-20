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
    const val KEY_CLIP_LINK = "clip_link"
    const val KEY_CLIP_PHONE = "clip_phone"
    const val KEY_CLIP_ADDRESS = "clip_address"
    const val KEY_MAP_MODE = "map_mode"
    const val KEY_MAP_PACKAGE = "map_package"

    const val BROWSER_SYSTEM = "system"

    const val BROWSER_PINNED = "pinned"

    const val BROWSER_ASK = "ask"

    const val MAP_SYSTEM = "system"

    const val MAP_PINNED = "pinned"

    const val MAP_ASK = "ask"

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isEnabled(s: SharedPreferences): Boolean = s.getBoolean(KEY_ENABLED, true)

    fun isHideIcon(s: SharedPreferences): Boolean = s.getBoolean(KEY_HIDE_ICON, false)

    fun isLyricEnabled(s: SharedPreferences): Boolean = s.getBoolean(KEY_LYRIC, true)

    fun isLinkEnabled(s: SharedPreferences): Boolean = s.getBoolean(KEY_LINK, true)

    fun isLinkHintOnly(s: SharedPreferences): Boolean = s.getBoolean(KEY_LINK_HINT_ONLY, false)

    fun isKeepAlive(s: SharedPreferences): Boolean = s.getBoolean(KEY_KEEP_ALIVE, true)

    fun isStandaloneLyric(s: SharedPreferences): Boolean = s.getBoolean(KEY_LYRIC_STANDALONE, false)

    fun isNeriAdapt(s: SharedPreferences): Boolean = s.getBoolean(KEY_NERI_ADAPT, false)

    fun isLinkClipEnabled(s: SharedPreferences): Boolean = s.getBoolean(KEY_CLIP_LINK, true)

    fun isPhoneEnabled(s: SharedPreferences): Boolean = s.getBoolean(KEY_CLIP_PHONE, true)

    fun isAddressEnabled(s: SharedPreferences): Boolean = s.getBoolean(KEY_CLIP_ADDRESS, true)

    fun mapMode(s: SharedPreferences): String =
        s.getString(KEY_MAP_MODE, MAP_SYSTEM) ?: MAP_SYSTEM

    fun mapPackage(s: SharedPreferences): String =
        s.getString(KEY_MAP_PACKAGE, "").orEmpty()

    fun browserMode(s: SharedPreferences): String =
        s.getString(KEY_BROWSER_MODE, BROWSER_SYSTEM) ?: BROWSER_SYSTEM

    fun browserPackage(s: SharedPreferences): String =
        s.getString(KEY_BROWSER_PACKAGE, "").orEmpty()

    fun edit(context: Context): SharedPreferences.Editor = of(context).edit()
}
