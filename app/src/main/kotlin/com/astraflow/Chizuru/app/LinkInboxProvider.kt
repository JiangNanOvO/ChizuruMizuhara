package com.astraflow.Chizuru.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.util.Log
import com.astraflow.Chizuru.capability.link.LinkHub
import com.astraflow.Chizuru.settings.ModuleEnabledState
import com.astraflow.Chizuru.settings.ModulePrefs

class LinkInboxProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val ctx = context ?: return Bundle.EMPTY

        

        

        val caller = Binder.getCallingUid()
        val trustedUid = caller == Process.SYSTEM_UID || caller == Process.myUid()

        if (!trustedUid) {
            Log.w(TAG, "拒绝来路不明的投递（caller uid=$caller）")
            return Bundle.EMPTY
        }

        return when (method) {
            METHOD_DELIVER -> onDeliver(ctx.applicationContext, arg)
            METHOD_HELLO -> onHello(ctx.applicationContext, extras)
            else -> Bundle.EMPTY
        }
    }

    private fun onHello(app: Context, extras: Bundle?): Bundle {
        val framework = extras?.getString(EXTRA_FRAMEWORK)
        val version = extras?.getInt(EXTRA_MODULE_VERSION, 0) ?: 0
        runCatching { ModuleEnabledState.markLoaded(app, framework, version) }
            .onFailure { Log.e(TAG, "markLoaded failed", it) }
        return Bundle.EMPTY
    }

    private fun onDeliver(app: Context, url: String?): Bundle {
        val link = url?.takeIf { it.isNotBlank() } ?: return Bundle.EMPTY

        

        runCatching { ModuleEnabledState.markLoaded(app, FRAMEWORK_CLIPBOARD_HOOK, 0) }

        val prefs = ModulePrefs.of(app)
        if (!ModulePrefs.isEnabled(prefs) || !ModulePrefs.isLinkEnabled(prefs)) {
            Log.i(TAG, "链接助手已关闭，忽略投递")
            return Bundle.EMPTY
        }
        Log.i(TAG, "收到一条链接投递")
        val ok = LinkHub.postBlocking(app, link, AWAIT_MS)
        return Bundle().apply { putInt(RESULT_CODE, if (ok) 0 else -1) }
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        const val AUTHORITY = "com.astraflow.Chizuru.inbox"
        const val METHOD_DELIVER = "deliver"
        const val METHOD_HELLO = "hello"
        const val EXTRA_FRAMEWORK = "framework"
        const val EXTRA_MODULE_VERSION = "moduleVersion"
        const val RESULT_CODE = "resultCode"
        private const val TAG = "Mitian"

        private const val AWAIT_MS = 5_000L

        const val FRAMEWORK_CLIPBOARD_HOOK = "system_server/ClipboardService"
    }
}
