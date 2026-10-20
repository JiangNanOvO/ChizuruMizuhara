package com.astraflow.Chizuru.capability.link

import android.content.ClipData
import android.content.Context
import android.os.Bundle
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import com.astraflow.Chizuru.redactUrl
import com.astraflow.Chizuru.settings.ModulePrefs

internal class LinkHook(
    private val module: XposedModule,
    private val classLoader: ClassLoader
) {

    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "mitian-linkhook").apply { isDaemon = true }
    }

    @Volatile
    private var sysContext: Context? = null

    @Volatile
    private var lastKey: String? = null
    @Volatile
    private var lastAt = 0L

    fun install(): Boolean {
        val cls = runCatching {
            Class.forName(CLIPBOARD_SERVICE, false, classLoader)
        }.getOrNull() ?: run {
            logW("找不到 $CLIPBOARD_SERVICE")
            return false
        }

        
        val methods = cls.declaredMethods.filter { it.name == "setPrimaryClipInternalLocked" }
        if (methods.isEmpty()) {
            logW("ClipboardService 没有 setPrimaryClipInternalLocked")
            return false
        }

        var hooked = 0
        for (m in methods) {
            runCatching {
                module.hook(m)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        onClipSet(chain.thisObject, chain.args)
                        chain.proceed()
                    }
            }.onSuccess { hooked++ }
        }

        if (hooked > 0) logI("链接助手已挂接（${hooked} 个重载）")
        else logE("所有重载均挂不上")
        return hooked > 0
    }

    private fun onClipSet(service: Any?, args: List<Any?>) {
        try {
            if (!linksAllowed()) return
            val clip = args.getOrNull(0) as? ClipData ?: return
            if (clip.itemCount == 0) return
            val text = runCatching {
                clip.getItemAt(0)?.coerceToText(null)?.toString()
            }.getOrNull() ?: return
            if (text.isBlank()) return

            val intent = ClipIntents.classify(text) ?: return
            if (!kindAllowed(intent.kind)) return

            val key = intent.kind.wire + "|" + intent.payload
            val now = android.os.SystemClock.elapsedRealtime()
            if (key == lastKey && now - lastAt < DEDUP_MS) return
            lastKey = key
            lastAt = now

            worker.execute { push(service, intent) }
        } catch (_: Throwable) {

        }
    }

    private fun linksAllowed(): Boolean = runCatching {
        val prefs = module.getRemotePreferences(ModulePrefs.NAME)
        prefs.getBoolean(ModulePrefs.KEY_ENABLED, true) &&
            prefs.getBoolean(ModulePrefs.KEY_LINK, true)
    }.getOrDefault(true)

    private fun kindAllowed(kind: ClipKind): Boolean = runCatching {
        val prefs = module.getRemotePreferences(ModulePrefs.NAME)
        if (!prefs.getBoolean(ModulePrefs.KEY_ENABLED, true)) return@runCatching false
        if (!prefs.getBoolean(ModulePrefs.KEY_LINK, true)) return@runCatching false
        when (kind) {
            ClipKind.LINK -> prefs.getBoolean(ModulePrefs.KEY_CLIP_LINK, true)
            ClipKind.PHONE -> prefs.getBoolean(ModulePrefs.KEY_CLIP_PHONE, true)
            ClipKind.ADDRESS -> prefs.getBoolean(ModulePrefs.KEY_CLIP_ADDRESS, true)
        }
    }.getOrDefault(false)

    private fun push(service: Any?, intent: ClipIntent) {
        val ctx = sysContext ?: resolveContext(service).also { sysContext = it }
        if (ctx == null) {
            logW("拿不到系统 Context，本条丢弃")
            return
        }
        runCatching {
            ctx.contentResolver.call(
                android.net.Uri.parse("content://" + AUTHORITY),
                METHOD_DELIVER,
                intent.payload,
                Bundle().apply {
                    putString(EXTRA_KIND, KIND_CLIPBOARD)
                    putString(EXTRA_CLIP_KIND, intent.kind.wire)
                }
            )
            logI("剪贴板意图已投递（${intent.kind.wire}）：${redactPayload(intent)}")
        }.onFailure { logE("投递失败", it) }
    }

    private fun resolveContext(service: Any?): Context? {
        if (service == null) return null

        var cls: Class<*>? = service.javaClass
        while (cls != null) {
            val found = runCatching {
                cls!!.getDeclaredField("mContext")
                    .also { it.isAccessible = true }
                    .get(service) as? Context
            }.getOrNull()
            if (found != null) {
                logI("已从 ${cls!!.simpleName}.mContext 拿到 Context")
                return found
            }
            cls = cls.superclass
        }
        logW("在 ${service.javaClass.name} 里找不到 mContext")
        return null
    }

    private fun redactPayload(intent: ClipIntent): String = when (intent.kind) {
        ClipKind.LINK -> redactUrl(intent.payload)
        ClipKind.PHONE -> intent.payload.take(3) + "****" + intent.payload.takeLast(2)
        ClipKind.ADDRESS -> intent.payload.take(6) + "…"
    }

    private fun logI(msg: String) = module.log(Log.INFO, TAG, msg)
    private fun logW(msg: String) = module.log(Log.WARN, TAG, msg)
    private fun logE(msg: String, t: Throwable? = null) {
        if (t != null) module.log(Log.ERROR, TAG, msg, t)
        else module.log(Log.ERROR, TAG, msg)
    }

    private companion object {
        const val CLIPBOARD_SERVICE = "com.android.server.clipboard.ClipboardService"
        const val AUTHORITY = "com.astraflow.Chizuru.inbox"
        const val METHOD_DELIVER = "deliver"
        const val EXTRA_KIND = "kind"
        const val KIND_CLIPBOARD = "clipboard"
        const val EXTRA_CLIP_KIND = "clipKind"
        const val DEDUP_MS = 1_000L
        const val TAG = "Mitian"
    }
}
