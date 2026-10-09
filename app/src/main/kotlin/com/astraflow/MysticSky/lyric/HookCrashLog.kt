package com.astraflow.MysticSky.lyric

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object HookCrashLog {

    private const val LOG_NAME = "mitian_hook_err.log"
    private const val MAX_BYTES = 512_000L

    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var sink: File? = null

    @Volatile
    private var sinkResolved = false

    private fun resolveSink(): File? {
        sink?.let { return it }
        if (sinkResolved) return null

        

        val candidates = listOf(
            File("/data/local/tmp/$LOG_NAME"),
        )
        for (file in candidates) {
            val ok = runCatching {

                file.parentFile?.mkdirs()
                file.appendText("")
                true
            }.getOrDefault(false)
            if (ok) {
                sink = file
                sinkResolved = true
                return file
            }
        }
        sinkResolved = true
        return null
    }

    fun record(where: String, throwable: Throwable) {
        val file = resolveSink() ?: return
        runCatching {
            if (file.length() > MAX_BYTES) file.delete()
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            file.appendText(
                "==== " + fmt.format(Date()) + " ====\n" +
                    "process=" + processName() + " uid=" + android.os.Process.myUid() + "\n" +
                    "where=" + where + "\n" +
                    sw.toString() + "\n"
            )
        }
    }

    private fun processName(): String = runCatching {
        File("/proc/self/cmdline").readText().trimEnd('\u0000').trim()
    }.getOrDefault("?")
}
