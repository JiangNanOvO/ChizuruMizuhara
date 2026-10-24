package com.astraflow.Chizuru.lyric

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class KugouLyricLoader(
    private val module: XposedModule,
    private val logger: ModuleLogger,
    private val classLoader: ClassLoader,
    private val onLyric: (LocalLyric, String) -> Unit
) {

    private val hooked = AtomicBoolean(false)

    private val candidateBudget = AtomicInteger(8)
    private val triedMethods = ConcurrentHashMap.newKeySet<String>()

    fun install() {
        tryHookDirect()
        if (hooked.get()) return
        hookClassLoadFallback()
    }

    private fun tryHookDirect() {
        val clazz = runCatching {
            Class.forName(LYRIC_MANAGER, false, classLoader)
        }.getOrNull() ?: return
        hookMethods(clazz)
    }

    private fun hookClassLoadFallback() {
        val loadClass = runCatching {
            ClassLoader::class.java.getDeclaredMethod("loadClass", String::class.java)
        }.getOrNull() ?: return
        runCatching {
            module.hook(loadClass)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    if (!hooked.get()) {
                        val name = chain.args.getOrNull(0) as? String
                        if (name == LYRIC_MANAGER) {
                            (result as? Class<*>)?.let { hookMethods(it) }
                        }
                    }
                    result
                }
            logger.debug("酷狗歌词直挂：等待 $LYRIC_MANAGER 加载")
        }.onFailure { logger.debug("酷狗歌词直挂 fallback 失败：${it.message}") }
    }

    private fun hookMethods(clazz: Class<*>) {
        if (!hooked.compareAndSet(false, true)) return
        var installed = 0
        for (method in clazz.declaredMethods) {
            val types = method.parameterTypes

            if (types.size != 2 || types[0] != String::class.java ||
                types[1] != java.lang.Boolean.TYPE
            ) continue
            method.isAccessible = true
            installHook(method)
            installed++
        }
        if (installed == 0) {
            hooked.set(false)
            logger.debug("酷狗歌词直挂：$LYRIC_MANAGER 里没有 (String,boolean) 方法")
            return
        }
        logger.info("酷狗歌词直挂：$LYRIC_MANAGER 挂载 $installed 个候选方法")
    }

    private fun installHook(method: java.lang.reflect.Method) {
        val key = method.name + "|" + method.declaringClass.name
        runCatching {
            module.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    val path = chain.args.getOrNull(0) as? String
                    if (path != null && candidateBudget.get() > 0) {
                        handlePath(path, key)
                    }
                    result
                }
        }.onFailure { logger.debug("酷狗歌词直挂挂载失败 $key：${it.message}") }
    }

    private fun handlePath(path: String, key: String) {
        val lower = path.lowercase()
        if (!lower.endsWith(".krc") && !lower.endsWith(".lrc") &&
            !lower.endsWith(".lyc") && !lower.endsWith(".txt")
        ) return
        if (!triedMethods.add(key)) return
        val file = File(path)
        if (!file.isFile || file.length() <= 0L || file.length() > MAX_LYRIC_BYTES) {
            markBadCandidate(key)
            return
        }
        Thread({
            val lyric = runCatching {
                val bytes = file.readBytes()
                when {
                    lower.endsWith(".krc") -> LyricParsers.parseKrc(bytes)
                    lower.endsWith(".lyc") -> LyricParsers.parseKrc(bytes)
                        ?: LyricParsers.parseLrcText(runCatching {
                            bytes.toString(Charsets.UTF_8)
                        }.getOrDefault(""))
                    else -> LyricParsers.parseLrcText(
                        runCatching { bytes.toString(Charsets.UTF_8) }.getOrDefault("")
                    )
                }
            }.getOrNull()
            if (lyric != null && LyricParsers.isUsable(lyric.lines)) {
                logger.info(
                    "酷狗直挂命中：$key ← ${file.name}，${lyric.lines.size} 行" +
                        (if (lyric.title != null) " ti=${lyric.title}" else "")
                )
                onLyric(lyric, "酷狗·" + file.name)
            } else {
                markBadCandidate(key)
            }
        }, "mitian-kg-load").apply { isDaemon = true }.start()
    }

    private fun markBadCandidate(key: String) {
        candidateBudget.decrementAndGet()
        logger.debug("酷狗直挂候选作废：$key（路径解不出歌词）")
    }

    private companion object {
        private const val LYRIC_MANAGER = "com.kugou.framework.lyric.LyricManager"
        private const val MAX_LYRIC_BYTES = 2L * 1024 * 1024
    }
}
