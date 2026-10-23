package com.astraflow.Chizuru.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt

class WatermarkView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = TEXT_SIZE_SP * resources.displayMetrics.scaledDensity
    }
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val night = (context.resources.configuration.uiMode
        and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES

    private var versionLine = ""
    private var timeLine = ""

    private val ticker = object : Runnable {
        override fun run() {
            updateTime()
            val now = System.currentTimeMillis()
            postDelayed(this, (60_000L - now % 60_000L).coerceAtLeast(1_000L))
        }
    }

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
        versionLine = LINE_ONE.ifBlank {
            runCatching {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                "${info.versionName}(${info.versionCode})"
            }.getOrDefault("")
        }
        updateTime()
    }

    private fun updateTime() {
        timeLine = LINE_TWO.ifBlank { formatter.format(Date(System.currentTimeMillis())) }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postDelayed(ticker, 1_000L)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0 || versionLine.isEmpty()) return
        val value = if (night) 255 else 0
        paint.color = Color.argb((ALPHA * 255).toInt(), value, value, value)
        val diagonal = sqrt(width.toFloat() * width + height.toFloat() * height)
        val spacingPx = SPACING_DP * density
        val lineHeight = paint.fontSpacing

        canvas.save()
        canvas.rotate(ROTATION, width / 2f, height / 2f)

        val widthVersion = paint.measureText(versionLine)
        val widthTime = paint.measureText(timeLine)
        val maxTextWidth = max(widthVersion, widthTime)
        val offsetVersion = (maxTextWidth - widthVersion) / 2f
        val offsetTime = (maxTextWidth - widthTime) / 2f
        val stepX = max(spacingPx, maxTextWidth * 1.3f)
        val stepY = max(spacingPx * 0.6f, lineHeight * 2.5f)
        val startX = -diagonal / 2f
        val startY = -diagonal / 2f + lineHeight
        val endX = width + diagonal / 2f
        val endY = height + diagonal / 2f

        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                canvas.drawText(versionLine, x + offsetVersion, y, paint)
                canvas.drawText(timeLine, x + offsetTime, y + lineHeight, paint)
                x += stepX
            }
            y += stepY
        }
        canvas.restore()
    }

    private companion object {
        const val LINE_ONE = "v1.6 谜天出品 最后构建时间 2026-10-22 00:23"
        const val LINE_TWO = ""
        const val ALPHA = 0.05f
        const val TEXT_SIZE_SP = 11f
        const val SPACING_DP = 180f
        const val ROTATION = -30f
    }
}
