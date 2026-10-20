package com.astraflow.Chizuru.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import com.astraflow.Chizuru.ui.ios.Theme
import kotlin.math.abs

class ToggleSwitch(context: Context) : View(context) {

    private val theme = Theme.of(context)
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val track = RectF()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var progress = 0f
    private var checked = false
    private var animator: ValueAnimator? = null

    private var downX = 0f
    private var dragging = false
    private var moved = false

    var onCheckedChange: ((Boolean) -> Unit)? = null

    init {
        layoutParams = ViewGroup.LayoutParams(dp(51f), dp(31f))
        isClickable = true
        isFocusable = true
    }

    fun setChecked(value: Boolean, animate: Boolean = false) {
        if (checked == value) return
        checked = value
        animateTo(if (value) 1f else 0f, animate)
    }

    fun isChecked(): Boolean = checked

    private fun dp(value: Float): Int = (value * density).toInt()

    private fun animateTo(target: Float, animate: Boolean) {
        animator?.cancel()
        if (!animate) {
            progress = target
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 180
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun progressAt(x: Float): Float {
        val knob = height / 2f - dp(2f)
        val min = dp(2f) + knob
        val max = width - dp(2f) - knob
        if (max <= min) return progress
        return ((x - min) / (max - min)).coerceIn(0f, 1f)
    }

    private fun commit(value: Boolean) {
        val changed = checked != value
        checked = value
        animateTo(if (value) 1f else 0f, animate = true)
        if (changed) onCheckedChange?.invoke(value)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                dragging = false
                moved = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging && abs(event.x - downX) > touchSlop) dragging = true
                if (dragging) {
                    moved = true
                    animator?.cancel()
                    progress = progressAt(event.x)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val wasDragging = dragging
                dragging = false
                if (wasDragging && moved) commit(progress >= 0.5f) else commit(!checked)
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                moved = false
                animateTo(if (checked) 1f else 0f, animate = true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val colors = theme.colors
        val radius = height / 2f
        track.set(0f, 0f, width.toFloat(), height.toFloat())

        paint.style = Paint.Style.FILL
        paint.color = blend(colors.switchTrackOff, colors.systemGreen, progress)
        canvas.drawRoundRect(track, radius, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = colors.switchTrackEdge
        canvas.drawRoundRect(track, radius, radius, paint)
        paint.style = Paint.Style.FILL

        val knobRadius = radius - dp(2f)
        val min = dp(2f) + knobRadius
        val max = width - dp(2f) - knobRadius
        val cx = min + (max - min) * progress
        val cy = height / 2f

        paint.color = colors.switchKnobShadow
        canvas.drawCircle(cx, cy + density * 0.5f, knobRadius, paint)

        paint.color = colors.switchKnob
        canvas.drawCircle(cx, cy, knobRadius, paint)
    }

    private fun blend(from: Int, to: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        return android.graphics.Color.argb(
            (android.graphics.Color.alpha(from) +
                (android.graphics.Color.alpha(to) - android.graphics.Color.alpha(from)) * r).toInt(),
            (android.graphics.Color.red(from) +
                (android.graphics.Color.red(to) - android.graphics.Color.red(from)) * r).toInt(),
            (android.graphics.Color.green(from) +
                (android.graphics.Color.green(to) - android.graphics.Color.green(from)) * r).toInt(),
            (android.graphics.Color.blue(from) +
                (android.graphics.Color.blue(to) - android.graphics.Color.blue(from)) * r).toInt(),
        )
    }
}
