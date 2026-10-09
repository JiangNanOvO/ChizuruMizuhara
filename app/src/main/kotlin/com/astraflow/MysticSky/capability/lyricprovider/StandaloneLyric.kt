package com.astraflow.MysticSky.capability.lyricprovider

import android.content.Context
import android.util.Log
import com.astraflow.MysticSky.capability.root.RootShell
import com.astraflow.MysticSky.lyric.Constants
import com.astraflow.MysticSky.lyric.LyricParsers
import com.astraflow.MysticSky.lyric.LocalLyric
import com.astraflow.MysticSky.lyric.LocalRecipe
import com.astraflow.MysticSky.lyric.LocalSource
import com.astraflow.MysticSky.lyric.BaseDir
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.provider.LyriconFactory
import io.github.proify.lyricon.provider.LyriconProvider
import io.github.proify.lyricon.provider.ProviderLogo

object StandaloneLyric {

    private const val TAG = "MysticSky-Standalone"

    private fun log(context: Context, text: String) {
        android.util.Log.i(TAG, text)
        runCatching {
            val file = java.io.File(context.filesDir, "standalone.log")
            if (file.length() > 256 * 1024) file.delete()
            val stamp = java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
            file.appendText("$stamp  $text\n")
        }
    }

    private val providers = HashMap<String, LyriconProvider>()
    private var lastSignature = ""
    private var lastPushAt = 0L
    private var lastReport = ""
    @Volatile private var lastActiveAt = 0L
    @Volatile private var lastIdleLog = 0L
    @Volatile private var lastSession: MediaSessionProbe.NowPlaying? = null

    private const val HEARTBEAT_MS = 3_000L
    private const val PROBE_ACTIVE_MS = 4_000L
    private const val PROBE_IDLE_MS = 15_000L

    /**
     * App 启动时就把「已安装的播放器」全部注册上。
     *
     * 星流的「已安装的歌词提供者」读的是 LyricON 中央服务里**当前已注册**的提供者，
     * 只靠"有播放时才注册"是看不到的 —— 官方那个 cmprovider 就是启动即注册。
     */
    fun registerInstalled(context: Context) {
        val pm = context.packageManager
        var count = 0
        log(context, "启动注册开始（已装播放器扫描）")
        for (pkg in Constants.LOCAL_PLAYER_PACKAGES) {
            val installed = runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
            if (!installed) continue
            log(context, "发现已安装播放器：$pkg")
            if (providers.containsKey(pkg)) continue
            val provider = register(context, pkg)
            if (provider != null) {
                providers[pkg] = provider
                count++
                log(context, "注册成功：$pkg")
            } else {
                log(context, "注册失败：$pkg（接入库没连上中央服务？）")
            }
        }
        log(context, "启动注册完成，成功 $count 个")
    }

    fun tick(context: Context, enabled: Boolean) {
        if (!enabled) return
        if (providers.isEmpty()) {
            registerInstalled(context)
            return
        }

        val now = if (shouldProbe()) {
            runCatching { MediaSessionProbe.probe() }.getOrNull()?.also { lastSession = it }
        } else {
            lastSession
        }

        if (now == null) {
            if (System.currentTimeMillis() - lastIdleLog > 60_000L) {
                lastIdleLog = System.currentTimeMillis()
                log(context, "心跳：空闲（已注册 ${providers.size} 个播放器）")
            }
            heartbeatIdle()
            return
        }
        if (now.pkg == context.packageName) return

        val recipe = Constants.recipeOf(now.pkg)
        val supported = recipe != null || Constants.LOCAL_PLAYER_PACKAGES.contains(now.pkg)
        if (!supported) {
            heartbeatIdle()
            return
        }

        val provider = providers[now.pkg] ?: register(context, now.pkg)?.also {
            providers[now.pkg] = it
            log(context, "播放中注册：${now.pkg}")
        } ?: return

        val signature = now.signature()
        if (signature != lastSignature) {
            lastSignature = signature
            val lyric = findLyric(recipe, now)
            if (lyric != null && lyric.lines.isNotEmpty()) {
                val published = publish(provider, now, lyric)
                report("${now.title} · ${lyric.lines.size} 行 · $published")
            } else {
                report("${now.title} · 缓存里没找到歌词")
            }
        }

        runCatching { provider.player.setPlaybackState(now.playing) }
        runCatching { provider.player.setPosition(now.positionMs) }
        lastActiveAt = System.currentTimeMillis()
        lastPushAt = System.currentTimeMillis()
    }

    private fun shouldProbe(): Boolean {
        val gap = if (lastActive()) PROBE_ACTIVE_MS else PROBE_IDLE_MS
        return System.currentTimeMillis() - lastPushAt >= gap
    }

    private fun heartbeatIdle() {
        if (providers.isEmpty()) return
        if (System.currentTimeMillis() - lastPushAt < HEARTBEAT_MS) return
        lastPushAt = System.currentTimeMillis()
        for (provider in providers.values) {
            runCatching { provider.player.setPlaybackState(false) }
            runCatching { provider.player.setPosition(0L) }
        }
    }

    fun lastActive(): Boolean = System.currentTimeMillis() - lastActiveAt < 15_000L

    fun report(): String = lastReport

    private fun report(text: String) {
        if (text != lastReport) {
            lastReport = text
            Log.i(TAG, text)
        }
    }

    private fun register(context: Context, playerPackage: String): LyriconProvider? = runCatching {
        LyriconFactory.createProvider(
            context = context.applicationContext,
            providerPackageName = context.packageName,
            playerPackageName = playerPackage,
            logo = runCatching { ProviderLogo.fromSvg(Constants.ICON) }.getOrNull(),
            processName = "app"
        ).apply {
            player.setDisplayTranslation(false)
            player.setDisplayRoma(false)
            register()
        }
    }.onFailure { Log.w(TAG, "注册失败（$playerPackage）：${it.message}") }.getOrNull()

    private fun findLyric(recipe: LocalRecipe?, now: MediaSessionProbe.NowPlaying): LocalLyric? {
        val sources = recipe?.sources ?: emptyList()
        if (sources.isEmpty()) return null
        for (source in sources) {
            val dir = dirOf(source, now.pkg)
            val names = RootShell.listNewest(dir, limit = 6)
            if (names.isEmpty()) continue
            for (name in names) {
                val bytes = RootShell.readBytes("$dir/$name") ?: continue
                val parsed = runCatching { LyricParsers.parseAnyBytes(bytes) }.getOrNull() ?: continue
                if (parsed.lines.isEmpty()) continue

                if (matches(parsed, now.title)) return parsed
                if (recipe?.packageName == now.pkg && names.size == 1) return parsed
            }

            for (name in names) {
                val bytes = RootShell.readBytes("$dir/$name") ?: continue
                val parsed = runCatching { LyricParsers.parseAnyBytes(bytes) }.getOrNull() ?: continue
                if (parsed.lines.isNotEmpty()) return parsed
            }
        }
        return null
    }

    private fun matches(lyric: LocalLyric, title: String): Boolean {
        val needle = title.trim()
        if (needle.isEmpty()) return false
        val candidates = listOfNotNull(lyric.title, lyric.firstLine)
        return candidates.any { it.contains(needle) || needle.contains(it.trim()) }
    }

    private fun dirOf(source: LocalSource, pkg: String): String {
        val rel = source.subPath
        return when (source.base) {
            BaseDir.EXTERNAL_FILES -> "/sdcard/Android/data/$pkg/files/$rel"
            BaseDir.EXTERNAL_CACHE -> "/sdcard/Android/data/$pkg/cache/$rel"
            BaseDir.FILES -> "/data/data/$pkg/files/$rel"
            BaseDir.CACHE -> "/data/data/$pkg/cache/$rel"
        }
    }

    private fun publish(
        provider: LyriconProvider,
        now: MediaSessionProbe.NowPlaying,
        lyric: LocalLyric
    ): String {
        val lines = lyric.lines
        for (line in lines) line.translation = null      
        val lastEnd = lines.lastOrNull()?.end ?: 0L
        val song = Song().apply {
            id = lyric.id ?: now.title
            name = now.title
            artist = now.artist
            duration = lastEnd
            lyrics = lines
        }
        val ok = runCatching { provider.player.setSong(song) }.getOrDefault(false)
        return if (ok) "已推送" else "推送失败"
    }
}
