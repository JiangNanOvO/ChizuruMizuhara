package com.astraflow.Chizuru.app

import android.app.Application
import android.util.Log
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.keepalive.KeepAliveService
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs
import com.astraflow.Chizuru.capability.lyricprovider.StandaloneLyric
import com.astraflow.Chizuru.capability.neriplayer.NeriSettingPatcher

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
