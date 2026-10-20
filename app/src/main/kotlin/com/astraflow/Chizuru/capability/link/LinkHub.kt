package com.astraflow.Chizuru.capability.link

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.astraflow.Chizuru.settings.ModulePrefs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object LinkHub {

    private const val TAG = "Mitian-LinkHub"

    const val CARD_ID = "mitian-link"

    private const val ACTION_DISMISS = "dismiss"
    private const val ACTION_DIAL = "dial"
    private const val ACTION_SMS = "sms"
    private const val ACTION_DIAL_MODE = "dialmode"

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

    @Volatile private var currentIntent: ClipIntent? = null

    @Volatile private var pendingIntent: ClipIntent? = null
    @Volatile private var retryIndex = 0

    fun postBlocking(app: Context, intent: ClipIntent, timeoutMs: Long = AWAIT_TIMEOUT_MS): Boolean {
        appContext = app.applicationContext
        pendingIntent = intent
        retryIndex = 0
        persistPending(intent)

        val latch = CountDownLatch(1)
        val outcome = AtomicInteger(QUEUED)
        worker.execute {
            outcome.set(show(intent))
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
        currentIntent = null
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

            when (actionId) {
                ACTION_DISMISS -> dismiss()
                ACTION_DIAL -> dial(lastShown()?.payload)
                ACTION_SMS -> switchPhoneMode(lastShown()?.payload, sms = true)
                ACTION_DIAL_MODE -> switchPhoneMode(lastShown()?.payload, sms = false)
            }
        }

        override fun onDismissed(activityId: String) {
            if (activityId == CARD_ID) currentIntent = null
        }

        override fun onEnded(activityId: String, reason: EndReason) {
            if (activityId == CARD_ID) {
                currentIntent = null
                Log.i(TAG, "卡片已结束：$reason")
            }
        }
    }

    private fun show(intent: ClipIntent): Int {
        val ctx = appContext ?: return FAILED
        val island = ensureClient() ?: run {
            Log.w(TAG, "接入库不可用，稍后重试")
            scheduleRetry(intent)
            return QUEUED
        }
        if (!island.isReady) {
            runCatching { island.connect() }
            scheduleRetry(intent)
            return QUEUED
        }

        val activity = runCatching { buildCard(ctx, intent) }
            .onFailure { Log.e(TAG, "卡片编码失败", it) }
            .getOrNull()
        if (activity == null) {
            pendingIntent = null
            clearPending()
            return FAILED
        }

        val result = runCatching { island.start(activity) }
            .onFailure { Log.e(TAG, "start 抛异常", it) }
            .getOrDefault(IslandResult.INVALID)
        Log.i(TAG, "start -> $result  kind=${intent.kind.wire} payload=${describe(intent)}")

        return when {
            result.accepted -> {
                currentIntent = intent
                rememberShown(intent)
                pendingIntent = null
                retryIndex = 0
                clearPending()
                POSTED
            }

            result == IslandResult.NOT_CONNECTED || result == IslandResult.BUSY -> {
                runCatching { island.connect() }
                scheduleRetry(intent)
                QUEUED
            }

            else -> {
                pendingIntent = null
                retryIndex = 0
                clearPending()
                FAILED
            }
        }
    }

    private fun scheduleRetry(intent: ClipIntent) {
        if (retryIndex >= RETRY_DELAYS_MS.size) {
            Log.w(TAG, "放弃投递（重试 ${retryIndex} 次仍未连上）：${describe(intent)}")
            retryIndex = 0
            return
        }
        val delay = RETRY_DELAYS_MS[retryIndex++]
        worker.execute {
            Thread.sleep(delay)
            if (pendingIntent == intent) show(intent)
        }
    }

    private fun flushPending() {
        val intent = pendingIntent ?: run {
            val ctx = appContext ?: return
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val payload = prefs.getString(KEY_PAYLOAD, null) ?: return
            val at = prefs.getLong(KEY_AT, 0L)
            if (System.currentTimeMillis() - at > STALE_MS) {
                prefs.edit().remove(KEY_PAYLOAD).remove(KEY_KIND).remove(KEY_AT).apply()
                return
            }
            ClipIntent(ClipKind.of(prefs.getString(KEY_KIND, null)), payload)
        }
        show(intent)
    }

    private fun describe(intent: ClipIntent): String = when (intent.kind) {
        ClipKind.LINK -> runCatching { Uri.parse(intent.payload).host ?: intent.payload }.getOrDefault(intent.payload)
        ClipKind.PHONE -> intent.payload.take(3) + "****" + intent.payload.takeLast(2)
        ClipKind.ADDRESS -> intent.payload.take(8) + "…"
    }

    private fun buildCard(context: Context, intent: ClipIntent, smsMode: Boolean = false): IslandActivity {
        val prefs = ModulePrefs.of(context)
        val quiet = ModulePrefs.isLinkHintOnly(prefs)
        val summary = ClipIntents.summaryOf(intent)

        val symbol = when (intent.kind) {
            ClipKind.LINK -> BuiltinSymbol.NAVIGATION
            ClipKind.PHONE -> BuiltinSymbol.MESSAGE
            ClipKind.ADDRESS -> BuiltinSymbol.NAVIGATION
        }
        val label = when (intent.kind) {
            ClipKind.LINK -> "链接"
            ClipKind.PHONE -> "电话"
            ClipKind.ADDRESS -> "地址"
        }
        val title = when (intent.kind) {
            ClipKind.LINK -> "发现链接"
            ClipKind.PHONE -> "发现号码"
            ClipKind.ADDRESS -> "发现地址"
        }
        val value = when (intent.kind) {
            ClipKind.LINK -> "打开"
            ClipKind.PHONE -> intent.payload
            ClipKind.ADDRESS -> "地图"
        }
        val tag = when (intent.kind) {
            ClipKind.LINK -> if (quiet) "点卡片打开链接" else "点卡片即打开"
            ClipKind.PHONE -> if (smsMode) "点卡片发短信" else "点卡片拨号"
            ClipKind.ADDRESS -> "点卡片看地图"
        }
        val description = when (intent.kind) {
            ClipKind.LINK -> "发现链接 $summary，点按卡片打开"
            ClipKind.PHONE -> if (smsMode) "发现号码，点按卡片发短信" else "发现号码，点按卡片拨号"
            ClipKind.ADDRESS -> "发现地址 $summary，点按卡片打开地图"
        }

        val capsule = Capsule.Builder(IslandImage.symbol(symbol))
            .setLabel(label)
            .setTrailing(CapsuleTrailing.text(summary))
            .build()

        val subtitle = if (summary == intent.payload) null else summary
        val bodyText = if (intent.kind == ClipKind.PHONE) null else intent.payload
        val card = GenericCard.Builder(title)
            .setSubtitle(subtitle)
            .setBody(bodyText)
            .setValue(CardValue.text(value))
            .setTag(CardTag(tag))
            .apply {
                if (intent.kind == ClipKind.PHONE) {
                    addButton(TextButton(ACTION_DISMISS, "忽略"))
                    addButton(TextButton(if (smsMode) ACTION_DIAL_MODE else ACTION_SMS, if (smsMode) "拨号" else "发短信"))
                } else {
                    addButton(TextButton(ACTION_DISMISS, "忽略"))
                }
            }
            .build()

        val openIntent: PendingIntent? = when (intent.kind) {
            ClipKind.LINK -> LinkLauncher.openIntent(context, intent.payload)
            ClipKind.PHONE -> if (smsMode) smsIntent(context, intent.payload) else phoneIntent(context, intent.payload)
            ClipKind.ADDRESS -> MapChoice.openIntent(context, intent.payload)
        }

        val builder = IslandActivity.Builder(CARD_ID, capsule, card)
            .setPriority(Priority.HIGH)
            .setAlertOnStart(!quiet)
            .setAlertOnUpdate(false)
            .setPostedAt(System.currentTimeMillis())
            .setStaleAt(System.currentTimeMillis() + STALE_MS)
            .setDismissPolicy(DismissPolicy.afterMillis(CARD_LIFETIME_MS))
            .setContentDescription(description)
        if (openIntent != null) builder.setOpenIntent(openIntent)
        return builder.build()
    }

    private fun dial(number: String?) {
        val ctx = appContext ?: return
        val target = number?.takeIf { it.isNotBlank() } ?: return
        launch(phoneIntent(ctx, target), "dial", target)
        dismiss()
    }

    private fun switchPhoneMode(number: String?, sms: Boolean) {
        val ctx = appContext ?: return
        val target = number?.takeIf { it.isNotBlank() } ?: return
        val island = client ?: return
        runCatching {
            island.update(buildCard(ctx, ClipIntent(ClipKind.PHONE, target), smsMode = sms))
            Log.i(TAG, "已切换为" + if (sms) "点卡片发短信" else "点卡片拨号")
        }.onFailure { Log.w(TAG, "switch mode failed: ${it.message}") }
    }

    private fun sms(number: String?) {
        val ctx = appContext ?: return
        val target = number?.takeIf { it.isNotBlank() } ?: return
        if (runCatching { smsIntent(ctx, target)?.send() }.isSuccess) {
            Log.i(TAG, "sms sent: $target")
        } else {
            Log.w(TAG, "sms send failed: $target")
        }
        dismiss()
    }

    private fun launch(intent: PendingIntent?, what: String, payload: String) {
        if (intent == null) {
            Log.w(TAG, "$what: PendingIntent 为空（payload=$payload）")
            return
        }
        if (runCatching { intent.send() }.isSuccess) {
            Log.i(TAG, "$what sent: $payload")
            return
        }
        val withOptions = runCatching {
            val options = android.app.ActivityOptions.makeBasic()
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                options.setPendingIntentCreatorBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                options.setPendingIntentBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }
            intent.send(null, 0, null, null, null, null, options.toBundle())
        }.isSuccess
        if (withOptions) {
            Log.i(TAG, "$what sent(bg): $payload")
            return
        }
        Log.w(TAG, "$what send failed, fallback startActivity: $payload")
    }

    private fun smsIntent(context: Context, number: String): PendingIntent? {
        val app = context.applicationContext
        val uri = runCatching { Uri.parse("smsto:" + Uri.encode(number)) }.getOrNull() ?: return null
        val target = Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            PendingIntent.getActivity(
                app,
                number.hashCode(),
                target,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }.onFailure { Log.w(TAG, "sms intent build failed: ${it.message}") }.getOrNull()
    }

    private fun phoneIntent(context: Context, number: String): PendingIntent? {
        val app = context.applicationContext
        val uri = runCatching { Uri.parse("tel:" + Uri.encode(number)) }.getOrNull() ?: return null
        val target = Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            PendingIntent.getActivity(
                app,
                number.hashCode(),
                target,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }.onFailure { Log.w(TAG, "phone intent build failed: ${it.message}") }.getOrNull()
    }

    private fun lastShown(): ClipIntent? {
        currentIntent?.let { return it }
        val ctx = appContext ?: return null
        return runCatching {
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val payload = prefs.getString(KEY_LAST_PAYLOAD, null)?.takeIf { it.isNotBlank() } ?: return null
            ClipIntent(ClipKind.of(prefs.getString(KEY_LAST_KIND, null)), payload)
        }.getOrNull()
    }

    private fun rememberShown(intent: ClipIntent) {
        val ctx = appContext ?: return
        runCatching {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_LAST_PAYLOAD, intent.payload)
                .putString(KEY_LAST_KIND, intent.kind.wire)
                .apply()
        }
    }

    private fun persistPending(intent: ClipIntent) {
        val ctx = appContext ?: return
        runCatching {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_PAYLOAD, intent.payload)
                .putString(KEY_KIND, intent.kind.wire)
                .putLong(KEY_AT, System.currentTimeMillis())
                .apply()
        }
    }

    private fun clearPending() {
        val ctx = appContext ?: return
        runCatching {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(KEY_PAYLOAD).remove(KEY_KIND).remove(KEY_AT).apply()
        }
    }

    private const val PREFS = "mitian_link_pending"
    private const val KEY_PAYLOAD = "payload"
    private const val KEY_LAST_PAYLOAD = "last_payload"
    private const val KEY_LAST_KIND = "last_kind"
    private const val KEY_KIND = "kind"
    private const val KEY_AT = "at"
}
