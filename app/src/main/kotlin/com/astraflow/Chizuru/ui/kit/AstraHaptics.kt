package com.astraflow.Chizuru.ui.kit

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * 部件的触感反馈,照系统的触感常量。系统设置里关了「触摸振动」时不振。
 * 只给开关、滑条、分段、底栏、选项菜单、确认框用;普通打开下一页的行不振。
 */
@Stable
class AstraHaptics internal constructor(private val view: View) {
    /** 开关拨到开或关。 */
    fun toggle(on: Boolean) = perform(if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)

    /** 换了一个选项:分段、底栏标签、菜单里的一项、分档滑条走过一档。 */
    fun tick() = perform(HapticFeedbackConstants.SEGMENT_TICK)

    /** 档位很密的滑条走过一档,比 [tick] 轻。 */
    fun lightTick() = perform(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)

    /** 连续滑条碰到两端。 */
    fun edge() = perform(HapticFeedbackConstants.CLOCK_TICK)

    /** 确认框里点了确定。 */
    fun confirm() = perform(HapticFeedbackConstants.CONFIRM)

    /** 操作被拒绝(例如没开通的设置)。 */
    fun reject() = perform(HapticFeedbackConstants.REJECT)

    /** 拖动开始(弹层下拉、滑块被抓起)。 */
    fun gestureStart() = perform(HapticFeedbackConstants.GESTURE_START)

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

/** 取当前界面的触感反馈:`val haptics = rememberAstraHaptics()`,再 `haptics.tick()`。 */
@Composable
fun rememberAstraHaptics(): AstraHaptics {
    val view = LocalView.current
    return remember(view) { AstraHaptics(view) }
}
