package com.astraflow.Chizuru.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.astraflow.Chizuru.ui.kit.AstraAlert

/**
 * 一次性的结果提示(「设置已保存」「已关闭常驻保活」这类):屏幕中间的提示框,点「知道了」收起。
 * 界面的结果反馈只走这一条路,不使用底部弹出的长条。
 */
@Stable
class NoticeState {
    var message by mutableStateOf<String?>(null)
        private set

    fun show(text: String) {
        message = text
    }

    fun dismiss() {
        message = null
    }
}

val LocalNoticeState = staticCompositionLocalOf { NoticeState() }

/**
 * 界面上唯一的提示框:文案由 [LocalNoticeState] 推送。
 *
 * 收起动画期间继续显示最后一条正文:直接跟着 [NoticeState.message] 取空值的话,点「知道了」的当刻正文先消失,
 * 只剩标题和一张空卡片,等卡片收起动画播完才整个消失,观感是正文先闪没了。
 * 下一次推送新消息时正文立即换成新的,不会先闪出上一条。
 */
@Composable
fun AstraNoticeHost() {
    val state = LocalNoticeState.current
    val message = state.message
    val shown = remember { mutableStateOf<String?>(null) }
    if (message != null) shown.value = message
    AstraAlert(
        visible = message != null,
        title = "提示",
        message = shown.value,
        onDismiss = { state.dismiss() },
        confirmText = "知道了",
        onConfirm = { state.dismiss() },
        dismissText = null,
    )
}
