package com.astraflow.Chizuru.ui.widget

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator

object Motion {

    val EMPHASIS = PathInterpolator(0.2f, 0f, 0f, 1f)

    private val EXIT = PathInterpolator(0.4f, 0f, 1f, 1f)

    const val PRESS_SCALE = 0.94f
    const val PRESS_DURATION = 110L
    const val RELEASE_DURATION = 200L

    fun pressFeedback(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(PRESS_SCALE).scaleY(PRESS_SCALE)
                        .setDuration(PRESS_DURATION)
                        .setInterpolator(EMPHASIS)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(RELEASE_DURATION)
                        .setInterpolator(EMPHASIS)
                        .start()
                }
            }
            false
        }
    }


    fun bindLargeTitle(scroll: android.widget.ScrollView, title: View) {
        val max = { title.height.coerceAtLeast(1).toFloat() }
        scroll.setOnScrollChangeListener { _, _, y, _, _ ->
            val t = (y / max()).coerceIn(0f, 1f)
            title.scaleX = 1f - 0.42f * t
            title.scaleY = 1f - 0.42f * t
            title.pivotX = 0f
            title.pivotY = 0f
            title.alpha = 1f - t
        }
    }



}
