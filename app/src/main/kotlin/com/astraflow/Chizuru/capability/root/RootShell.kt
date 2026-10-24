package com.astraflow.Chizuru.capability.root

import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object RootShell {

    private const val TAG = "Chizuru-Root"

    @Volatile private var state: Boolean? = null

    fun available(): Boolean {
        state?.let { return it }
        val ok = runCatching {
            exec("id", timeoutMs = 60_000)?.contains("uid=0") == true
        }.getOrDefault(false)
        state = ok
        Log.i(TAG, "root available = $ok")
        return ok
    }

    fun forget() {
        state = null
    }

    fun exec(command: String, timeoutMs: Long = 15_000): String? = runCatching {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(true)
            .start()
        val out = StringBuilder()
        val reader = Thread {
            runCatching {
                process.inputStream.bufferedReader().forEachLine { out.appendLine(it) }
            }
        }
        reader.isDaemon = true
        reader.start()
        if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            process.destroy()
            Log.w(TAG, "命令超时：${command.take(60)}")
            return@runCatching null
        }
        reader.join(1000)
        out.toString()
    }.getOrNull()

    fun readBytes(path: String, timeoutMs: Long = 10_000): ByteArray? = runCatching {
        val process = ProcessBuilder("su", "-c", "cat '$path'")
            .redirectErrorStream(false)
            .start()
        val buffer = ByteArrayOutputStream()
        val copier = Thread {
            runCatching {
                val input = process.inputStream
                val chunk = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(chunk)
                    if (n <= 0) break
                    buffer.write(chunk, 0, n)
                }
            }
        }
        copier.isDaemon = true
        copier.start()
        if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            process.destroy()
            return@runCatching null
        }
        copier.join(1000)
        if (process.exitValue() != 0) return@runCatching null
        buffer.toByteArray().takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun listNewest(path: String, limit: Int = 8): List<String> =
        exec("ls -t '$path' 2>/dev/null | head -$limit")
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toList()
            ?: emptyList()

    fun writeFile(path: String, bytes: ByteArray, timeoutMs: Long = 15_000): Boolean {
        val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val tmp = "/data/local/tmp/mitian_patch.b64"
        val ok = exec("echo '$encoded' > $tmp && base64 -d $tmp > '$path' && rm -f $tmp && echo DONE",
            timeoutMs = timeoutMs)
        return ok?.contains("DONE") == true
    }

    fun exists(path: String): Boolean = exec("[ -e '$path' ] && echo yes")?.contains("yes") == true
}
