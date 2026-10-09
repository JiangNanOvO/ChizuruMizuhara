package com.astraflow.MysticSky.lyric

import android.util.Log
import io.github.libxposed.api.XposedModule
import java.io.File

class ModuleLogger(
    private val module: XposedModule,
    private val tag: String = "Mitian"
) {
    private val fmt = java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS", java.util.Locale.US)
    private var sinkCache: File? = null
    private var sinkResolved = false

    private fun candidates(): List<File> {
        val list = ArrayList<File>(4)
        runCatching { appContext()?.filesDir?.let { list.add(File(it, LOG_NAME)) } }
        val pkg = packageName
        if (pkg != null) {
            list.add(File("/data/user/0/$pkg/files/$LOG_NAME"))
            list.add(File("/data/data/$pkg/files/$LOG_NAME"))
            list.add(File("/sdcard/Android/data/$pkg/files/$LOG_NAME"))
        }
        return list
    }

    private fun sink(): File? {
        sinkCache?.let { return it }
        if (sinkResolved) return null
        var found: File? = null
        for (file in candidates()) {
            val ok = runCatching {

                file.parentFile?.mkdirs()
                file.appendText("")
                true
            }.getOrDefault(false)
            if (ok) { found = file; break }
        }
        sinkCache = found

        
        sinkResolved = true
        return found
    }

    private fun appContext(): android.content.Context? = runCatching {
        Class.forName("android.app.ActivityThread")
            .getDeclaredMethod("currentApplication").invoke(null) as? android.content.Context
    }.getOrNull()

    private val packageName: String? by lazy { currentPackage() }

    private fun currentPackage(): String? {
        val raw = runCatching {
            File("/proc/self/cmdline").readText().trimEnd('\u0000').trim()
        }.getOrNull()
        val pkg = raw?.substringBefore(':')?.takeIf { it.isNotBlank() } ?: return null
        return pkg
    }

    private val buffer = StringBuilder(4096)
    private val bufferLock = Any()

    private val FLUSH_THRESHOLD = 8_000

    private fun toFile(level: String, message: String) {
        val file = sink() ?: return
        val line = fmt.format(java.util.Date()) + " " + level + " " + message + "\n"
        val flushNow: Boolean
        synchronized(bufferLock) {
            buffer.append(line)
            flushNow = buffer.length >= FLUSH_THRESHOLD
        }
        if (!flushNow) return
        flush(file)
    }

    private fun flush(file: File) {
        val payload: String
        synchronized(bufferLock) {
            if (buffer.isEmpty()) return
            payload = buffer.toString()
            buffer.setLength(0)
        }
        runCatching {
            if (file.length() > 2_000_000L) file.delete()
            file.appendText(payload)
        }
    }

    fun debug(message: String) {
        module.log(Log.DEBUG, tag, message)
        toFile("D", message)
    }
    fun info(message: String) {
        module.log(Log.INFO, tag, message)
        toFile("I", message)
    }
    fun warn(message: String) {
        module.log(Log.WARN, tag, message)
        toFile("W", message)
    }
    fun error(message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            module.log(Log.ERROR, tag, message)
        } else {
            module.log(Log.ERROR, tag, message, throwable)
        }
        toFile("E", message + (throwable?.let { " | " + it } ?: ""))
    }

    private companion object {
        const val LOG_NAME = "lyricon_debug.log"
    }
}
