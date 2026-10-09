package com.astraflow.MysticSky.ui.widget

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator

object Motion {

    val EMPHASIS = PathInterpolator(0.2f, 0f, 0f, 1f)

    const val PRESS_SCALE = 0.975f
    const val PRESS_DURATION = 110L
    const val RELEASE_DURATION = 180L

    fun pressFeedback(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(PRESS_SCALE).scaleY(PRESS_SCALE)
                        .setDuration(PRESS_DURATION)
                        .setInterpolator(EMPHASIS)
                        .start()
                    v.alpha = 0.92f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(RELEASE_DURATION)
                        .setInterpolator(EMPHASIS)
                        .start()
                    v.alpha = 1f
                }
            }
            false
        }
    }

    fun breathe(view: View, minAlpha: Float = 0.35f, periodMs: Long = 2200L) {
        val animator = ValueAnimator.ofFloat(minAlpha, 1f).apply {
            duration = periodMs / 2
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = PathInterpolator(0.4f, 0f, 0.6f, 1f)
            addUpdateListener { view.alpha = it.animatedValue as Float }
            start()
        }

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) = animator.cancel()
        })
    }

    fun fadeInUp(view: View, delayMs: Long = 0L) {
        view.alpha = 0f
        view.translationY = 18f * view.resources.displayMetrics.density
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delayMs)
            .setDuration(320)
            .setInterpolator(EMPHASIS)
            .start()
    }

    fun animateTrack(knob: View, toX: Float, durationMs: Long = 220L) {
        knob.animate()
            .translationX(toX)
            .setDuration(durationMs)
            .setInterpolator(EMPHASIS)
            .start()
    }
}
