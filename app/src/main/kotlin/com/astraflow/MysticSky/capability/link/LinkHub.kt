package com.astraflow.MysticSky.capability.link

import android.content.Context
import android.util.Log
import com.astraisland.sdk.BuiltinSymbol
import com.astraisland.sdk.Capsule
import com.astraisland.sdk.CapsuleTrailing
import com.astraisland.sdk.CardTag
import com.astraisland.sdk.CardValue
import com.astraisland.sdk.DismissPolicy
import com.astraisland.sdk.EndReason
import com.astraisland.sdk.GenericCard
import com.astraisland.sdk.IslandActivity
import com.astraisland.sdk.IslandCallback
import com.astraisland.sdk.IslandClient
import com.astraisland.sdk.IslandImage
import com.astraisland.sdk.IslandResult
import com.astraisland.sdk.Priority
import com.astraisland.sdk.TextButton
import com.astraflow.MysticSky.settings.ModulePrefs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import com.astraflow.MysticSky.redactUrl

object LinkHub {

    private const val TAG = "Mitian-LinkHub"

    const val CARD_ID = "mitian-link"

    private const val ACTION_DISMISS = "dismiss"

    private const val CARD_TITLE = "发现链接"
    private const val CARD_LABEL = "链接"
    private const val CARD_VALUE = "打开"
    private const val CARD_TAG_OPEN = "点卡片即打开"
    private const val CARD_TAG_QUIET = "点卡片打开链接"

    private const val ACCENT = 0xFF2F6BFF.toInt()

    private const val CARD_LIFETIME_MS = 3 * 60_000L

    private const val STALE_MS = 10 * 60_000L

    private val RETRY_DELAYS_MS = longArrayOf(300L, 500L, 800L, 1200L, 1800L, 2500L, 3000L)

    private const val AWAIT_TIMEOUT_MS = 5_000L

    private const val POSTED = 0
    private const val FAILED = 1
    private const val QUEUED = -1

    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread(r, "mitian-hub").apply { isDaemon = true }
    }

    @Volatile private var client: IslandClient? = null
    @Volatile private var appContext: Context? = null

    @Volatile private var currentUrl: String? = null

    @Volatile private var pendingUrl: String? = null
    @Volatile private var retryIndex = 0

    fun postBlocking(app: Context, url: String, timeoutMs: Long = AWAIT_TIMEOUT_MS): Boolean {
        appContext = app.applicationContext
        pendingUrl = url
        retryIndex = 0
        persistPending(url)

        val latch = CountDownLatch(1)
        val outcome = AtomicInteger(QUEUED)
        worker.execute {
            outcome.set(show(url))
            latch.countDown()
        }
        return runCatching {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            outcome.get() != FAILED
        }.getOrDefault(false)
    }

    fun warmUp(app: Context) {
        appContext = app.applicationContext
        worker.execute { ensureClient() }
    }

    fun isReady(): Boolean = client?.isReady == true

    fun stateName(): String = client?.state?.name ?: "NOT_CONNECTED"

    fun sdkVersion(): String = IslandClient.SDK_VERSION

    fun dismiss() {
        currentUrl = null
        worker.execute { runCatching { client?.end(CARD_ID) } }
    }

    private fun ensureClient(): IslandClient? {
        client?.let { return it }
        val ctx = appContext ?: return null
        return synchronized(this) {
            client ?: runCatching {
                IslandClient(ctx).apply {
                    setCallback(callback)
                    connect()
                }
            }.onFailure { Log.e(TAG, "IslandClient 创建失败", it) }
                .getOrNull()
                .also { client = it }
        }
    }

    private val callback = object : IslandCallback() {

        override fun onStateChanged(state: IslandClient.State) {
            Log.i(TAG, "星河岛连接状态：$state")

            if (state == IslandClient.State.READY) worker.execute { flushPending() }
        }

        override fun onAction(activityId: String, actionId: String) {
            if (activityId != CARD_ID) return
            Log.i(TAG, "按钮：$actionId")

            if (actionId == ACTION_DISMISS) dismiss()
        }

        override fun onDismissed(activityId: String) {
            if (activityId == CARD_ID) currentUrl = null
        }

        override fun onEnded(activityId: String, reason: EndReason) {
            if (activityId == CARD_ID) {
                currentUrl = null
                Log.i(TAG, "卡片已结束：$reason")
            }
        }
    }

    private fun show(url: String): Int {
        val ctx = appContext ?: return FAILED
        val island = ensureClient() ?: run {
            Log.w(TAG, "接入库不可用，稍后重试")
            scheduleRetry(url)
            return QUEUED
        }
        if (!island.isReady) {
            runCatching { island.connect() }
            scheduleRetry(url)
            return QUEUED
        }

        val activity = runCatching { buildCard(ctx, url) }
            .onFailure { Log.e(TAG, "卡片编码失败", it) }
            .getOrNull()
        if (activity == null) {
            pendingUrl = null
            clearPending()
            return FAILED
        }

        val result = runCatching { island.start(activity) }
            .onFailure { Log.e(TAG, "start 抛异常", it) }
            .getOrDefault(IslandResult.INVALID)
        Log.i(TAG, "start -> $result  url=${redactUrl(url)}")

        return when {
            result.accepted -> {
                currentUrl = url
                pendingUrl = null
                retryIndex = 0
                clearPending()
                POSTED
            }

            result == IslandResult.NOT_CONNECTED || result == IslandResult.BUSY -> {
                runCatching { island.connect() }
                scheduleRetry(url)
                QUEUED
            }

            else -> {
                pendingUrl = null
                retryIndex = 0
                clearPending()
                FAILED
            }
        }
    }

    private fun scheduleRetry(url: String) {
        if (retryIndex >= RETRY_DELAYS_MS.size) {
            Log.w(TAG, "放弃投递（重试 ${retryIndex} 次仍未连上）：${redactUrl(url)}")
            retryIndex = 0
            return
        }
        val delay = RETRY_DELAYS_MS[retryIndex++]
        worker.execute {
            Thread.sleep(delay)
            if (pendingUrl == url) show(url)
        }
    }

    private fun flushPending() {
        val url = pendingUrl ?: run {
            val ctx = appContext ?: return
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY_URL, null) ?: return
            val at = prefs.getLong(KEY_AT, 0L)
            if (System.currentTimeMillis() - at > STALE_MS) {
                prefs.edit().remove(KEY_URL).remove(KEY_AT).apply()
                return
            }
            stored
        }
        show(url)
    }

    private fun buildCard(context: Context, url: String): IslandActivity {
        val prefs = ModulePrefs.of(context)
        val quiet = ModulePrefs.isLinkHintOnly(prefs)
        val host = LinkPatterns.hostOf(url)

        val capsule = Capsule.Builder(IslandImage.symbol(BuiltinSymbol.NAVIGATION))
            .setLabel(CARD_LABEL)
            .setTrailing(CapsuleTrailing.text(host))
            .build()

        val card = GenericCard.Builder(CARD_TITLE)
            .setSubtitle(host)
            .setBody(url)
            .setValue(CardValue.text(CARD_VALUE))
            .setTag(CardTag(if (quiet) CARD_TAG_QUIET else CARD_TAG_OPEN))
            .addButton(TextButton(ACTION_DISMISS, "忽略"))
            .build()

        return IslandActivity.Builder(CARD_ID, capsule, card)
            .setPriority(Priority.HIGH)
            .setAlertOnStart(!quiet)
            .setAlertOnUpdate(false)
            .setOpenIntent(LinkLauncher.openIntent(context, url))
            .setPostedAt(System.currentTimeMillis())
            .setStaleAt(System.currentTimeMillis() + STALE_MS)
            .setDismissPolicy(DismissPolicy.afterMillis(CARD_LIFETIME_MS))
            .setAccentColor(ACCENT)
            .setContentDescription("发现链接 $host，点按卡片打开")
            .build()
    }

    private fun persistPending(url: String) {
        val ctx = appContext ?: return
        runCatching {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_URL, url)
                .putLong(KEY_AT, System.currentTimeMillis())
                .apply()
        }
    }

    private fun clearPending() {
        val ctx = appContext ?: return
        runCatching {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(KEY_URL).remove(KEY_AT).apply()
        }
    }

    private const val PREFS = "mitian_link_pending"
    private const val KEY_URL = "url"
    private const val KEY_AT = "at"
}
