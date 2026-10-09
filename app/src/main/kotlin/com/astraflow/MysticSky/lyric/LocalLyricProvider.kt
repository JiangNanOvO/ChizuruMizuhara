package com.astraflow.MysticSky.lyric

import android.app.Application
import android.app.Instrumentation
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import com.astraflow.MysticSky.settings.ModulePrefs
import io.github.proify.lyricon.lyric.model.RichLyricLine
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.provider.LyriconFactory
import io.github.proify.lyricon.provider.LyriconProvider
import io.github.proify.lyricon.provider.ProviderLogo
import java.lang.reflect.Executable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

internal class LocalLyricProvider(
    private val module: XposedModule,
    private val logger: ModuleLogger,
    private val classLoader: ClassLoader,
    private val hostPackage: String,
    private val processName: String,
    private val recipe: LocalRecipe?
) {

    private val stateLock = Any()

    @Volatile
    private var provider: LyriconProvider? = null

    @Volatile
    private var application: Application? = null

    @Volatile
    private var currentSignature: String? = null

    @Volatile
    private var currentMediaId: String? = null
    private var lastLoggedState = -1
    private var pendingSwitch: PendingSwitch? = null
    private var pendingSwitchAt = 0L
    private var pendingSwitchLyric: LocalLyric? = null

    private var hasCommittedSong = false
    private var sawPositionSinceSwitch = false
    private var lastReport = -1L
    private var lastReportAt = 0L

    @Volatile
    private var currentTitle: String? = null

    @Volatile
    private var currentArtist: String? = null

    @Volatile
    private var knownLyricLines: Set<String> = emptySet()

    private val songFirstArtistLast: Boolean = hostPackage == Constants.QQ_PACKAGE

    private val trustArtistStructure: Boolean =
        hostPackage in Constants.ARTIST_STRUCTURE_PACKAGES

    @Volatile
    private var metadataDurationMs: Long = 0L

    @Volatile
    private var lyricFound = false

    @Volatile
    private var fallbackSent = false

    private val attemptIndex = AtomicInteger(0)

    @Volatile
    private var pendingLyric: Triple<LocalLyric, String, Long>? = null

    private var preloadedLyric: Triple<LocalLyric, String, Long>? = null

    private var everPublished = false

    @Volatile
    private var songSwitchAt: Long = 0L

    @Volatile
    private var songSwitchWall: Long = 0L

    @Volatile
    private var lastSongSwitchAt: Long = 0L

    @Volatile
    private var anchorPosition: Long = 0L

    @Volatile
    private var anchorRealtime: Long = 0L

    @Volatile
    private var anchorPlaying: Boolean = false

    @Volatile
    private var playbackSpeed: Float = 1.0f

    @Volatile
    private var hasAnchor: Boolean = false

    private var lastPushedPlaying: Boolean? = null

    private val progressTicker = object : Runnable {
        override fun run() {
            tickProgress()
            mainHandler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    private val lookupExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mitian-lyric-lookup").apply { isDaemon = true }
    }

    private val retryTask = Runnable { tryLookup() }

    fun installHooks() {
        installRnBridge()
        hookApplicationLifecycle()
        hookMediaSession()
        installSniffer()
        installBluetoothBoost()
        logger.info(
            "Local lyric hooks installed for $hostPackage " +
                "(${recipe?.displayName ?: "通用嗅探"}) in $processName"
        )
    }

    private fun installBluetoothBoost() {
        if (hostPackage !in Constants.BLUETOOTH_BOOST_PACKAGES) return
        val targets = listOf(
            "android.media.AudioManager" to "isBluetoothA2dpOn",
            "android.bluetooth.BluetoothAdapter" to "isEnabled"
        )
        for ((className, methodName) in targets) {
            try {
                val clazz = Class.forName(className, false, classLoader)
                val method = clazz.getDeclaredMethod(methodName)
                module.hook(method).intercept { true }
                logger.info("蓝牙输出伪装已挂载：$className#$methodName")
            } catch (throwable: Throwable) {
                logger.warn("蓝牙输出伪装失败：$className#$methodName（${throwable.message}）")
            }
        }
    }

    private fun ensureProvider(): LyriconProvider? {
        provider?.let { return it }
        synchronized(stateLock) {
            provider?.let { return it }
            val context = application ?: currentApplication()
            if (context == null) {
                logger.warn("Provider not created: no context in $processName")
                return null
            }
            return try {
                val logo = runCatching { ProviderLogo.fromSvg(Constants.ICON) }.getOrNull()
                val created = LyriconFactory.createProvider(
                    context = context,
                    providerPackageName = Constants.PROVIDER_PACKAGE_NAME,
                    playerPackageName = hostPackage,
                    logo = logo,
                    processName = processName
                ).apply {

                    
                    player.setDisplayTranslation(false)
                    player.setDisplayRoma(false)
                    register()
                }
                provider = created
                logger.info("Provider registered: player=$hostPackage, process=$processName")
                created
            } catch (throwable: Throwable) {
                logger.error("Provider registration failed in $processName", throwable)
                null
            }
        }
    }

    private fun currentApplication(): Application? = try {
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as? Application
    } catch (throwable: Throwable) {
        null
    }

    private fun hookApplicationLifecycle() {
        val method = Instrumentation::class.java.getDeclaredMethod(
            "callApplicationOnCreate",
            Application::class.java
        )
        installProtectiveAfterHook(method, "Instrumentation.callApplicationOnCreate") { chain, _ ->
            val hostApplication = chain.args.getOrNull(0) as? Application
            if (hostApplication != null) application = hostApplication
        }
    }

    private fun installSniffer() {
        runCatching {
            HttpLyricSniffer(module, logger, classLoader, isActive = { lyricAllowed() }) { lyric, source ->
                onNetworkLyric(lyric, source)
            }.install()
        }.onFailure { logger.error("网络歌词嗅探挂载失败", it) }
    }

    private fun installRnBridge() {
        runCatching {
            RnLyricBridge(module, logger, classLoader, isActive = { lyricAllowed() }) { lyric, source -> onNetworkLyric(lyric, source) }.install()
        }.onFailure { logger.error("RN 歌词嗅探挂载失败", it) }
    }

    private fun lyricAllowed(): Boolean = runCatching {
        val prefs = module.getRemotePreferences(ModulePrefs.NAME)
        prefs.getBoolean(ModulePrefs.KEY_ENABLED, true) &&
            prefs.getBoolean(ModulePrefs.KEY_LYRIC, true)
    }.getOrDefault(true)

    private fun onNetworkLyric(lyric: LocalLyric, source: String, trustedBySource: Boolean = false) {
        if (!lyricAllowed()) return
        val sniffedAt = SystemClock.elapsedRealtime()
        pendingLyric = Triple(lyric, source, sniffedAt)
        val signature = currentSignature ?: return
        val trusted = trustedBySource || hasTrustworthyIdentity(lyric)

        if (lyricFound && !trusted) return
        val parts = signature.split('|')
        val title = parts.getOrNull(0).orEmpty()
        if (title.isBlank()) return
        val artist = parts.getOrNull(1)?.takeIf { it.isNotBlank() && it != "null" }
        val duration = metadataDurationMs
        val mediaId = parts.getOrNull(2)?.takeIf { it.isNotBlank() && it != "null" }
            ?: currentMediaId
        logger.info("[$source] 嗅到歌词：${lyric.lines.size} 行，标题=<${lyric.title ?: lyric.firstLine}>")
        val mustVerify = !trustedBySource ||
            hostPackage == "cn.kuwo.player" || hostPackage == "com.netease.cloudmusic"
        if (mustVerify && !belongsToCurrentSong(lyric, sniffedAt, title, artist, duration, mediaId)) {
            logger.info("[$source] 丢弃非当前歌曲的歌词（当前=$title - $artist）")
            return
        }
        mainHandler.post {
            if (currentSignature != signature) return@post

            

            if (lyricFound) return@post
            everPublished = true
            publish(title, artist, duration, mediaId, lyric.lines, source, lyric.title)
        }
    }

    private fun hasTrustworthyIdentity(lyric: LocalLyric): Boolean {
        val want = LocalLyricFinder.normalize(currentTitle)
        if (want.isEmpty()) return false
        val candidate = LocalLyricFinder.normalize(lyric.title)
        if (candidate.isEmpty()) return false
        return candidate == want || candidate.contains(want) || want.contains(candidate)
    }

    private fun belongsToCurrentSong(
        lyric: LocalLyric,
        sniffedAt: Long,
        title: String,
        artist: String?,
        durationMs: Long,
        mediaId: String? = null
    ): Boolean {
        val sniffed = LocalLyricFinder.normalize(lyric.title)
        val sniffedArtist = LocalLyricFinder.normalize(lyric.artist)

        val wantId = LocalLyricFinder.normalize(mediaId)
        val sniffedId = LocalLyricFinder.normalize(lyric.id)
        if (wantId.isNotEmpty() && sniffedId.isNotEmpty() && wantId != sniffedId) return false
        if (sniffed.isNotEmpty()) {
            if (LocalLyricFinder.titlesMatch(lyric.title, title)) return true
            val wantArtist = LocalLyricFinder.normalize(artist)
            if (wantArtist.isNotEmpty() && sniffedArtist.isNotEmpty() &&
                (sniffedArtist.contains(wantArtist) || wantArtist.contains(sniffedArtist))
            ) return true
            return false
        }

        
        val lastLine = lyric.lines.lastOrNull()?.begin ?: 0L
        if (durationMs > 0L && lastLine > 0L) {
            val diff = if (lastLine > durationMs) lastLine - durationMs else durationMs - lastLine
            if (diff <= 8_000L) return true

            
            val guess = lyric.firstLine?.trim().orEmpty()
            if (guess.isNotEmpty() && guess.length <= 24 && !guess.startsWith("[")) {
                val g = LocalLyricFinder.normalize(guess)
                val w = LocalLyricFinder.normalize(title)
                if (g.isNotEmpty() && w.isNotEmpty() && !(g.contains(w) || w.contains(g))) {
                    preloadedLyric = Triple(lyric, "预载", sniffedAt)
                    logger.info("暂存下一首歌词：首行=<$guess> 当前=$title")
                    return false
                }
            }
        }
        return sniffedAt >= songSwitchAt - SWITCH_GRACE_MS
    }

    private fun hookMediaSession() {
        val metadataMethod = MediaSession::class.java.getDeclaredMethod(
            "setMetadata",
            MediaMetadata::class.java
        )
        installProtectiveAfterHook(metadataMethod, "MediaSession.setMetadata") { chain, _ ->
            val md = chain.args.getOrNull(0) as? MediaMetadata
            logger.debug("setMetadata size=" + md?.size() + ", title=" + md?.getString(MediaMetadata.METADATA_KEY_TITLE))
            onMetadataChanged(md)
        }

        val playbackMethod = MediaSession::class.java.getDeclaredMethod(
            "setPlaybackState",
            PlaybackState::class.java
        )
        installProtectiveAfterHook(playbackMethod, "MediaSession.setPlaybackState") { chain, _ ->
            val st = chain.args.getOrNull(0) as? PlaybackState
            logger.debug("setPlaybackState state=" + st?.state + ", pos=" + st?.position)
            onPlaybackStateChanged(st)
        }
    }

    private fun dumpMetadataExtras(metadata: MediaMetadata) {
        val extras = runCatching {
            MediaMetadata::class.java.getMethod("getExtras").invoke(metadata) as? Bundle
        }.getOrNull() ?: return
        if (extras.size() == 0) return
        logger.debug("元数据附加字段(" + extras.size() + "): " + extras.keySet().joinToString(","))
        for (key in extras.keySet()) {
            val lower = key.lowercase()
            if (!lower.contains("lyric") && !lower.contains("krc") && !lower.contains("lrc")) continue
            val value = extras.get(key)
            val text = when (value) {
                is String -> value
                is ByteArray -> runCatching { String(value, Charsets.UTF_8) }.getOrNull()
                else -> null
            } ?: continue
            if (text.length < 32) continue
            logger.info("元数据里带歌词字段：$key（${text.length} 字符）")
            submitMetadataLyric(text, key)
        }
    }

    private fun submitMetadataLyric(text: String, key: String) {
        mainHandler.post {
            val lyric = runCatching { LyricParsers.parseAnyPayload(text) }.getOrNull()
                ?: runCatching { LyricParsers.parseAnyBytes(text.toByteArray()) }.getOrNull()
                ?: return@post
            if (!LyricParsers.isUsable(lyric.lines)) return@post
            onNetworkLyric(lyric, "元数据·" + key, trustedBySource = true)
        }
    }

    private fun onMetadataChanged(metadata: MediaMetadata?) {
        metadata ?: return
        if (!lyricAllowed()) return
        dumpMetadataExtras(metadata)
        val rawTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() && it != "未知歌曲" }
        val rawArtist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() && it != "未知歌手" }
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val mediaId = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?.takeIf { it.isNotBlank() }

        if (duration > 0) metadataDurationMs = duration

        

        val resolved = PlayerTitle.resolve(
            title = rawTitle,
            artist = rawArtist,
            currentName = currentTitle,
            currentArtist = currentArtist,
            lyricLines = knownLyricLines,
            songFirstArtistLast = songFirstArtistLast,
            trustArtistStructure = trustArtistStructure
        )
        val title = resolved.name
        if (title == null) {
            logger.info(
                "元数据无法解析，忽略：title=$rawTitle artist=$rawArtist " +
                    "(suspect=${resolved.suspectTitle})"
            )
            return
        }
        val artist = resolved.artist

        

        

        

        
        val idChanged = mediaId != null && currentMediaId != null && mediaId != currentMediaId
        val sameSong = resolved.sameSong
        if (idChanged) {
            logger.info("mediaId 变化（$currentMediaId → $mediaId），仍按同一首处理：$title - $artist")
        }
        logger.info("元数据：title=$rawTitle artist=$rawArtist id=$mediaId 时长=$duration 解析后=$title / $artist")

        if (sameSong) {
            if (resolved.suspectTitle) {
                logger.info(
                    "同一首歌的元数据（title=$rawTitle artist=$rawArtist → $title / $artist），跳过重查"
                )
            }
            if (mediaId != null) currentMediaId = mediaId
            synchronized(stateLock) {
                if (resolved.suspectTitle && !artist.isNullOrBlank()) {

                    currentArtist = artist
                }
                    currentSignature = songSignature(title, artist, mediaId ?: currentMediaId)
            }
            return
        }

        

        currentMediaId = mediaId
        synchronized(stateLock) {
            currentSignature = songSignature(title, artist, mediaId)
            currentTitle = title
            currentArtist = artist
        }
        songSwitchAt = SystemClock.elapsedRealtime()
        songSwitchWall = System.currentTimeMillis()
        lastSongSwitchAt = songSwitchAt
        preloadedLyric?.let { p ->
            val stale = SystemClock.elapsedRealtime() - p.third > 120_000L
            val last = p.first.lines.lastOrNull()?.begin ?: 0L
            val mate = duration > 0L && last > 0L && kotlin.math.abs(last - duration) <= 8_000L
            if (!stale && mate) {
                logger.info("使用暂存的歌词：<$title> ${p.first.lines.size} 行")
                mainHandler.post {
                    if (currentTitle != title) return@post
                    everPublished = true
                    publish(title, artist, duration, mediaId, p.first.lines, p.second, p.first.title)
                }
            }
        }
        preloadedLyric = null
        if (!hasCommittedSong) {

            logger.info("启动后首曲立即提交：$title - $artist")
            pendingSwitchAt = songSwitchAt
            commitSwitch(PendingSwitch(title, artist, duration, mediaId))
            startProgressTicker()
            return
        }
        pendingLyric = null
        pendingSwitchLyric = null
        pendingSwitch = PendingSwitch(title, artist, duration, mediaId)
        pendingSwitchAt = songSwitchAt
        sawPositionSinceSwitch = false
        mainHandler.removeCallbacks(switchFallback)
        mainHandler.postDelayed(switchFallback, SWITCH_HOLD_FALLBACK_MS)
        logger.info("切歌待定：" + title + " - " + artist + "（等位置回调确认音频已切）")
        startProgressTicker()
        return

        knownLyricLines = emptySet()
        lyricFound = false
        fallbackSent = false
        attemptIndex.set(0)

        anchorPosition = 0L
        anchorRealtime = SystemClock.elapsedRealtime()
        hasAnchor = true
        lastPushedPlaying = null

        val registered = ensureProvider()
        if (registered == null) {

            

            logger.warn("No provider yet for $title - $artist in $processName（稍后补推）")
        } else {
            runCatching { registered.player.setPosition(0L) }
        }

        logger.info("Metadata changed: $title - $artist ($duration) id=$mediaId in $processName")

        
        if (registered != null) publishPlaceholder(title, artist, duration, mediaId)
        publishPending(title, artist, duration, mediaId)
        startProgressTicker()
        mainHandler.removeCallbacks(retryTask)
        mainHandler.post(retryTask)
    }

    private data class PendingSwitch(
        val title: String,
        val artist: String?,
        val duration: Long,
        val mediaId: String?
    )

    private fun commitSwitch(fallback: PendingSwitch) {
        hasCommittedSong = true
        val held = pendingSwitchLyric
        pendingSwitch = null
        pendingSwitchAt = 0L
        pendingSwitchLyric = null
        knownLyricLines = emptySet()
        lyricFound = false
        fallbackSent = false
        attemptIndex.set(0)
        songSwitchAt = SystemClock.elapsedRealtime()
        songSwitchWall = System.currentTimeMillis()
        lastSongSwitchAt = songSwitchAt
        val now = SystemClock.elapsedRealtime()

        
        val start = 0L
        anchorPosition = start
        anchorRealtime = now
        hasAnchor = true
        lastPushedPlaying = null
        val registered = ensureProvider()
        if (registered != null) runCatching { registered.player.setPosition(start) }
        logger.info("Metadata changed: " + fallback.title + " - " + fallback.artist +
            " (" + fallback.duration + ") id=" + fallback.mediaId + " 起锚=" + start)
        if (registered != null) {
            publishPlaceholder(fallback.title, fallback.artist, fallback.duration, fallback.mediaId)
        }
        if (held != null) {
            logger.info("使用待定期间暂存的歌词：" + held.lines.size + " 行")
            publish(fallback.title, fallback.artist, fallback.duration, fallback.mediaId, held.lines, "网络", held.title)
        }
        publishPending(fallback.title, fallback.artist, fallback.duration, fallback.mediaId)
        startProgressTicker()
        mainHandler.removeCallbacks(retryTask)
        mainHandler.post(retryTask)
    }

    private val switchFallback = Runnable {
        val waiting = pendingSwitch ?: return@Runnable
        if (sawPositionSinceSwitch) return@Runnable
        logger.info("切歌后没有位置回调，直接按切歌收尾：" + waiting.title)
        commitSwitch(waiting)
    }

    private fun onPlaybackStateChanged(state: PlaybackState?) {
        state ?: return
        if (!lyricAllowed()) return
        val registered = ensureProvider() ?: return

        val rawPosition = state.position
        val playing = playingOf(state.state, rawPosition)
        val speed = state.playbackSpeed.takeIf { it > 0f } ?: 1.0f

        

        val nowMs = SystemClock.elapsedRealtime()
        val reportAge = if (state.lastPositionUpdateTime in 1..nowMs) {
            nowMs - state.lastPositionUpdateTime
        } else -1L
        val position = if (rawPosition >= 0L && playing && reportAge in 0..8_000L) {
            rawPosition + (reportAge.toDouble() * speed.toDouble()).toLong()
        } else rawPosition
        val waiting = pendingSwitch
        if (waiting != null && position >= 0L) {
            sawPositionSinceSwitch = true
            val expected = extrapolatedPosition()
            val delta = if (position > expected) position - expected else expected - position
            val waited = SystemClock.elapsedRealtime() - pendingSwitchAt
            if (delta > SWITCH_HOLD_TOLERANCE_MS || waited > SWITCH_HOLD_TIMEOUT_MS) {
                logger.info(
                    "音频已切过来（位置=" + position + " 上一首推算=" + expected +
                        " 等待=" + waited + "ms），补做切歌"
                )
                mainHandler.removeCallbacks(switchFallback)
                commitSwitch(waiting)
            }
        }

        val predicted = extrapolatedPosition()
        val delta = position - predicted
        if (position >= 0L && (delta > 3000 || delta < -3000 || state.state != lastLoggedState)) {
            lastLoggedState = state.state
            logger.info(
                "位置回调：state=" + state.state + " pos=" + position + " 推算=" + predicted +
                    " 差=" + delta + " 接受=" + shouldAcceptAnchor(position, playing)
            )
        }
        if (position >= 0L && shouldAcceptAnchor(position, playing)) {
            val expected = extrapolatedPosition()

            val accepted = if (position < expected &&
                position >= expected - BACKWARD_TOLERANCE_MS
            ) expected else position
            anchorPosition = accepted
            anchorRealtime = SystemClock.elapsedRealtime()
            hasAnchor = true
        }
        anchorPlaying = playing
        playbackSpeed = speed

        runCatching { registered.player.setPlaybackState(playing) }
        runCatching { registered.player.setPosition(extrapolatedPosition()) }
        lastPushedPlaying = playing
        startProgressTicker()
    }

    private fun canAdoptPosition(position: Long, playing: Boolean, state: Int, now: Long): Boolean {
        val expected = extrapolatedPosition()
        val delta = if (position > expected) position - expected else expected - position
        if (delta <= SYNC_TOLERANCE_MS) return true
        if (!playing) return true
        when (state) {
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> return true
        }

        
        if (hostPackage == "com.kugou.android.lite") return true
        if (lastReportAt <= 0L) return false
        val gap = now - lastReportAt
        if (gap !in 1..JUMP_CONFIRM_WINDOW_MS) return false
        val move = position - lastReport
        val moveAbs = if (move > 0) move else -move
        if (moveAbs < JUMP_MIN_MOVE_MS) return false
        val step = (gap.toDouble() * playbackSpeed.toDouble()).toLong()
        val drift = if (move > step) move - step else step - move
        if (drift > JUMP_CONFIRM_SLACK_MS) {
            logger.info("忽略可疑位置：pos=" + position + " 推算=" + expected +
                " 差=" + (position - expected))
            return false
        }
        return true
    }

    private fun playingOf(state: Int, position: Long): Boolean = when (state) {

        

        PlaybackState.STATE_PAUSED,
        PlaybackState.STATE_STOPPED,
        PlaybackState.STATE_ERROR -> if (position >= 0L) false else anchorPlaying

        PlaybackState.STATE_NONE -> anchorPlaying
        else -> true
    }

    private fun shouldAcceptAnchor(newPosition: Long, newPlaying: Boolean): Boolean {
        if (!hasAnchor) return true

        

        

        val now = SystemClock.elapsedRealtime()
        val sinceSwitch = now - lastSongSwitchAt

        val noGrace = hostPackage == "com.kugou.android.lite" ||
            hostPackage == "com.kugou.android" || hostPackage == "com.music"
        if (!noGrace && sinceSwitch < ANCHOR_SWITCH_GRACE_MS &&
            newPosition > SWITCH_MAX_ACCEPT_MS
        ) return false
        return true
    }

    private fun startProgressTicker() {
        mainHandler.removeCallbacks(progressTicker)
        mainHandler.post(progressTicker)
    }

    private fun extrapolatedPosition(): Long {
        if (!hasAnchor) return 0L
        if (!anchorPlaying) return anchorPosition
        val elapsed = SystemClock.elapsedRealtime() - anchorRealtime
        val pos = anchorPosition + (elapsed.toDouble() * playbackSpeed.toDouble()).toLong()
        val duration = metadataDurationMs
        return if (duration > 0) pos.coerceIn(0L, duration) else pos.coerceAtLeast(0L)
    }

    private var lastDiagAt = 0L
    private var lastAlignAt = 0L
    private var publishedLines: List<RichLyricLine> = emptyList()

    private fun tickProgress() {
        val registered = provider ?: return
        if (!hasAnchor) return

        val diagNow = SystemClock.elapsedRealtime()
        if (diagNow - lastAlignAt >= 3_000L && publishedLines.isNotEmpty()) {
            lastAlignAt = diagNow
            val clock = extrapolatedPosition()
            var index = -1
            for (i in publishedLines.indices) {
                if (publishedLines[i].begin <= clock) index = i else break
            }
            logger.info(
                "对齐: clock=" + clock + " " + (if (anchorPlaying) "播放" else "暂停") +
                    " 第" + (index + 1) + "/" + publishedLines.size + "行 [" +
                    (publishedLines.getOrNull(index)?.text ?: "") + "]"
            )
        }
        if (diagNow - lastDiagAt >= 10_000L) {
            lastDiagAt = diagNow
            logger.debug(
                "进度: pos=${extrapolatedPosition()} playing=$anchorPlaying " +
                    "anchor=$anchorPosition speed=$playbackSpeed " +
                    "lyrics=${if (lyricFound) "有" else "无"}"
            )
        }
        runCatching {
            if (lastPushedPlaying != anchorPlaying) {
                registered.player.setPlaybackState(anchorPlaying)
                lastPushedPlaying = anchorPlaying
            }
            registered.player.setPosition(extrapolatedPosition())
        }
    }

    private fun songSignature(title: String?, artist: String?, mediaId: String?): String {
        fun fold(value: String?): String =
            value?.takeIf { it.isNotBlank() && it != "null" }.orEmpty()
        return fold(title) + "|" + fold(artist) + "|" + fold(mediaId)
    }

    private fun tryLookup() {
        if (!lyricAllowed()) return
        if (lyricFound) return
        val signature = currentSignature ?: return
        val parts = signature.split('|')
        val title = parts.getOrNull(0).orEmpty()
        val artist = parts.getOrNull(1)?.takeIf { it.isNotBlank() && it != "null" }
        val duration = metadataDurationMs
        val mediaId = parts.getOrNull(2)?.takeIf { it.isNotBlank() && it != "null" }
            ?: currentMediaId
        if (title.isBlank()) return

        val index = attemptIndex.getAndIncrement()
        val delay = RETRY_DELAYS_MS.getOrElse(index) { RETRY_DELAYS_MS.last() }
        val lastAttempt = index >= RETRY_DELAYS_MS.size

        val localRecipe = recipe
        if (localRecipe == null && hostPackage != Constants.LUNA_PACKAGE) {

            if (lastAttempt) publishFallback(title, artist, duration, mediaId)
            else mainHandler.postDelayed(retryTask, delay)
            return
        }

        lookupExecutor.execute {
            val context = application ?: currentApplication()
            if (context == null) {
                logger.warn("Lookup skipped: no context in $processName")
                return@execute
            }
            val started = SystemClock.elapsedRealtime()
            if (hostPackage == Constants.LUNA_PACKAGE) {

                val luna = runCatching { LunaLyric.find(context, mediaId) }.getOrNull()
                val cost = SystemClock.elapsedRealtime() - started
                if (luna != null && luna.lines.size >= 3) {
                    if (currentSignature != signature) {
                        logger.info("Dropped stale lyric (song changed)")
                        return@execute
                    }
                    lyricFound = true
                    mainHandler.post {
                        publish(title, artist, duration, mediaId, luna.lines, "汽水音乐")
                    }
                    return@execute
                }
                logger.info("汽水音乐诊断#" + (index + 1) + "：" + LunaLyric.describe(context, mediaId))
                logger.info("汽水音乐：缓存里还没有歌词（attempt #${index + 1}, ${cost}ms）")
                if (!lyricFound && currentSignature == signature) {
                    if (lastAttempt) {
                        mainHandler.post { publishFallback(title, artist, duration, mediaId) }
                    } else {
                        mainHandler.postDelayed(retryTask, delay)
                    }
                }
                return@execute
            }
            val target = localRecipe!!
            var found = try {
                LocalLyricFinder.find(
                    context, target, title, artist, duration, mediaId,
                    songStartedAtMs = songSwitchWall
                ) {
                    logger.info("[${target.displayName}] $it")
                }
            } catch (t: Throwable) {
                logger.error("Local lyric lookup failed: ${t.message}", t)
                null
            }
            if (found == null) {

                
                found = try {
                    LocalLyricFinder.findInOwnStorage(
                        context, title, artist, duration, mediaId,
                        songStartedAtMs = songSwitchWall
                    ) {
                        logger.info("[${target.displayName}·私有目录] $it")
                    }
                } catch (t: Throwable) {
                    logger.error("私有目录扫描失败: ${t.message}", t)
                    null
                }
            }
            val lines = found?.lines
            val cost = SystemClock.elapsedRealtime() - started
            if (lines.isNullOrEmpty()) {
                logger.info("No local lyric (attempt #${index + 1}, ${cost}ms): $title - $artist")
            } else {
                if (currentSignature != signature) {
                    logger.info("Dropped stale lyric (song changed)")
                    return@execute
                }
                lyricFound = true
                mainHandler.post {
                    publish(title, artist, duration, mediaId, lines, localRecipe.displayName, found.title)
                }
                return@execute
            }
            if (!lyricFound && currentSignature == signature) {
                if (lastAttempt) {
                    mainHandler.post { publishFallback(title, artist, duration, mediaId) }
                } else {
                    mainHandler.postDelayed(retryTask, delay)
                }
            }
        }
    }

    private fun publish(
        title: String,
        artist: String?,
        durationMs: Long,
        mediaId: String?,
        lyrics: List<RichLyricLine>,
        source: String,
        nameHint: String? = null
    ) {
        val registered = ensureProvider()
        if (registered == null) {
            logger.warn("Lyric dropped: provider not registered in $processName")
            return
        }
        val lines = sanitize(lyrics)
        if (lines.isEmpty()) {
            logger.warn("Lyric dropped: no usable line for $title")
            return
        }

        for (line in lines) line.translation = null
        if (pendingSwitchAt != 0L) {
            pendingSwitchLyric = LocalLyric(lines, null, null, null, title)
            logger.info("歌词暂存（音频还没切过来）：" + title + "，" + lines.size + " 行")
            return
        }

        val displayTitle = if (!nameHint.isNullOrBlank() &&
            PlayerTitle.isSuspectTitle(title, knownLyricLines) &&
            !PlayerTitle.isSuspectTitle(nameHint)
        ) nameHint else title
        knownLyricLines = lines.mapNotNullTo(HashSet(lines.size)) {
            PlayerTitle.normalize(it.text).takeIf { text -> text.isNotEmpty() }
        }
        if (displayTitle != title) currentTitle = displayTitle

        val lastEnd = lines.last().end
        val song = Song().apply {

            id = mediaId ?: title
            name = displayTitle
            this.artist = artist
            this.duration = if (durationMs > 0) durationMs else lastEnd
            this.lyrics = lines
        }
        val words = lines.count { !it.words.isNullOrEmpty() }
        val translated = lines.count { !it.translation.isNullOrBlank() }
        val ok = try {
            registered.player.setSong(song)
        } catch (t: Throwable) {
            logger.error("setSong threw", t)
            false
        }
        lyricFound = true
        publishedLines = lines
        lastAlignAt = 0L
        logger.info(
            "Lyric published from $source: $displayTitle, ${lines.size} lines " +
                "(逐字 $words 行, 翻译 $translated 行), id=${song.id}, " +
                "duration=${song.duration}, active=${registered.player.isActive}, ok=$ok"
        )
    }

    

    private fun publishPending(title: String, artist: String?, durationMs: Long, mediaId: String?) {
        val cached = pendingLyric ?: return
        val (lyric, source, at) = cached
        val age = SystemClock.elapsedRealtime() - at
        if (age > PENDING_TTL_MS) {
            pendingLyric = null
            return
        }
        if (!belongsToCurrentSong(lyric, at, title, artist, durationMs, mediaId)) return
        logger.info("[$source] 使用缓存歌词（${age}ms 前嗅到）")
        publish(title, artist, durationMs, mediaId, lyric.lines, source, lyric.title)
    }

    private fun publishPlaceholder(title: String, artist: String?, durationMs: Long, mediaId: String?) {
        val registered = ensureProvider() ?: return
        val song = Song().apply {
            id = mediaId ?: title
            name = title
            this.artist = artist
            this.duration = durationMs
            this.lyrics = emptyList()
        }
        val ok = runCatching { registered.player.setSong(song) }.getOrDefault(false)
        logger.info("切歌占位：推送歌曲信息 $title - $artist, ok=$ok")
    }

    private fun publishFallback(title: String, artist: String?, durationMs: Long, mediaId: String?) {
        if (lyricFound || fallbackSent) return
        val registered = ensureProvider() ?: return
        fallbackSent = true
        val song = Song().apply {
            id = mediaId ?: title
            name = title
            this.artist = artist
            this.duration = durationMs
            this.lyrics = emptyList()
        }
        val ok = runCatching { registered.player.setSong(song) }.getOrDefault(false)
        logger.info("无歌词兜底：推送歌曲信息 $title - $artist, ok=$ok")
    }

    private fun sanitize(raw: List<RichLyricLine>): List<RichLyricLine> {
        val cleaned = raw
            .filter {
                !it.text.isNullOrBlank() && it.begin >= 0 &&
                    !LyricParsers.looksLikeNoise(it.text.orEmpty())
            }
            .sortedBy { it.begin }
        if (cleaned.isEmpty()) return emptyList()

        val out = ArrayList<RichLyricLine>(cleaned.size)
        for (i in cleaned.indices) {
            val line = cleaned[i]
            val nextBegin = cleaned.getOrNull(i + 1)?.begin
            var end = if (line.end > line.begin) line.end else line.begin + 1
            if (nextBegin != null && end > nextBegin) end = nextBegin
            if (end <= line.begin) end = line.begin + 1
            line.end = end
            line.duration = end - line.begin
            clampWords(line)
            out.add(line)
        }
        return out
    }

    private fun clampWords(line: RichLyricLine) {
        val words = line.words ?: return
        for (word in words) {
            if (word.begin < line.begin) word.begin = line.begin
            if (word.end <= word.begin) word.end = word.begin + 1
            if (word.end > line.end) word.end = line.end
            if (word.end <= word.begin) word.end = word.begin + 1
            word.duration = word.end - word.begin
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

                    try {
                        val result = chain.proceed()
                        try {
                            callback(chain, result)
                        } catch (t: Throwable) {
                            logger.error("After-hook failed: $description", t)
                            HookCrashLog.record(description, t)
                        }
                        result
                    } catch (t: Throwable) {
                        HookCrashLog.record("proceed:$description", t)
                        throw t
                    }
                }
            logger.debug("Installed protective hook: $description")
        } catch (t: Throwable) {
            logger.error("Unable to install hook: $description", t)
        }
    }

    companion object {

        
        private const val SWITCH_GRACE_MS = 1_500L

        private const val BACKWARD_TOLERANCE_MS = 1_500L

        private const val ZERO_RESET_IGNORE_MS = 3_000L

        private const val SONG_SWITCH_IGNORE_MS = 2_500L

        private const val ANCHOR_SWITCH_GRACE_MS = 400L

        private const val SWITCH_MAX_ACCEPT_MS = 8_000L

        private const val SWITCH_HOLD_TOLERANCE_MS = 3_000L

        private const val SYNC_TOLERANCE_MS = 3_000L

        private const val JUMP_CONFIRM_WINDOW_MS = 2_000L

        private const val JUMP_MIN_MOVE_MS = 2_000L

        private const val JUMP_CONFIRM_SLACK_MS = 800L

        private const val SWITCH_HOLD_TIMEOUT_MS = 25_000L

        private const val SWITCH_HOLD_FALLBACK_MS = 1_500L
        private const val PENDING_TTL_MS = 30_000L

        private const val TICK_INTERVAL_MS = 48L

        private val RETRY_DELAYS_MS = longArrayOf(
            600L, 1200L, 2000L, 3000L, 4500L, 6500L, 9000L,
            12000L, 16000L, 20000L, 25000L, 30000L
        )
    }
}
