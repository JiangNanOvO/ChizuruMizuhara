package com.astraflow.Chizuru.lyric

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.security.MessageDigest
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.Executors
import com.astraflow.Chizuru.redactUrl

internal fun looksLikeLyricPayloadHead(head: ByteArray): Boolean {
    if (head.size < 4) return true
    if (KuwoLyric.isGzip(head)) return true
    if (KuwoLyric.looksLike(head)) return true
    val text = String(head, 0, minOf(head.size, 96), Charsets.ISO_8859_1)
    return text.contains('[') || text.contains("lrcx") || text.contains("TP=") ||
        text.contains("lyric", true) || text.contains("LyricContent")
}

internal class HttpLyricSniffer(
    private val module: XposedModule,
    private val logger: ModuleLogger,
    private val classLoader: ClassLoader,
    private val isActive: () -> Boolean = { true },
    private val onLyric: (LocalLyric, String) -> Unit
) {

    private val maxPayload = 1_500_000

    private val seen = LinkedHashSet<String>()

    private val probed = LinkedHashSet<String>()

    private fun probedOnce(url: String, size: Int): Boolean = synchronized(probed) {
        if (probed.size > 64) probed.clear()
        probed.add(url + "|" + size)
    }

    private val bodyUrls: MutableMap<Any, String> = Collections.synchronizedMap(WeakHashMap())

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mitian-net-lyric").apply { isDaemon = true }
    }

    fun install() {
        val bodyClass = runCatching {
            Class.forName("okhttp3.ResponseBody", false, classLoader)
        }.getOrNull()
        if (bodyClass == null) {
            logger.debug("okhttp3.ResponseBody 不存在，跳过网络歌词嗅探")
        } else {
            hookMethod(bodyClass, "string")
            hookMethod(bodyClass, "bytes")
            hookStream(bodyClass)
        }

        for (name in listOf("okio.RealBufferedSource", "okio.Buffer")) {
            val klass = runCatching { Class.forName(name, false, classLoader) }.getOrNull() ?: continue
            hookMethod(klass, "readByteArray")
        }
        for (name in listOf("okio.RealBufferedSource")) {
            val klass = runCatching { Class.forName(name, false, classLoader) }.getOrNull() ?: continue
            hookMethod(klass, "readUtf8")
        }
        installUrlLogger()
        installResponseProbe()
    }

    private fun looksLikeLyricUrl(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.contains(".krc") || lower.contains(".qrc") || lower.contains(".lrc")) return true
        return lower.contains("lyric") || lower.contains("/krc") || lower.contains("lrc=")
    }

    private fun installUrlLogger() {
        val clientClass = runCatching {
            Class.forName("okhttp3.OkHttpClient", false, classLoader)
        }.getOrNull() ?: return
        val requestClass = runCatching {
            Class.forName("okhttp3.Request", false, classLoader)
        }.getOrNull() ?: return
        val newCall = runCatching {
            clientClass.getDeclaredMethod("newCall", requestClass)
        }.getOrNull() ?: return
        val urlMethod = runCatching { requestClass.getDeclaredMethod("url") }.getOrNull() ?: return
        runCatching {
            module.hook(newCall)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!isActive()) return@intercept chain.proceed()
                    runCatching {
                        val url = urlMethod.invoke(chain.args.getOrNull(0))?.toString().orEmpty()
                        if (url.isNotEmpty() && looksLikeLyricUrl(url)) {
                            logger.info("歌词相关请求：${redactUrl(url)}")
                        }
                    }
                    chain.proceed()
                }
            logger.info("网络歌词嗅探已挂载：okhttp3.OkHttpClient#newCall")
        }.onFailure { logger.debug("OkHttpClient#newCall 挂载失败：" + it.message) }
    }

    private fun installResponseProbe() {
        val responseClass = runCatching {
            Class.forName("okhttp3.Response", false, classLoader)
        }.getOrNull() ?: return
        val bodyMethod = runCatching { responseClass.getDeclaredMethod("body") }.getOrNull()
        if (bodyMethod == null) {
            logger.debug("okhttp3.Response#body 不存在，跳过歌词响应探针")
            return
        }
        val requestMethod = runCatching { responseClass.getDeclaredMethod("request") }.getOrNull()
        val urlMethod = runCatching {
            Class.forName("okhttp3.Request", false, classLoader).getDeclaredMethod("url")
        }.getOrNull()
        val peekBody = runCatching {
            responseClass.getDeclaredMethod("peekBody", Long::class.javaPrimitiveType)
        }.getOrNull()
        runCatching {
            module.hook(bodyMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!isActive()) return@intercept chain.proceed()
                    val result = chain.proceed()
                    runCatching {
                        val response = chain.thisObject
                        val url = if (requestMethod != null && urlMethod != null) {
                            urlMethod.invoke(requestMethod.invoke(response))?.toString().orEmpty()
                        } else {
                            ""
                        }
                        if (url.isNotEmpty() && looksLikeLyricUrl(url)) {
                            result?.let { bodyUrls[it] = url }
                            probe(url, response, result, peekBody)
                        }
                    }
                    result
                }
            logger.info("网络歌词嗅探已挂载：okhttp3.Response#body（歌词响应探针）")
        }.onFailure { logger.debug("Response#body 挂载失败：" + it.message) }
    }

    private fun probe(url: String, response: Any?, body: Any?, peekBody: java.lang.reflect.Method?) {
        val length = callLong(body, "contentLength")
        if (length > maxPayload) {
            logger.info("歌词响应过大，跳过：len=$length ${redactUrl(url)}")
            return
        }
        if (response == null || peekBody == null) return
        val limit = if (length in 1..maxPayload.toLong()) length else 64L * 1024L
        val peeked = runCatching { peekBody.invoke(response, limit) }.getOrNull() ?: return
        val bytes = runCatching {
            peeked.javaClass.getMethod("bytes").invoke(peeked) as? ByteArray
        }.getOrNull() ?: return
        if (bytes.isEmpty()) {
            logger.info("歌词响应为空：${redactUrl(url)}")
            return
        }
        if (probedOnce(url, bytes.size)) {
            logger.info(
                "歌词响应：type=${callString(body, "contentType")} len=${bytes.size} " +
                    "head=${hex(bytes, 24)}"
            )
        }
        dispatch(bytes)
    }

    private fun hookStream(bodyClass: Class<*>) {
        val method = runCatching { bodyClass.getDeclaredMethod("byteStream") }.getOrNull()
        if (method == null) {
            logger.debug("ResponseBody#byteStream 不存在，跳过")
            return
        }
        try {
            module.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!isActive()) return@intercept chain.proceed()
                    val stream = chain.proceed() as? java.io.InputStream
                    if (stream == null) stream else KuwoStreamTee(stream) { dispatch(it) }
                }
            logger.info("网络歌词嗅探已挂载：okhttp3.ResponseBody#byteStream")
        } catch (throwable: Throwable) {
            logger.error("挂载 byteStream 钩子失败", throwable)
        }
    }

    private fun hookMethod(target: Class<*>, name: String) {
        val method = runCatching { target.getDeclaredMethod(name) }.getOrNull()
        if (method == null) {
            logger.debug("$target#$name 不存在，跳过")
            return
        }
        try {
            module.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!isActive()) return@intercept chain.proceed()
                    val result = chain.proceed()
                    runCatching {
                        if (bodyUrls[chain.thisObject] != null) {
                            val size = (result as? ByteArray)?.size ?: (result as? String)?.length ?: -1
                            logger.info(name + " 读到的歌词响应：len=" + size + " head=" + hexPreview(result))
                        }
                    }
                    try {
                        inspect(result)
                    } catch (throwable: Throwable) {
                        logger.error("嗅探 $name 失败", throwable)
                    }
                    result
                }
            logger.info("网络歌词嗅探已挂载：${target.name}#$name")
        } catch (throwable: Throwable) {
            logger.error("无法挂载嗅探钩子：$name", throwable)
        }
    }

    private fun inspect(result: Any?) {
        when (result) {
            is String -> {
                if (looksLikeKuwoText(result)) {
                    submitKuwo(result.toByteArray(Charsets.ISO_8859_1))
                } else if (looksLikeLyric(result)) {
                    submitString(result)
                }
            }
            is ByteArray -> inspectBytes(result)
        }
    }

    private fun inspectBytes(bytes: ByteArray) {
        if (bytes.size >= 2 && KuwoLyric.isGzip(bytes)) {

            val inflated = runCatching { java.util.zip.GZIPInputStream(bytes.inputStream()).readBytes() }
                .getOrNull()
            if (inflated != null && inflated.isNotEmpty() && inflated.size <= maxPayload) {
                inspectBytes(inflated)
            }
            return
        }
        if (KuwoLyric.looksLike(bytes)) {
            submitKuwo(bytes)
            return
        }
        if (bytes.size < 64 || bytes.size > maxPayload) return
        if (bytes[0] == 'k'.code.toByte() || looksLikeLyric(String(bytes, Charsets.UTF_8))) {
            submitBytes(bytes)
        }
    }

    private fun dispatch(bytes: ByteArray) {
        if (bytes.size < 32) return
        if (KuwoLyric.isGzip(bytes)) {
            inspectBytes(bytes)
            return
        }
        if (KuwoLyric.looksLike(bytes)) {
            submitKuwo(bytes)
            return
        }
        val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull() ?: return
        if (looksLikeLyric(text)) submitString(text)
    }

    private fun looksLikeKuwoText(text: String): Boolean {
        if (text.length < 32) return false
        val head = text.take(64)
        return head.contains("TP=content") || head.contains("lrcx=")
    }

    private fun submitKuwo(bytes: ByteArray) {
        if (!mark("kuwo:" + bytes.size + ':' + bytes[0] + ':' + bytes[bytes.size - 1])) return
        executor.execute {
            val text = KuwoLyric.decode(bytes)
            if (text == null) {
                logger.info("酷我歌词解码失败（" + bytes.size + " 字节）head=" + hex(bytes, 24))
                return@execute
            }
            val lyric = runCatching { LyricParsers.parseKuwoLrcx(text) }.getOrNull()
            if (lyric == null || !LyricParsers.isUsable(lyric.lines)) {
                logger.info("酷我歌词解析失败（${text.length} 字符，内容不记录）")
                return@execute
            }
            onLyric(lyric, "酷我")
        }
    }

    private fun looksLikeLyric(text: String): Boolean {
        val length = text.length
        if (length < 64 || length > maxPayload) return false
        if (!text.contains('[') && !text.contains("lyric", true) && !text.contains("lrc", true)) return false
        return LRC_LIKE.containsMatchIn(text) || QRC_LIKE.containsMatchIn(text) ||
            text.contains("LyricContent") || text.contains("tlyric") ||
            text.contains("lang_translations")
    }

    private fun submitString(text: String) {
        if (!mark(text)) return
        executor.execute {
            val lyric = runCatching { LyricParsers.parseAnyPayload(text) }.getOrNull()
            if (lyric == null || !LyricParsers.isUsable(lyric.lines)) {

                logger.info("疑似歌词但解析失败（${text.length} 字符，内容不记录）")
                return@execute
            }
            onLyric(lyric, "网络")
        }
    }
    private fun submitBytes(bytes: ByteArray) {
        val key = bytes.size.toString() + ':' + bytes.take(64).joinToString(",")
        if (!mark(key)) return
        executor.execute {
            val lyric = runCatching { LyricParsers.parseAnyBytes(bytes) }.getOrNull() ?: return@execute
            if (!LyricParsers.isUsable(lyric.lines)) return@execute
            onLyric(lyric, "网络")
        }
    }

    private fun mark(payload: String): Boolean {
        val sum = runCatching {
            MessageDigest.getInstance("MD5")
                .digest(payload.take(4096).toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.getOrElse { payload.take(256) }
        synchronized(seen) {
            if (seen.contains(sum)) return false
            if (seen.size > 256) seen.clear()
            seen.add(sum)
        }
        return true
    }

    private fun callLong(target: Any?, name: String): Long {
        if (target == null) return -1L
        return runCatching {
            target.javaClass.getMethod(name).invoke(target) as? Long ?: -1L
        }.getOrDefault(-1L)
    }

    private fun callString(target: Any?, name: String): String {
        if (target == null) return "?"
        return runCatching {
            target.javaClass.getMethod(name).invoke(target)?.toString() ?: "?"
        }.getOrDefault("?")
    }

    private companion object {
        private val LRC_LIKE = Regex("""\[\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?]""")
        private val QRC_LIKE = Regex("""\[\d{2,7},\d{2,7}]""")

        fun hex(bytes: ByteArray, max: Int): String {
            val n = minOf(bytes.size, max)
            val sb = StringBuilder(n * 2)
            for (i in 0 until n) sb.append("%02x".format(bytes[i]))
            return sb.toString()
        }

        fun hexPreview(result: Any?): String = when (result) {
            is ByteArray -> hex(result, 16)
            is String -> result.take(24)
            else -> "?"
        }
    }
}
