package com.astraflow.Chizuru.lyric

import com.astraflow.Chizuru.settings.ModulePrefs
import android.app.Application
import android.app.Instrumentation
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.proify.lyricon.lyric.model.RichLyricLine
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.provider.LyriconFactory
import io.github.proify.lyricon.provider.LyriconProvider
import io.github.proify.lyricon.provider.ProviderLogo
import java.io.File
import java.lang.reflect.Executable
import java.util.concurrent.Executors
import kotlin.math.abs

internal class KuwoLyricProvider(
    private val module: XposedModule,
    private val logger: ModuleLogger,
    private val classLoader: ClassLoader,
    private val hostPackage: String,
    private val processName: String
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lookupExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "mitian-kuwo-lookup").apply { isDaemon = true }
    }

    @Volatile private var provider: LyriconProvider? = null
    @Volatile private var application: Application? = null

    @Volatile private var signature: String? = null
    @Volatile private var currentTitle: String? = null
    @Volatile private var currentArtist: String? = null
    @Volatile private var currentDuration: Long = 0L
    @Volatile private var currentMediaId: String? = null
    @Volatile private var songSwitchAt: Long = 0L
    @Volatile private var songSwitchWall: Long = 0L
    @Volatile private var lyricFound = false

    @Volatile private var anchorPosition = 0L
    @Volatile private var anchorRealtime = 0L
    @Volatile private var anchorPlaying = false
    @Volatile private var playbackSpeed = 1.0f
    @Volatile private var hasAnchor = false
    @Volatile private var lastPushedPlaying: Boolean? = null

    @Volatile private var hasPreciseLyric = false
    private val hookedLock = Any()
    private var lastHookedKey = 0

    private var attemptCount = 0

    private val progressTicker = object : Runnable {
        override fun run() {
            tickProgress()
            mainHandler.postDelayed(this, TICK_MS)
        }
    }

    private val lookupRetry = Runnable { tryLookupLyric() }

    fun installHooks() {
        hookApplicationLifecycle()
        hookMediaSession()
        installKuwoSources()
        logger.info("Kuwo lyric provider installed for $hostPackage in $processName")
    }

    private fun ensureProvider(): LyriconProvider? {
        provider?.let { return it }
        val ctx = application ?: currentApplication() ?: return null
        return try {
            val logo = runCatching { ProviderLogo.fromSvg(Constants.ICON) }.getOrNull()
            val created = LyriconFactory.createProvider(
                context = ctx,
                providerPackageName = Constants.PROVIDER_PACKAGE_NAME,
                playerPackageName = hostPackage,
                logo = logo,
                processName = processName
            ).apply {
                player.setDisplayRoma(false)
                register()
            }
            provider = created
            logger.info("Kuwo provider registered in $processName")
            created
        } catch (t: Throwable) {
            logger.error("Kuwo provider register failed", t)
            null
        }
    }

    private fun currentApplication(): Application? = try {
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication").invoke(null) as? Application
    } catch (_: Throwable) { null }

    private fun hookApplicationLifecycle() {
        val method = Instrumentation::class.java.getDeclaredMethod(
            "callApplicationOnCreate", Application::class.java
        )
        installProtectiveAfterHook(method, "Instrumentation.callApplicationOnCreate") { chain, _ ->
            (chain.args.getOrNull(0) as? Application)?.let { application = it }
        }
    }

    private fun hookMediaSession() {
        val md = MediaSession::class.java.getDeclaredMethod(
            "setMetadata", MediaMetadata::class.java
        )
        installProtectiveAfterHook(md, "MediaSession.setMetadata") { chain, _ ->
            onMetadataChanged(chain.args.getOrNull(0) as? MediaMetadata)
        }
        val pb = MediaSession::class.java.getDeclaredMethod(
            "setPlaybackState", PlaybackState::class.java
        )
        installProtectiveAfterHook(pb, "MediaSession.setPlaybackState") { chain, _ ->
            onPlaybackStateChanged(chain.args.getOrNull(0) as? PlaybackState)
        }
    }

    private fun onMetadataChanged(md: MediaMetadata?) {
        md ?: return
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() && it != "未知歌曲" } ?: return
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() && it != "未知歌手" }
        val duration = md.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val mediaId = md.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?.takeIf { it.isNotBlank() && it != "null" }
        val newSig = "$title|$artist|$duration|$mediaId"
        if (newSig == signature) return
        signature = newSig
        currentTitle = title
        currentArtist = artist
        currentDuration = duration
        currentMediaId = mediaId
        songSwitchAt = SystemClock.elapsedRealtime()
        songSwitchWall = System.currentTimeMillis()
        lyricFound = false
        hasPreciseLyric = false
        synchronized(hookedLock) { lastHookedKey = 0 }
        attemptCount = 0

        logger.info("Kuwo 元数据：$title - $artist (${duration}ms) id=$mediaId")
        ensureProvider()

        anchorPosition = 0L
        anchorRealtime = SystemClock.elapsedRealtime()
        anchorPlaying = false
        playbackSpeed = 1.0f
        hasAnchor = false
        lastPushedPlaying = null
        startTicker()

        pushPlaceholder()

        tryLookupLyric()
    }

    private fun onPlaybackStateChanged(st: PlaybackState?) {
        st ?: return
        ensureProvider() ?: return
        val playing = when (st.state) {
            PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_ERROR -> st.position >= 0L && false
            PlaybackState.STATE_NONE -> anchorPlaying
            else -> true
        }
        val speed = st.playbackSpeed.takeIf { it > 0f } ?: 1.0f

        val now = SystemClock.elapsedRealtime()
        val reportAge = if (st.lastPositionUpdateTime in 1..now) now - st.lastPositionUpdateTime else -1L
        val rawPos = st.position
        val pos = if (rawPos >= 0L && playing && reportAge in 0..8_000L) {
            rawPos + (reportAge * speed.toDouble()).toLong()
        } else rawPos

        if (pos >= 0L) {
            val expected = extrapolatedPosition()
            val staleDrop = hasAnchor && playing &&
                pos < expected - STALE_TOLERANCE_MS &&
                st.state == PlaybackState.STATE_PLAYING

            val justSwitched = now - songSwitchAt < SWITCH_POS_GRACE_MS &&
                pos > SWITCH_POS_MAX_MS
            if (!staleDrop && !justSwitched) {
                anchorPosition = pos
                anchorRealtime = now
                hasAnchor = true
            }
            anchorPlaying = playing
            playbackSpeed = speed
        } else {
            anchorPlaying = playing
        }

        if (!hasAnchor && playing) {
            anchorPosition = pos.coerceAtLeast(0L)
            anchorRealtime = now
            hasAnchor = true
            logger.info("Kuwo 兜底起锚 pos=$anchorPosition")
        }

        startTicker()
        tickProgress()
    }

    private fun installKuwoSources() {
        val pending = arrayListOf<Pair<String, () -> Boolean>>(
            "歌词回调 onGetLyric" to ::hookLyricDelegate,
            "逐字解析器 fh.g" to ::hookKdtxParser,
            "歌词下载 e0.f" to ::hookLyricsDownload
        )
        Thread {
            var round = 0
            while (pending.isNotEmpty() && round < 240) {
                val iterator = pending.iterator()
                while (iterator.hasNext()) {
                    val (name, installer) = iterator.next()
                    if (runCatching { installer() }.getOrDefault(false)) {
                        logger.info("酷我歌词直取已挂载：$name")
                        iterator.remove()
                    }
                }
                if (pending.isEmpty()) return@Thread
                Thread.sleep(500)
                round++
            }
            if (pending.isNotEmpty()) {
                logger.debug("酷我歌词直取未挂载（混淆名可能变了）：" + pending.joinToString { it.first })
            }
        }.apply {
            name = "mitian-kuwo-src"
            isDaemon = true
        }.start()
    }

    private fun hookLyricDelegate(): Boolean {
        val clazz = loadClass("cn.kuwo.mod.playcontrol.session.media.MediaSessionDelegate")
            ?: return false
        var hooked = false
        for (method in clazz.declaredMethods) {
            if (method.name != "onGetLyric") continue
            val types = method.parameterTypes
            if (types.isEmpty() || types.last() != String::class.java) continue
            installProtectiveAfterHook(method, "MediaSessionDelegate.onGetLyric") { chain, _ ->
                val text = chain.args.lastOrNull() as? String
                if (!text.isNullOrBlank() &&
                    !text.contains("正在搜索") && !text.contains("搜索歌词")
                ) {
                    submitHookedLrc(text)
                }
            }
            hooked = true
        }
        return hooked
    }

    private fun hookKdtxParser(): Boolean {
        val clazz = loadClass("fh.g") ?: return false
        var hooked = false
        for (method in clazz.declaredMethods) {
            if (method.name != "a") continue
            val types = method.parameterTypes
            if (types.size != 2 || types[0] != ByteArray::class.java ||
                types[1] != Integer.TYPE
            ) continue
            installProtectiveAfterHook(method, "fh.g.a (KDTX逐字)") { _, result ->
                result?.let { submitHookedKdtx(it) }
            }
            hooked = true
        }
        return hooked
    }

    private fun hookLyricsDownload(): Boolean {
        val clazz = loadClass("cn.kuwo.mod.lyrics.e0") ?: return false
        val music = loadClass("cn.kuwo.base.bean.Music") ?: return false
        var hooked = false
        for (method in clazz.declaredMethods) {
            if (method.name != "f") continue
            val types = method.parameterTypes
            if (types.size != 3 || types[0] != music ||
                types[1] != java.lang.Boolean.TYPE || types[2] != music
            ) continue
            installProtectiveAfterHook(method, "e0.f (歌词下载)") { _, result ->
                result ?: return@installProtectiveAfterHook
                val data = readField(result, "lyricsData") as? String
                if (data.isNullOrEmpty() || !KUWO_WORD_TAG.containsMatchIn(data)) {
                    return@installProtectiveAfterHook
                }
                val offset = (readField(result, "offset") as? Int) ?: 0
                submitHookedLrcx(data, offset)
            }
            hooked = true
        }
        return hooked
    }

    private fun submitHookedLrc(text: String) {
        lookupExecutor.execute {
            val lyric = runCatching { LyricParsers.parseLrcText(text) }.getOrNull()
                ?: return@execute
            if (!LyricParsers.isUsable(lyric.lines)) return@execute
            for (line in lyric.lines) {
                if (line.words.isNullOrEmpty()) line.words = KuwoLyric.synthesizeWords(line)
            }
            publishHooked(lyric, precise = false, source = "onGetLyric")
        }
    }

    private fun submitHookedKdtx(result: Any) {
        lookupExecutor.execute {
            val lyric = runCatching { KuwoLyric.parseKdtxObject(result) }.getOrNull()
                ?: return@execute
            if (!LyricParsers.isUsable(lyric.lines)) return@execute
            publishHooked(lyric, precise = true, source = "KDTX")
        }
    }

    private fun submitHookedLrcx(text: String, offsetMs: Int) {
        lookupExecutor.execute {
            val lyric = runCatching { LyricParsers.parseKuwoLrcx(text) }.getOrNull()

                ?: runCatching { LyricParsers.parseKuwoLrcx("[kuwo:11]\n$text") }.getOrNull()
                ?: return@execute
            if (!LyricParsers.isUsable(lyric.lines)) return@execute
            if (offsetMs != 0) shift(lyric.lines, offsetMs.toLong())
            publishHooked(lyric, precise = true, source = "LRCX")
        }
    }

    private fun shift(lines: List<RichLyricLine>, delta: Long) {
        for (line in lines) {
            line.begin += delta
            line.end += delta
            line.duration = line.end - line.begin
            line.words?.forEach { w ->
                w.begin += delta
                w.end += delta
                w.duration = w.end - w.begin
            }
        }
    }

    private fun publishHooked(lyric: LocalLyric, precise: Boolean, source: String) {
        val key = (if (precise) 1 else 0) * 31 +
            lyric.lines.size * 7 +
            (lyric.lines.firstOrNull()?.text?.hashCode() ?: 0)
        synchronized(hookedLock) {
            if (key == lastHookedKey) return
            lastHookedKey = key
        }
        if (!precise && hasPreciseLyric) return
        val title = currentTitle ?: return
        val sig = signature ?: return

        val embedded = guessSongTitle(lyric)
        if (embedded != null) {
            val want = LocalLyricFinder.titleCore(title)
            val got = LocalLyricFinder.titleCore(embedded)
            if (want.isNotEmpty() && got.isNotEmpty() &&
                !(got == want || got.contains(want) || want.contains(got))
            ) {
                logger.info("酷我直取丢弃他歌歌词：$source ti=$embedded 当前=$title")
                return
            }
        } else if (currentDuration > 0L) {
            val last = lyric.lines.lastOrNull()?.begin ?: return
            if (abs(last - currentDuration) > HOOKED_DURATION_TOLERANCE_MS) {
                logger.info(
                    "酷我直取丢弃时长不符歌词：$source 末行=${last}ms 时长=${currentDuration}ms"
                )
                return
            }
        }

        lyricFound = true
        if (precise) hasPreciseLyric = true
        mainHandler.post {
            if (signature != sig) return@post
            logger.info("酷我直取命中($source)：${lyric.lines.size} 行")
            publish(lyric)
        }
    }

    private fun readField(obj: Any, name: String): Any? = runCatching {
        val field = runCatching { obj.javaClass.getField(name) }.getOrNull()
            ?: obj.javaClass.getDeclaredField(name).apply { isAccessible = true }
        field.get(obj)
    }.getOrNull()

    private fun loadClass(name: String): Class<*>? =
        runCatching { Class.forName(name, false, classLoader) }.getOrNull()

    private fun tryLookupLyric() {
        if (lyricFound) return
        val sig = signature ?: return
        val title = currentTitle ?: return
        val duration = currentDuration
        val attempt = attemptCount++

        lookupExecutor.execute {
            val ctx = application ?: currentApplication() ?: return@execute
            val dir = File(ctx.getExternalFilesDir(null), "KuwoMusic/data/LYRICS_CACHE")
            if (!dir.isDirectory) {
                File(ctx.filesDir, "KuwoMusic/data/LYRICS_CACHE").let {
                    if (it.isDirectory) return@execute scanDir(it, sig, title, duration)
                }
                scheduleRetry(attempt)
                return@execute
            }
            scanDir(dir, sig, title, duration)
            scheduleRetry(attempt)
        }
    }

    private fun scheduleRetry(attempt: Int) {
        if (lyricFound || signature == null) return
        if (attempt >= RETRY_DELAYS.size) {

            mainHandler.post { pushFallback() }
            return
        }
        mainHandler.postDelayed(lookupRetry, RETRY_DELAYS[attempt])
    }

    private fun scanDir(dir: File, sig: String, title: String, duration: Long) {
        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".dat") }
            ?.sortedByDescending { it.lastModified() } ?: return
        if (files.isEmpty()) return

        val wantTitle = LocalLyricFinder.normalize(title)
        val wantCore = LocalLyricFinder.titleCore(title)
        val startedAt = songSwitchWall
        val now = System.currentTimeMillis()

        var titled: Pair<File, LocalLyric>? = null
        var fresh: Pair<File, LocalLyric>? = null
        var freshDiff = Long.MAX_VALUE

        for (file in files.take(MAX_SCAN)) {
            if (file.length() > MAX_FILE_BYTES) continue
            val lyric = runCatching {
                LyricParsers.parseKuwoDat(file.readBytes())
            }.getOrNull() ?: continue
            if (!LyricParsers.isUsable(lyric.lines)) continue

            val embeddedTitle = guessSongTitle(lyric)
            val embeddedCore = LocalLyricFinder.titleCore(embeddedTitle)
            if (embeddedCore.isNotEmpty() && wantCore.isNotEmpty() &&
                (embeddedCore == wantCore || embeddedCore.contains(wantCore) ||
                    wantCore.contains(embeddedCore))
            ) {

                if (artistConfirms(lyric, currentArtist) != false) {
                    titled = file to lyric
                    logger.info("Kuwo 歌名匹配命中：" + file.name + " ti=" + embeddedTitle)
                    break
                }
                logger.info("Kuwo 歌名对但歌手不符：" + file.name + " ar=" + lyric.artist)
            }

            if (duration <= 0L) continue
            if (artistConflicts(lyric, currentArtist)) {
                logger.debug("Kuwo 排除 " + file.name + "：ar=" + lyric.artist + " 与当前不符")
                continue
            }
            val lastBegin = lyric.lines.lastOrNull()?.begin ?: continue
            val diff = abs(lastBegin - duration)
            val mtime = file.lastModified()
            val isFreshWrite = startedAt > 0 && mtime >= startedAt - WRITE_BEFORE_MS
            val arBoost = artistConfirms(lyric, currentArtist) == true
            val tolerance = when {
                arBoost -> DURATION_TOLERANCE_MS + 2_000L
                isFreshWrite -> DURATION_TOLERANCE_MS
                else -> OLD_CACHE_TOLERANCE_MS
            }
            if (diff > tolerance) continue
            if (!isFreshWrite && !arBoost &&
                now - mtime > WRITE_AFTER_MS && diff > OLD_CACHE_TOLERANCE_MS
            ) continue
            if (diff < freshDiff) {
                freshDiff = diff
                fresh = file to lyric
            }
        }

        val picked = titled ?: fresh
        picked ?: return
        if (signature != sig) {
            logger.info("Kuwo 丢弃过期歌词（已切歌）")
            return
        }
        lyricFound = true
        val how = if (titled != null) "ti匹配" else "时长+新写入"
        mainHandler.post {
            logger.info("Kuwo 命中(" + how + ")：" + picked.first.name)
            publish(picked.second)
        }
    }

    private fun guessSongTitle(lyric: LocalLyric): String? {
        val ti = lyric.title?.takeIf { it.isNotBlank() }
        if (ti != null) return ti
        val first = lyric.firstLine?.trim().orEmpty()
        if (first.isEmpty()) return null
        if (CREDIT_PREFIX.any { first.startsWith(it) }) return null
        val guess = LyricParsers.guessTitleArtist(first) ?: return first
        return guess.first
    }

    private fun artistConfirms(lyric: LocalLyric, currentArtist: String?): Boolean? {
        val want = LocalLyricFinder.normalize(currentArtist)
        if (want.isEmpty()) return null
        val embedded = LocalLyricFinder.normalize(lyric.artist)
        if (embedded.isEmpty()) return null
        return embedded == want || embedded.contains(want) || want.contains(embedded)
    }

    private fun artistConflicts(lyric: LocalLyric, currentArtist: String?): Boolean {
        return artistConfirms(lyric, currentArtist) == false
    }

    private fun publish(lyric: LocalLyric) {
        val p = ensureProvider() ?: return
        val title = currentTitle ?: return
        val artist = currentArtist
        val duration = currentDuration
        val mediaId = currentMediaId

        val lines = lyric.lines
            .filter { !it.text.isNullOrBlank() && it.begin >= 0 }
            .sortedBy { it.begin }
        if (lines.isEmpty()) return

        for (i in lines.indices) {
            val line = lines[i]
            val nextBegin = lines.getOrNull(i + 1)?.begin
            var end = if (line.end > line.begin) line.end else line.begin + 1
            if (nextBegin != null && end > nextBegin) end = nextBegin
            if (end <= line.begin) end = line.begin + 1
            line.end = end
            line.duration = end - line.begin
            line.words?.forEach { w ->
                if (w.begin < line.begin) w.begin = line.begin
                if (w.end <= w.begin) w.end = w.begin + 1
                if (w.end > line.end) w.end = line.end
                w.duration = w.end - w.begin
            }
        }

        val song = Song().apply {
            id = mediaId ?: "kuwo|$title|$artist"
            name = lyric.title ?: title
            this.artist = artist
            this.duration = if (duration > 0) duration else lines.last().end
            this.lyrics = lines
        }
        val ok = runCatching { p.player.setSong(song) }.getOrDefault(false)
        logger.info("Kuwo 推送歌词：${lines.size} 行（id=${song.id} ok=$ok）")
    }

    private fun pushPlaceholder() {
        val p = provider ?: return
        val title = currentTitle ?: return
        val artist = currentArtist
        val duration = currentDuration
        val mediaId = currentMediaId
        val song = Song().apply {
            id = mediaId ?: "kuwo|$title|$artist"
            name = title
            this.artist = artist
            this.duration = duration
            this.lyrics = emptyList()
        }
        runCatching { p.player.setSong(song) }
    }

    private fun pushFallback() {
        if (lyricFound) return
        pushPlaceholder()
    }

    private fun startTicker() {
        mainHandler.removeCallbacks(progressTicker)
        mainHandler.post(progressTicker)
    }

    private fun extrapolatedPosition(): Long {
        if (!hasAnchor) return 0L
        if (!anchorPlaying) return anchorPosition
        val elapsed = SystemClock.elapsedRealtime() - anchorRealtime
        val pos = anchorPosition + (elapsed * playbackSpeed.toDouble()).toLong()
        val dur = currentDuration
        return if (dur > 0) pos.coerceIn(0L, dur) else pos.coerceAtLeast(0L)
    }

    private fun tickProgress() {
        val p = provider ?: return
        if (!hasAnchor) return
        if (lastPushedPlaying != anchorPlaying) {
            runCatching { p.player.setPlaybackState(anchorPlaying) }
            lastPushedPlaying = anchorPlaying
        }
        runCatching { p.player.setPosition(extrapolatedPosition()) }
    }

    private fun isLyricEnabled(): Boolean = runCatching {
        val prefs = module.getRemotePreferences(ModulePrefs.NAME)
        prefs.getBoolean(ModulePrefs.KEY_ENABLED, true) &&
            prefs.getBoolean(ModulePrefs.KEY_LYRIC, true)
    }.getOrDefault(true)

    private fun installProtectiveAfterHook(
        executable: Executable, description: String,
        callback: (XposedInterface.Chain, Any?) -> Unit
    ) {
        try {
            module.hook(executable)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    if (!isLyricEnabled()) return@intercept result
                    try { callback(chain, result) }
                    catch (t: Throwable) { logger.error("After-hook failed: $description", t) }
                    result
                }
        } catch (t: Throwable) {
            logger.error("Unable to install hook: $description", t)
        }
    }

    private companion object {
        private const val TICK_MS = 48L
        private const val STALE_TOLERANCE_MS = 3_500L
        private val CREDIT_PREFIX = listOf(
            "作词", "作曲", "编曲", "演唱", "制作人", "监制", "混音", "录音",
            "和声", "母带", "出品", "发行", "统筹", "策划", "OP", "SP", "吉他", "贝斯", "鼓"
        )

        private const val SWITCH_POS_GRACE_MS = 2_500L

        private const val SWITCH_POS_MAX_MS = 12_000L
        private const val DURATION_TOLERANCE_MS = 2_500L

        private const val OLD_CACHE_TOLERANCE_MS = 800L
        private const val WRITE_BEFORE_MS = 30_000L
        private const val WRITE_AFTER_MS = 120_000L

        private val KUWO_WORD_TAG = Regex("""<\d+,\d+""")

        private const val HOOKED_DURATION_TOLERANCE_MS = 12_000L
        private const val MAX_SCAN = 200
        private const val MAX_FILE_BYTES = 2L * 1024 * 1024
        private val RETRY_DELAYS = longArrayOf(
            600L, 1200L, 2000L, 3000L, 4500L, 6500L, 9000L,
            12000L, 16000L, 20000L, 25000L, 30000L
        )
    }
}
