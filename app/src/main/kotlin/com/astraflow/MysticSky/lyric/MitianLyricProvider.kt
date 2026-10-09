package com.astraflow.MysticSky.lyric

import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import com.astraflow.MysticSky.lyric.Constants
import com.astraflow.MysticSky.lyric.HookCrashLog
import com.astraflow.MysticSky.lyric.ModuleLogger
import com.astraflow.MysticSky.settings.ModulePrefs
import io.github.proify.lyricon.lyric.model.RichLyricLine
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.provider.LyriconFactory
import io.github.proify.lyricon.provider.LyriconProvider
import io.github.proify.lyricon.provider.ProviderLogo
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Executable

internal class MitianLyricProvider(
    private val module: XposedModule,
    private val logger: ModuleLogger,
    private val classLoader: ClassLoader
) {

    private val stateLock = Any()

    @Volatile
    private var application: Application? = null

    @Volatile
    private var provider: LyriconProvider? = null

    @Volatile
    private var currentSongId: String? = null

    private var lyricForCurrentSong: List<RichLyricLine>? = null

    private var currentMeta: SongMeta? = null

    private var lastPublishedSignature: String? = null

    @Volatile
    private var activeSessionHash: Int = 0

    @Volatile
    private var activeSessionRef: java.lang.ref.WeakReference<MediaSession>? = null

    @Volatile
    private var pendingSessionLyrics: List<RichLyricLine>? = null

    private var diagLastAt: Long = 0L

    @Volatile
    private var metadataDurationMs: Long = 0L

    @Volatile
    private var anchorPosition: Long = 0L

    @Volatile
    private var anchorRealtime: Long = 0L

    @Volatile
    private var isPlaying: Boolean = false

    @Volatile
    private var hasAnchor: Boolean = false

    @Volatile
    private var playbackSpeed: Float = 1.0f

    @Volatile
    private var confirmedPosition: Long = 0L

    @Volatile
    private var lastSongSwitchRealtime: Long = 0L

    private var delayedPublishQueued = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val progressTicker = object : Runnable {
        override fun run() {
            tickProgress()
            mainHandler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    fun installHooks() {
        hookApplicationLifecycle()
        hookMediaSession()
        hookMeloYouFiles()
        hookMeloYouSessionDiag()
        hookApplicationOnCreate()
        logger.info("All MeloYou hooks installed")
        mainHandler.postDelayed({ ensureStarted() }, 700L)
    }

    private fun hookApplicationLifecycle() {
        val method = Instrumentation::class.java.getDeclaredMethod(
            "callApplicationOnCreate",
            Application::class.java
        )
        installProtectiveAfterHook(method, "Instrumentation.callApplicationOnCreate") { chain, _ ->
            val hostApplication = chain.args.getOrNull(0) as? Application
            if (hostApplication != null) onApplicationCreated(hostApplication)
        }
    }

    private fun hookApplicationOnCreate() {
        runCatching {
            val onCreate = Application::class.java.getDeclaredMethod("onCreate")
            installProtectiveAfterHook(onCreate, "Application.onCreate") { chain, _ ->
                val app = chain.thisObject as? Application
                if (app != null && app.packageName == Constants.PLAYER_PACKAGE_NAME) {
                    if (application == null) application = app
                    ensureStarted(app)
                }
            }
        }.onFailure { logger.warn("Application.onCreate hook unavailable: ${it.message}") }
    }

    private fun onApplicationCreated(hostApplication: Application) {
        if (application == null) application = hostApplication
        logger.info("MeloYou Application created: ${hostApplication.packageName}")
        ensureStarted(hostApplication)
    }

    private var startAttempts = 0

    private fun ensureStarted(context: Context? = null) {
        if (provider != null) return
        val ctx = application ?: context?.applicationContext ?: context ?: currentApplication()
        if (ctx == null) {
            if (startAttempts++ < 20) {
                logger.info("ensureStarted: 暂时拿不到 Context（第 ${startAttempts} 次），稍后重试")
                mainHandler.postDelayed({ ensureStarted() }, 1500L)
            }
            return
        }
        if (application == null) {
            application = ctx.applicationContext as? Application ?: (ctx as? Application)
        }
        if (provider == null) {
            try {
                setupProvider(ctx)
            } catch (throwable: Throwable) {
                logger.error("Provider initialization failed", throwable)
                return
            }
        }
        replayFromDisk(ctx)
        logger.info(
            "MeloYou 启动补读完成：歌曲=${currentMeta?.name} 歌词=${lyricForCurrentSong?.size ?: 0} 行 provider=${provider != null}"
        )
        mainHandler.removeCallbacks(progressTicker)
        mainHandler.post(progressTicker)
    }

    private fun currentApplication(): Application? = try {
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as? Application
    } catch (throwable: Throwable) {
        null
    }

    private fun setupProvider(context: Context) {
        val created = LyriconFactory.createProvider(
            context = context,
            providerPackageName = Constants.PROVIDER_PACKAGE_NAME,
            playerPackageName = context.packageName,
            logo = ProviderLogo.fromSvg(Constants.ICON)
        ).apply {

            player.setDisplayTranslation(false)
            player.setDisplayRoma(false)
            register()
        }
        provider = created
        logger.info("Lyricon provider registered, player=${context.packageName}")
    }

    private fun extrapolatedPosition(): Long {
        if (!hasAnchor) return 0L
        if (!isPlaying) return anchorPosition
        val elapsed = SystemClock.elapsedRealtime() - anchorRealtime
        val pos = anchorPosition + (elapsed.toDouble() * playbackSpeed.toDouble()).toLong()
        val duration = metadataDurationMs.coerceAtLeast(currentMeta?.duration ?: 0L)
        return if (duration > 0) pos.coerceAtMost(duration) else pos
    }

    private fun tickProgress() {
        val p = provider ?: return
        if (!hasAnchor) return

        val current = extrapolatedPosition()
        try {
            p.player.setPlaybackState(isPlaying)
            p.player.setPosition(current)
        } catch (throwable: Throwable) {
            logger.error("tickProgress failed", throwable)
        }
    }

    private fun hookMediaSession() {
        val metadataMethod = MediaSession::class.java.getDeclaredMethod(
            "setMetadata",
            MediaMetadata::class.java
        )
        installProtectiveAfterHook(metadataMethod, "MediaSession.setMetadata") { chain, _ ->
            onMetadataChanged(
                chain.thisObject as? MediaSession,
                chain.args.getOrNull(0) as? MediaMetadata
            )
        }

        val playbackMethod = MediaSession::class.java.getDeclaredMethod(
            "setPlaybackState",
            PlaybackState::class.java
        )
        installProtectiveAfterHook(playbackMethod, "MediaSession.setPlaybackState") { chain, _ ->
            onPlaybackStateChanged(
                chain.thisObject as? MediaSession,
                chain.args.getOrNull(0) as? PlaybackState
            )
        }

        

        val extrasMethod = MediaSession::class.java.getDeclaredMethod(
            "setExtras",
            Bundle::class.java
        )
        installProtectiveAfterHook(extrasMethod, "MediaSession.setExtras") { chain, _ ->
            onSessionExtras(
                chain.thisObject as? MediaSession,
                chain.args.getOrNull(0) as? Bundle
            )
        }
    }

    @Volatile
    private var lastMetadataSignature: String? = null

    private var lastSongInfoKey: String? = null

    private var meloAttempts = 0

    private val meloRetry = Runnable { refreshFromDisk() }

    private var meloRetryActive = false

    private fun scheduleMeloRetry(restart: Boolean = false) {
        if (meloRetryActive && !restart) return
        meloAttempts = 0
        meloRetryActive = true
        mainHandler.removeCallbacks(meloRetry)
        mainHandler.post(meloRetry)
    }

    private fun lyricAllowed(): Boolean = runCatching {
        val prefs = module.getRemotePreferences(ModulePrefs.NAME)
        prefs.getBoolean(ModulePrefs.KEY_ENABLED, true) &&
            prefs.getBoolean(ModulePrefs.KEY_LYRIC, true)
    }.getOrDefault(true)

    private fun onMetadataChanged(session: MediaSession?, metadata: MediaMetadata?) {
        metadata ?: return
        if (!lyricAllowed()) return
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        if (duration > 0) metadataDurationMs = duration

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() && it != "未知歌曲" } ?: return
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() && it != "未知歌手" }

        ensureStarted()

        if (PlayerTitle.isSuspectTitle(title) && currentMeta != null) {
            logger.debug("忽略可疑标题（疑似歌词行 / 制作信息）：$title")
            return
        }
        rememberActiveSession(session)
        val signature = "$title|$artist"
        if (signature == lastMetadataSignature) return
        lastMetadataSignature = signature
        logger.info("MeloYou metadata changed: $title - $artist")

        var sameSong = false
        synchronized(stateLock) {
            sameSong = currentMeta?.name?.let { normalize(it) == normalize(title) } == true
            if (!sameSong) {
                lyricForCurrentSong = null
                currentSongId = null
                lastPublishedSignature = null
                lastSongSwitchWall = System.currentTimeMillis()
            pendingSessionLyrics = null
            }
            currentMeta = SongMeta(
                id = if (sameSong) currentMeta?.id else null,
                name = title,
                artist = artist ?: currentMeta?.artist,
                duration = if (duration > 0) duration else (currentMeta?.duration ?: 0L)
            )
        }

        
        if (!sameSong || !isLyricReady()) {
            publishPlaceholder()
        } else {
            logger.debug("同歌元数据到达：已有歌词，跳过占位快照")
        }
        scheduleMeloRetry(restart = true)
    }

    private fun refreshFromDisk() {
        if (!lyricAllowed()) return
        val context = application ?: return
        try {
            val nowPlaying = readAppFile(context, Constants.NOW_PLAYING_FILE)
            val fileSong = nowPlaying
                ?.let { runCatching { JSONObject(it).optString("name") }.getOrNull() }
                ?.takeIf { it.isNotBlank() && it != "null" }
            val want = currentMeta?.name
            val sameSong = want.isNullOrBlank() || fileSong.isNullOrBlank() ||
                normalize(fileSong).contains(normalize(want)) ||
                normalize(want).contains(normalize(fileSong))

            if (sameSong) {
                nowPlaying?.let { onNowPlayingWritten(it) }
                readAppFile(context, Constants.SONG_LYRIC_FILE)?.let { onLyricsWritten(context, it) }
            } else {
                logger.info("MeloYou 歌词文件属于其它歌曲（当前=$want, 文件=$fileSong），继续等待")
            }
        } catch (throwable: Throwable) {
            logger.error("MeloYou refreshFromDisk failed", throwable)
        }

        pendingSessionLyrics?.let { lines ->
            if (!currentMeta?.name.isNullOrBlank() && belongsToCurrentSong(lines)) {
                pendingSessionLyrics = null
                synchronized(stateLock) { lyricForCurrentSong = lines }
                publishIfReady()
            }
        }

        if (!isLyricReady() && meloAttempts < MELO_RETRY_DELAYS_MS.size) {
            val delay = MELO_RETRY_DELAYS_MS[meloAttempts]
            meloAttempts++
            mainHandler.postDelayed(meloRetry, delay)
        } else {
            meloRetryActive = false
        }
    }

    private fun isLyricReady(): Boolean =
        synchronized(stateLock) { !lyricForCurrentSong.isNullOrEmpty() }

    private fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val sb = StringBuilder(text.length)
        for (c in text.lowercase()) {
            if (c.isLetterOrDigit()) sb.append(c)
        }
        return sb.toString()
    }

    private fun onPlaybackStateChanged(session: MediaSession?, state: PlaybackState?) {
        state ?: return
        if (!lyricAllowed()) return

        ensureStarted()

        
        if (DIAG_LOG) {
            logger.info(
                "DIAG state: session=" + System.identityHashCode(session) +
                    " pos=" + state.position + " state=" + state.state +
                    " speed=" + state.playbackSpeed + " upd=" + state.lastPositionUpdateTime
            )
        }
        if (!isActiveSession(session)) {
            logger.debug("忽略非当前 MediaSession 的进度：pos=${state.position}")
            return
        }
        val position = state.position
        val newPlaying = playingOf(state.state, position)

        if (position >= 0L) {

            

            val expected = extrapolatedPosition()

            val accepted = if (hasAnchor && position < expected &&
                position >= expected - BACKWARD_TOLERANCE_MS
            ) {
                expected
            } else {
                position
            }
            anchorPosition = accepted
            anchorRealtime = SystemClock.elapsedRealtime()
            confirmedPosition = accepted
            hasAnchor = true
            isPlaying = newPlaying
            playbackSpeed = state.playbackSpeed.takeIf { it > 0f } ?: 1.0f
            logger.debug(
                "Anchor accepted: position=$position playing=$newPlaying speed=$playbackSpeed"
            )
        } else {
            isPlaying = newPlaying
        }

        tickProgress()
    }

    private fun playingOf(state: Int, position: Long): Boolean = when (state) {
        PlaybackState.STATE_PAUSED,
        PlaybackState.STATE_STOPPED,
        PlaybackState.STATE_ERROR -> if (position >= 0L) false else isPlaying

        PlaybackState.STATE_NONE -> isPlaying
        else -> true
    }

    private fun shouldAcceptAnchor(newPosition: Long, newPlaying: Boolean): Boolean {
        if (!hasAnchor) return true

        
        val now = SystemClock.elapsedRealtime()
        if (now - lastSongSwitchRealtime < ANCHOR_SWITCH_GRACE_MS &&
            newPosition > SWITCH_MAX_ACCEPT_MS
        ) return false
        return true
    }

    private fun isRecentSongSwitch(): Boolean {
        val now = SystemClock.elapsedRealtime()
        return now - lastSongSwitchRealtime < SONG_SWITCH_IGNORE_MS
    }

    private fun hookMeloYouFiles() {
        try {
            val fileUtilClass = Class.forName("com.bumptech.glide.manager.j", false, classLoader)
            val writeMethod = fileUtilClass.getDeclaredMethod(
                "B",
                Context::class.java,
                String::class.java,
                String::class.java
            )
            installProtectiveAfterHook(writeMethod, "j.B(file write)") { chain, _ ->
                val context = chain.args.getOrNull(0) as? Context
                val content = chain.args.getOrNull(1) as? String
                val filename = chain.args.getOrNull(2) as? String
                when (filename) {
                    Constants.NOW_PLAYING_FILE -> onNowPlayingWritten(content)
                    Constants.SONG_LYRIC_FILE -> onLyricsWritten(context, content)
                }
            }
        } catch (throwable: Throwable) {
            logger.error("Hook j.B unavailable", throwable)
        }
    }

    private fun onNowPlayingWritten(content: String?) {
        if (!lyricAllowed()) return
        if (content.isNullOrBlank()) return
        val obj = runCatching { JSONObject(content) }.getOrNull() ?: return
        val rid = obj.optString("rid").takeIf { it.isNotBlank() && it != "null" }
        val name = obj.optString("name").takeIf { it.isNotBlank() && it != "null" && it != "未知歌曲" }
        val artist = obj.optString("artist").takeIf { it.isNotBlank() && it != "null" && it != "未知歌手" }

        var songChanged = false
        synchronized(stateLock) {
            if (rid != null && rid != currentSongId) {
                songChanged = currentSongId != null && currentSongId != rid
                currentSongId = rid
                lyricForCurrentSong = null
            }
            val fileDuration = obj.optLong("duration", 0L)
            val effectiveDuration = when {
                metadataDurationMs > 0 -> metadataDurationMs
                fileDuration > 0 -> fileDuration
                else -> currentMeta?.duration ?: 0L
            }
            currentMeta = SongMeta(
                id = rid ?: currentSongId,
                name = name ?: currentMeta?.name,
                artist = artist ?: currentMeta?.artist,
                duration = effectiveDuration
            )
        }

        val infoKey = "$rid|$name|$artist|$songChanged"
        if (infoKey != lastSongInfoKey) {
            lastSongInfoKey = infoKey
            logger.info("MeloYou 歌曲信息：id=$rid name=$name artist=$artist 换歌=$songChanged")
        }
        if (!isLyricReady()) scheduleMeloRetry()

        if (songChanged) {
            lastSongSwitchRealtime = SystemClock.elapsedRealtime()
            lastSongSwitchWall = System.currentTimeMillis()
            pendingSessionLyrics = null
            anchorPosition = 0L
            anchorRealtime = SystemClock.elapsedRealtime()
            hasAnchor = true
            confirmedPosition = 0L
            isPlaying = true
            try {
                provider?.player?.setPosition(0L)
            } catch (throwable: Throwable) {
                logger.error("Reset position failed", throwable)
            }
            publishPlaceholder()
            logger.debug("Song changed, reset anchor position to 0")
        }

        publishIfReady()
    }

    private fun onLyricsWritten(context: Context?, content: String?) {
        if (!lyricAllowed()) return
        val text = content ?: run {
            context?.let { readAppFile(it, Constants.SONG_LYRIC_FILE) }
        } ?: return

        val lines = parseMeloYouLyrics(text) ?: return

        if (!belongsToCurrentSong(lines)) {
            logger.info("忽略不匹配的 MeloYou 歌词（当前=${currentMeta?.name}）")
            return
        }
        synchronized(stateLock) {
            lyricForCurrentSong = lines
        }
        publishIfReady()
    }

    private fun replayFromDisk(context: Context) {
        if (!lyricAllowed()) return
        val nowPlaying = readAppFile(context, Constants.NOW_PLAYING_FILE)
        val songLyric = readAppFile(context, Constants.SONG_LYRIC_FILE)
        logger.info(
            "MeloYou 启动补读：nowPlaying=" + (nowPlaying?.length ?: -1) + "B songLyric=" +
                (songLyric?.length ?: -1) + "B 已认歌曲=" + currentMeta?.name
        )
        if (!nowPlaying.isNullOrBlank()) onNowPlayingWritten(nowPlaying)
        if (!songLyric.isNullOrBlank()) onLyricsWritten(context, songLyric)
    }

    @Volatile
    private var lastSongSwitchWall: Long = 0L

    private var lastPlaceholderSignature: String? = null

    private fun publishPlaceholder() {
        val p = provider ?: return
        val meta = synchronized(stateLock) { currentMeta } ?: return
        val id = meta.id ?: return
        val signature = "$id|${meta.name}|${meta.artist}"
        if (signature == lastPlaceholderSignature) return
        lastPlaceholderSignature = signature
        val song = Song().apply {
            this.id = "meloyou:$id"
            this.name = meta.name
            this.artist = meta.artist
            this.duration = meta.duration
            this.lyrics = emptyList()
        }
        val ok = runCatching { p.player.setSong(song) }.getOrDefault(false)

        
        lastPublishedSignature = null
        logger.info("切歌占位：$id ${meta.name}, ok=$ok")
    }

    private fun belongsToCurrentSong(lines: List<RichLyricLine>): Boolean {
        val want = currentMeta?.name ?: return true
        val wantKey = coreTitle(want)
        if (wantKey.isEmpty()) return true

        val head = normalize(lines.firstOrNull()?.text)
        if (head.contains(wantKey)) {
            logger.debug("MeloYou 歌词首行匹配：<" + head + "> ~ <" + wantKey + ">")
            return true
        }

        val context = application
        val songId = currentSongId
        if (context != null) {
            val lyricFile = runCatching {
                context.getFileStreamPath(Constants.SONG_LYRIC_FILE)
            }.getOrNull()
            val stamp = if (lyricFile != null && lyricFile.isFile) {
                lyricFile.lastModified() to lyricFile.length()
            } else null

            if (stamp != null && songId != null && acceptedLyricFiles[songId] == stamp) return true

            val nowPlayingAt = runCatching {
                context.getFileStreamPath(Constants.NOW_PLAYING_FILE).lastModified()
            }.getOrDefault(0L)
            if (lyricFile != null && lyricFile.isFile &&
                lyricFile.lastModified() >= nowPlayingAt - FILE_FRESH_TOLERANCE_MS
            ) {
                if (stamp != null && songId != null) acceptedLyricFiles[songId] = stamp
                return true
            }

            logger.info(
                "MeloYou 歌词文件属于其它歌曲（当前=$want, 文件时间=${lyricFile?.lastModified()}, " +
                    "歌曲信息时间=$nowPlayingAt）"
            )
            return false
        }
        return System.currentTimeMillis() - lastSongSwitchWall <= FILE_FRESH_WINDOW_MS
    }

    private val acceptedLyricFiles = HashMap<String, Pair<Long, Long>>()

    private fun coreTitle(raw: String): String {
        var text = raw
        val cut = text.indexOfFirst {
            it == '-' || it == '–' || it == '_' || it == '《' ||
                it == '(' || it == '（' || it == '[' || it == '【'
        }
        if (cut > 1) text = text.substring(0, cut)
        return normalize(text)
    }

    private fun publishIfReady() {
        val p = provider ?: return
        val meta: SongMeta
        val lyrics: List<RichLyricLine>?
        synchronized(stateLock) {
            meta = currentMeta ?: return
            if (meta.id.isNullOrBlank()) return
            lyrics = lyricForCurrentSong
        }

        

        if (lyrics.isNullOrEmpty()) {
            scheduleDelayedPublish()
            schedulePlaceholderFallback()
            return
        }

        val signature = buildString {
            append(meta.id).append('|')
            append(meta.name).append('|')
            append(meta.artist).append('|')
            append(lyrics?.size ?: -1).append('|')
            append(lyrics?.lastOrNull()?.text)
        }
        if (signature == lastPublishedSignature) return
        lastPublishedSignature = signature

        lyrics?.forEach { it.translation = null }

        val song = Song().apply {
            this.id = meta.id!!.let { "meloyou:$it" }
            this.name = meta.name
            this.artist = meta.artist
            this.duration = meta.duration
            this.lyrics = lyrics
        }

        try {
            p.player.setSong(song)
            logger.debug(
                "Published: id=${song.id}, name=${song.name}, artist=${song.artist}, " +
                    "duration=${song.duration}, lyricLines=${lyrics?.size ?: 0}"
            )
        } catch (throwable: Throwable) {
            logger.error("setSong failed", throwable)
        }
    }

    private val placeholderFallback = Runnable {
        val meta = synchronized(stateLock) { currentMeta } ?: return@Runnable
        val id = meta.id ?: return@Runnable
        if (isLyricReady()) return@Runnable
        if (lastPublishedSignature?.startsWith("$id|") == true) return@Runnable
        if (lastPlaceholderSignature == "$id|" + meta.name + "|" + meta.artist) return@Runnable
        logger.info("歌词未到，先推歌曲信息占位：" + id + " " + meta.name)
        publishPlaceholder()
    }

    private fun schedulePlaceholderFallback() {
        mainHandler.removeCallbacks(placeholderFallback)
        mainHandler.postDelayed(placeholderFallback, PLACEHOLDER_FALLBACK_MS)
    }

    private fun scheduleDelayedPublish() {
        if (delayedPublishQueued) return
        delayedPublishQueued = true
        mainHandler.postDelayed({
            delayedPublishQueued = false
            publishIfReady()
        }, DELAYED_PUBLISH_MS)
    }

    private fun parseMeloYouLyrics(content: String): List<RichLyricLine>? {
        return try {
            val array = JSONArray(content)
            val items = ArrayList<Pair<Long, String>>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val text = obj.optString("lineLyric").trim()
                if (text.isEmpty()) continue
                if (PLACEHOLDER_PATTERNS.any { text.contains(it) }) continue
                val seconds = obj.optString("time", "0").toDoubleOrNull() ?: 0.0
                items.add((seconds * 1000).toLong() to text)
            }
            if (items.isEmpty()) return null
            items.sortBy { it.first }
            items.mapIndexed { index, (begin, text) ->
                val end = items.getOrNull(index + 1)?.first ?: (begin + 3000L)
                RichLyricLine(begin = begin, end = end, text = text)
            }
        } catch (throwable: Throwable) {
            logger.error("Parse songLyric.json failed: ${throwable.message}")
            null
        }
    }

    private fun readAppFile(context: Context, name: String): String? {
        return try {
            context.openFileInput(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (throwable: Throwable) {
            null
        }
    }

    private fun installProtectiveAfterHook(
        executable: Executable,
        description: String,
        callback: (XposedInterface.Chain, Any?) -> Unit
    ) {
        try {
            module.hook(executable)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    try {
                        callback(chain, result)
                    } catch (throwable: Throwable) {
                        logger.error("After-hook failed: $description", throwable)
                    }
                    result
                }
            logger.debug("Installed protective hook: $description")
        } catch (throwable: Throwable) {
            logger.error("Unable to install hook: $description", throwable)
        }
    }

    private data class SongMeta(
        val id: String?,
        val name: String?,
        val artist: String?,
        val duration: Long = 0L
    )

    private fun rememberActiveSession(session: MediaSession?) {
        if (session == null) return
        activeSessionRef = java.lang.ref.WeakReference(session)
        val hash = System.identityHashCode(session)
        if (hash != activeSessionHash) {
            activeSessionHash = hash
            logger.debug("当前 MediaSession 会话：$hash")
        }
    }

    private fun isActiveSession(session: MediaSession?): Boolean {
        val current = activeSessionRef?.get() ?: return true
        return session == null || session === current
    }

    private fun onSessionExtras(session: MediaSession?, extras: Bundle?) {
        extras ?: return
        if (!isActiveSession(session)) {
            logger.debug("忽略非当前 MediaSession 的歌词 extras")
            return
        }
        val lines = parseSessionLyrics(extras) ?: return
        if (DIAG_LOG) {
            logger.info(
                "DIAG extras: 行数=" + lines.size + " 首行=" + lines.firstOrNull()?.text +
                    " 末行=" + lines.lastOrNull()?.text +
                    " 当前句=" + extras.getString("current_lyric") +
                    " 当前句时间=" + extras.getLong("current_lyric_time") +
                    " 当前句下标=" + extras.getInt("current_lyric_index", -1)
            )
        }
        val current = synchronized(stateLock) { currentMeta }
        if (current?.name.isNullOrBlank()) {
            pendingSessionLyrics = lines
            logger.info("等待歌曲信息，先缓存会话歌词（首行=${lines.firstOrNull()?.text}）")
            return
        }
        if (!belongsToCurrentSong(lines)) {
            logger.info(
                "忽略不匹配的 MeloYou 会话歌词（当前=${current?.name}，会话首行=${lines.firstOrNull()?.text}）"
            )
            return
        }
        val changed = synchronized(stateLock) {
            val old = lyricForCurrentSong
            if (old != null && old.size == lines.size &&
                old.lastOrNull()?.text == lines.lastOrNull()?.text
            ) {
                false
            } else {
                lyricForCurrentSong = lines
                true
            }
        }
        if (changed) {
            pendingSessionLyrics = null
            logger.info("MeloYou 会话歌词：${lines.size} 行（首行=${lines.firstOrNull()?.text}）")
        }
        publishIfReady()
    }

    private fun parseSessionLyrics(extras: Bundle): List<RichLyricLine>? {
        val times = extras.getLongArray("lyric_timestamps") ?: return null
        val texts = extras.getStringArray("lyric_texts") ?: return null
        val count = minOf(times.size, texts.size)
        if (count <= 0) return null
        val items = ArrayList<Pair<Long, String>>(count)
        for (i in 0 until count) {
            val text = texts[i]?.trim().orEmpty()
            if (text.isEmpty()) continue
            if (PLACEHOLDER_PATTERNS.any { text.contains(it) }) continue
            items.add(times[i] to text)
        }
        if (items.isEmpty()) return null
        items.sortBy { it.first }
        return items.mapIndexed { index, (begin, text) ->
            val end = items.getOrNull(index + 1)?.first ?: (begin + 3000L)
            RichLyricLine(begin = begin, end = end, text = text)
        }
    }

    private fun hookMeloYouSessionDiag() {
        if (!DIAG_LOG) return
        runCatching {
            val method = Class.forName("H1.a", false, classLoader).getDeclaredMethod("a")
            installProtectiveAfterHook(method, "DIAG H1.a.a") { _, result ->
                val value = (result as? Long) ?: -1L
                val now = SystemClock.elapsedRealtime()
                if (now - diagLastAt > 1500L) {
                    diagLastAt = now
                    logger.info("DIAG 播放器进度 a()=$value")
                }
            }
        }.onFailure { logger.warn("DIAG 无法挂钩播放器进度：${it.message}") }
    }
    companion object {

        private const val DIAG_LOG = false

        private const val TICK_INTERVAL_MS = 48L

        private const val FILE_FRESH_WINDOW_MS = 6000L

        private const val BACKWARD_TOLERANCE_MS = 2000L

        private const val ZERO_RESET_IGNORE_MS = 3000L

        private const val SONG_SWITCH_IGNORE_MS = 2500L

        private const val ANCHOR_SWITCH_GRACE_MS = 400L

        private const val SWITCH_MAX_ACCEPT_MS = 8000L

        private val MELO_RETRY_DELAYS_MS = longArrayOf(
            800L, 1200L, 1800L, 2500L, 3500L, 5000L, 7000L,
            9000L, 12000L, 16000L, 20000L, 25000L, 30000L, 30000L
        )

        private const val FILE_FRESH_TOLERANCE_MS = 1500L

        private const val DELAYED_PUBLISH_MS = 1200L

        private const val PLACEHOLDER_FALLBACK_MS = 1500L

        private val PLACEHOLDER_PATTERNS = listOf(
            "歌曲暂无歌词",
            "暂无歌词",
            "请欣赏音乐",
            "纯音乐",
            "该歌曲为纯音乐"
        )
    }
}
