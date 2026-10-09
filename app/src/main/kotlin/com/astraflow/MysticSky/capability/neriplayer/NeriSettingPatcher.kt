package com.astraflow.MysticSky.capability.neriplayer

import android.content.Context
import com.astraflow.MysticSky.capability.root.RootShell

object NeriSettingPatcher {

    const val PACKAGE = "moe.ouom.neriplayer"

    private val DATA_STORE_CANDIDATES = listOf(
        "/data/user/0/$PACKAGE/files/datastore/settings.preferences_pb",
        "/data/data/$PACKAGE/files/datastore/settings.preferences_pb",
        "/data_mirror/data_ce/null/0/$PACKAGE/files/datastore/settings.preferences_pb"
    )
    private const val KEY = "lyricon_enabled"
    private const val BACKUP = "/data/local/tmp/mitian_neri_settings.bak"

    private const val TAG = "MysticSky-Neri"

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

    fun ensureEnabled(context: Context): Boolean {
        val hasRoot = RootShell.available()
        log(context, "音理音理适配：root=$hasRoot")
        if (!hasRoot) return false

        val found = DATA_STORE_CANDIDATES.firstOrNull { RootShell.exists(it) }
        if (found == null) {
            val probe = RootShell.exec(
                "ls -d /data/data /data/user/0 /data_mirror/data_ce/null/0 2>&1; ls -l /data/user/0/moe.ouom.neriplayer/files/datastore 2>&1"
            )
            log(context, "音理音理：找不到它的设置文件。探测=" + probe?.replace("\n", " | ")?.take(200))
            return false
        }
        val dataStore = found
        log(context, "音理音理：使用路径 $dataStore")

        val original = RootShell.readBytes(dataStore)
        if (original == null) {
            log(context, "音理音理：读不到它的设置文件（没装或没权限）")
            return false
        }
        log(context, "音理音理：设置文件 ${original.size} 字节")
        when (readState(original)) {
            true -> {
                log(context, "音理音理：它的外部歌词输出已经是开的")
                return true
            }
            null -> {
                log(context, "音理音理：文件里没解析到 $KEY")
                return false
            }
            false -> Unit
        }

        val patched = patch(original)
        if (patched == null) {
            log(context, "音理音理：结构不符合预期，放弃（不动它的文件）")
            return false
        }
        RootShell.exec("cp '$dataStore' '$BACKUP'")
        if (!RootShell.writeFile(dataStore, patched)) {
            log(context, "音理音理：写回失败")
            return false
        }
        val verify = RootShell.readBytes(dataStore)
        val ok = verify != null && readState(verify) == true
        log(context, if (ok) "音理音理：已自动打开它的外部歌词输出（已备份）"
                       else "音理音理：写入后校验失败")
        return ok
    }

    private fun readState(data: ByteArray): Boolean? {
        val at = indexOf(data, KEY.toByteArray())
        if (at < 0) return null
        val v = findValueAfter(data, at + KEY.length) ?: return null
        if (v.size < 2 || v[0] != 0x08.toByte()) return null
        return when (v[1]) {
            0x00.toByte() -> false
            0x01.toByte() -> true
            else -> null
        }
    }

    private fun patch(data: ByteArray): ByteArray? {
        val at = indexOf(data, KEY.toByteArray())
        if (at < 0) return null
        val valueStart = findValuePayload(data, at + KEY.length) ?: return null
        if (data[valueStart] != 0x08.toByte()) return null
        val out = data.copyOf()
        out[valueStart + 1] = 0x01
        return out
    }

    private fun findValueAfter(data: ByteArray, from: Int): ByteArray? {
        val payload = findValuePayload(data, from) ?: return null
        val len = data[payload - 1].toInt() and 0xFF
        if (payload + len > data.size) return null
        return data.copyOfRange(payload, payload + len)
    }

    private fun findValuePayload(data: ByteArray, from: Int): Int? {
        if (from + 1 >= data.size) return null
        if (data[from] != 0x12.toByte()) return null
        val len = data[from + 1].toInt() and 0xFF
        if (from + 2 + len > data.size) return null
        if (len < 2) return null
        return from + 2
    }

    private fun indexOf(data: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..data.size - needle.size) {
            for (j in needle.indices) {
                if (data[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
