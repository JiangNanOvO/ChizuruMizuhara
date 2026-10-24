package com.astraflow.Chizuru.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs

/**
 * 页面读的一份设置快照:插件设置一改(哪怕改的人是别的入口),页面跟着重画。
 * 插件的设置保存在本应用自己的 SharedPreferences 里(模块在别的进程里通过远端设置读同一份)。
 */
class PluginPrefs internal constructor(
    val prefs: SharedPreferences,
    val enabled: Boolean,
    val hideIcon: Boolean,
    val lyric: Boolean,
    val link: Boolean,
    val lyricStandalone: Boolean,
    val neriAdapt: Boolean,
    val keepAlive: Boolean,
    val clipLink: Boolean,
    val clipPhone: Boolean,
    val clipAddress: Boolean,
    val browserMode: String,
    val browserPackage: String,
    val mapMode: String,
    val mapPackage: String,
    val hintOnly: Boolean,
    val moduleLoaded: Boolean,
) {
    val lyricOn: Boolean get() = enabled && lyric
    val linkOn: Boolean get() = enabled && link
}

@Composable
fun rememberPluginPrefs(): PluginPrefs {
    val context = LocalContext.current
    val prefs = remember { ModulePrefs.of(context) }
    var version by remember { mutableStateOf(0) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    // 页面每次回到前台也重读一次(别的进程写进来的值不一定有回调)
    val loaded = remember(version) { ModuleEnabledState.isModuleEnabled(context) }
    return PluginPrefs(
        prefs = prefs,
        enabled = ModulePrefs.isEnabled(prefs),
        hideIcon = ModulePrefs.isHideIcon(prefs),
        lyric = ModulePrefs.isLyricEnabled(prefs),
        link = ModulePrefs.isLinkEnabled(prefs),
        lyricStandalone = ModulePrefs.isStandaloneLyric(prefs),
        neriAdapt = ModulePrefs.isNeriAdapt(prefs),
        keepAlive = ModulePrefs.isKeepAlive(prefs),
        clipLink = ModulePrefs.isLinkClipEnabled(prefs),
        clipPhone = ModulePrefs.isPhoneEnabled(prefs),
        clipAddress = ModulePrefs.isAddressEnabled(prefs),
        browserMode = ModulePrefs.browserMode(prefs),
        browserPackage = ModulePrefs.browserPackage(prefs),
        mapMode = ModulePrefs.mapMode(prefs),
        mapPackage = ModulePrefs.mapPackage(prefs),
        hintOnly = ModulePrefs.isLinkHintOnly(prefs),
        moduleLoaded = loaded,
    )
}

/**
 * 星河岛连接与保活服务的即时状态:这两样会自己变,页面开着时定时刷新。
 * 只读,不改任何东西。
 */
class LiveStatus(val islandState: String, val islandReady: Boolean, val keepAliveRunning: Boolean, val sdkVersion: String)

@Composable
fun rememberLiveStatus(tickMs: Long = 600L): LiveStatus {
    var tick by remember { mutableStateOf(0) }
    val current = remember(tick) {
        LiveStatus(
            islandState = runCatching { LinkHub.stateName() }.getOrDefault("NOT_CONNECTED"),
            islandReady = runCatching { LinkHub.isReady() }.getOrDefault(false),
            keepAliveRunning = KeepAliveService.running,
            sdkVersion = runCatching { LinkHub.sdkVersion() }.getOrDefault("?"),
        )
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(tickMs)
            tick++
        }
    }
    return current
}

/** 星河岛连接状态写成一句话。 */
fun islandStateLabel(state: String): String = when (state) {
    "READY" -> "已连接"
    "NOT_INSTALLED" -> "未安装星流"
    "REJECTED" -> "被拒绝"
    "WAITING", "NOT_CONNECTED" -> "未就绪"
    else -> state
}
