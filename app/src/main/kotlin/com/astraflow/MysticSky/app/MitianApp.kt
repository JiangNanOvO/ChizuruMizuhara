package com.astraflow.MysticSky.app

import android.app.Application
import android.util.Log
import com.astraflow.MysticSky.capability.link.LinkHub
import com.astraflow.MysticSky.keepalive.KeepAliveService
import com.astraflow.MysticSky.settings.ModuleEnabledState
import com.astraflow.MysticSky.settings.ModulePrefs
import com.astraflow.MysticSky.capability.lyricprovider.StandaloneLyric
import com.astraflow.MysticSky.capability.neriplayer.NeriSettingPatcher

class MitianApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ModuleEnabledState.startListening(this)
        val prefs = ModulePrefs.of(this)

        if (ModulePrefs.isEnabled(prefs) && ModulePrefs.isNeriAdapt(prefs)) {
            Thread {
                runCatching { NeriSettingPatcher.ensureEnabled(this) }
                    .onFailure { Log.w(TAG, "neri adapt failed: ${'$'}{it.message}") }
            }.apply { isDaemon = true; name = "mitian-neri" }.start()
        }

        if (ModulePrefs.isEnabled(prefs) && ModulePrefs.isLyricEnabled(prefs) && ModulePrefs.isStandaloneLyric(prefs)) {
            runCatching { StandaloneLyric.registerInstalled(this) }
                .onFailure { Log.w(TAG, "standalone register failed: ${'$'}{it.message}") }
            runCatching { KeepAliveService.start(this) }
                .onFailure { Log.w(TAG, "standalone lyric service start failed: ${'$'}{it.message}") }
        }

        if (ModulePrefs.isEnabled(prefs) && ModulePrefs.isLinkEnabled(prefs)) {
            runCatching { LinkHub.warmUp(this) }
                .onFailure { Log.w(TAG, "island warm-up failed: ${it.message}") }

            if (ModulePrefs.isKeepAlive(prefs)) {
                runCatching { KeepAliveService.start(this) }
                    .onFailure { Log.w(TAG, "keep-alive start failed: ${it.message}") }
            }
        }
    }

    private companion object {
        const val TAG = "Mitian"
    }
}
