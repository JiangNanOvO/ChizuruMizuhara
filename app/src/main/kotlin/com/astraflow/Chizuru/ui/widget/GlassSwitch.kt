package com.astraflow.Chizuru.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import com.astraflow.Chizuru.ui.ios.GlassPainter

class GlassSwitch(context: Context) : View(context) {

    private val trackRect = RectF()
    private var progress = 0f          
    private var checkedState = false
    private var dragging = false
    private var pressStretch = 0f      
    private var glow = 0f              

    private var anim: ValueAnimator? = null
    private var glowAnim: ValueAnimator? = null
    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f
    private var moved = false

    var onCheckedChange: ((Boolean) -> Unit)? = null

    private fun dp(v: Number) = v.toFloat() * density

    init {
        layoutParams = ViewGroup.LayoutParams(dp(52).toInt(), dp(32).toInt())
        isClickable = true
        isFocusable = true
    }

    fun setChecked(value: Boolean, animate: Boolean = true) {
        if (checkedState == value) return
        checkedState = value
        animateTo(if (value) 1f else 0f, animate)
    }

    fun isCheckedState(): Boolean = checkedState

    private fun animateTo(target: Float, animate: Boolean, overshoot: Boolean = true) {
        anim?.cancel()
        if (!animate) {
            progress = target
            invalidate()
            return
        }
        anim = ValueAnimator.ofFloat(progress, target).apply {
            duration = if (overshoot) 320 else 180
            interpolator = if (overshoot)
                PathInterpolator(0.34f, 1.42f, 0.56f, 1f)   
            else
                PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateGlow(target: Float) {
        glowAnim?.cancel()
        glowAnim = ValueAnimator.ofFloat(glow, target).apply {
            duration = if (target > glow) 120 else 320
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener {
                glow = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateStretch(target: Float) {
        ValueAnimator.ofFloat(pressStretch, target).apply {
            duration = 140
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener {
                pressStretch = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = dp(0.5f)
        trackRect.set(inset, inset, w - inset, h - inset)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                moved = false
                animateStretch(1f)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging &&
                    (Math.abs(event.x - downX) > touchSlop || Math.abs(event.y - downY) > touchSlop)
                ) {
                    dragging = true
                    animateGlow(1f)
                }
                if (dragging) {
                    moved = true
                    anim?.cancel()
                    progress = progressFor(event.x)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animateStretch(0f)
                val wasDragging = dragging
                dragging = false
                animateGlow(0f)
                if (wasDragging && moved) {

                    setCheckedState(progress >= 0.5f)
                } else {

                    setCheckedState(!checkedState)
                }
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun setCheckedState(value: Boolean) {
        val changed = checkedState != value
        checkedState = value
        animateTo(if (value) 1f else 0f, animate = true)
        if (changed) onCheckedChange?.invoke(value)
    }

    private fun progressFor(x: Float): Float {
        val knobR = trackRect.height() / 2f - dp(2)
        val minX = trackRect.left + dp(2) + knobR
        val maxX = trackRect.right - dp(2) - knobR
        if (maxX <= minX) return progress
        return ((x - minX) / (maxX - minX)).coerceIn(0f, 1f)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (trackRect.isEmpty) return
        val radius = trackRect.height() / 2f
        val knobR = radius - dp(3)
        val minX = trackRect.left + dp(3) + knobR
        val maxX = trackRect.right - dp(3) - knobR
        val cx = minX + (maxX - minX) * progress

        val offTop = 0xFFE9E9EC.toInt()
        val offBottom = 0xFFD6D6DC.toInt()
        val onTop = 0xFF3ED968.toInt()
        val onBottom = 0xFF28B84C.toInt()
        GlassPainter.track(
            canvas, trackRect, radius,
            blend(offTop, onTop, progress),
            blend(offBottom, onBottom, progress)
        )

        GlassPainter.innerTopShadow(
            canvas, trackRect, radius, trackRect.height() * 0.62f,
            alpha = (0x30 - (progress * 0x18).toInt()).coerceAtLeast(0x14)
        )

        GlassPainter.innerBottomGlow(
            canvas, trackRect, radius, trackRect.height() * 0.52f,
            alpha = (0x3C + progress * 0x22).toInt()
        )

        if (progress > 0.02f && progress < 0.98f) {
            drawLiquidEdge(canvas, cx, radius)
        }

        GlassPainter.edge(
            canvas, trackRect, radius,
            topAlpha = (0x66 + progress * 0x3A).toInt(),
            bottomAlpha = 0x30
        )

        GlassPainter.sheen(canvas, trackRect, radius, strength = (0x20 + glow * 0x18).toInt())

        val stretch = 1f + pressStretch * 0.10f + glow * 0.24f
        drawKnob(canvas, cx, trackRect.centerY(), knobR * stretch, knobR)
    }

    private fun drawLiquidEdge(canvas: Canvas, x: Float, radius: Float) {
        val half = radius * 0.62f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(trackRect, radius, radius, Path.Direction.CW) })

        val band = RectF(x - half, trackRect.top + dp(1), x + half, trackRect.bottom - dp(1))
        paint.shader = LinearGradient(
            band.left, 0f, band.right, 0f,
            intArrayOf(Color.TRANSPARENT, 0x88FFFFFF.toInt(), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(band, paint)

        val dark = RectF(
            x + half * 0.15f, trackRect.top + dp(1),
            x + half * 1.6f, trackRect.bottom - dp(1)
        )
        paint.shader = LinearGradient(
            dark.left, 0f, dark.right, 0f,
            intArrayOf(0x2A000000, Color.TRANSPARENT), null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(dark, paint)
        canvas.restore()
    }

    private fun drawKnob(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float) {

        

        val rect = RectF(cx - rx, cy - ry, cx + rx, cy + ry)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            cx, cy - ry, cx, cy + ry,
            0xFFFFFFFF.toInt(), 0xFFF7F7FA.toInt(), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, ry, ry, paint)

        paint.shader = RadialGradient(
            cx - rx * 0.30f, cy - ry * 0.48f, ry * 1.02f,
            intArrayOf(0x77FFFFFF, Color.TRANSPARENT), null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, ry, ry, paint)

        paint.shader = LinearGradient(
            cx, cy - ry, cx, cy + ry,
            0x99FFFFFF.toInt(), 0x11FFFFFF, Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1.1f)
        canvas.drawRoundRect(rect, ry, ry, paint)
        paint.style = Paint.Style.FILL

        paint.shader = LinearGradient(
            cx, cy + ry * 0.35f, cx, cy + ry,
            Color.TRANSPARENT, 0x0F000000, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, ry, ry, paint)

        paint.shader = RadialGradient(
            cx - rx * 0.34f, cy - ry * 0.52f, ry * 0.34f,
            intArrayOf(0xCCFFFFFF.toInt(), Color.TRANSPARENT), null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, ry, ry, paint)
    }

    private fun blend(from: Int, to: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        val rr = (Color.red(from) + (Color.red(to) - Color.red(from)) * r).toInt()
        val gg = (Color.green(from) + (Color.green(to) - Color.green(from)) * r).toInt()
        val bb = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * r).toInt()
        val aa = (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * r).toInt()
        return Color.argb(aa, rr, gg, bb)
    }
}
