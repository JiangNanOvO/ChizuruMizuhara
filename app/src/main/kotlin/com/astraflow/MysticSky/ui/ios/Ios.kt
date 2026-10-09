package com.astraflow.MysticSky.ui.ios

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider

object Ios {

    const val BG = 0xFFF2F2F7.toInt()             
    const val CARD = 0xFFFFFFFF.toInt()           
    const val CARD_ELEVATED = 0xFFE9E9EB.toInt()
    const val FILL = 0x0F000000                   

    const val LABEL = 0xFF000000.toInt()
    const val LABEL_SECONDARY = 0x993C3C43.toInt()
    const val LABEL_TERTIARY = 0x4D3C3C43.toInt()

    const val SEPARATOR = 0x3DC6C6C8
    const val SEPARATOR_OPAQUE = 0xFFC6C6C8.toInt()

    const val BLUE = 0xFF007AFF.toInt()
    const val GREEN = 0xFF34C759.toInt()
    const val RED = 0xFFFF3B30.toInt()
    const val ORANGE = 0xFFFF9500.toInt()
    const val YELLOW = 0xFFFFCC00.toInt()
    const val PURPLE = 0xFFAF52DE.toInt()
    const val TEAL = 0xFF5AC8FA.toInt()

    fun dp(context: Context, v: Number): Int =
        (v.toFloat() * context.resources.displayMetrics.density).toInt()

    fun radius(context: Context): Float = dp(context, 16).toFloat()
}

object LiquidGlass {

    fun panel(
        context: Context,
        radiusPx: Float,
        tint: Int = Ios.CARD,
        strokeAlpha: Int = 0x14
    ): Drawable {
        val base = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(tint)
        }
        val sheen = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0x0FFFFFFF, 0x05FFFFFF, 0x00FFFFFF)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
        }
        val edge = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setStroke(Ios.dp(context, 1), (strokeAlpha shl 24))
        }
        return LayerDrawable(arrayOf(base, sheen, edge))
    }

    fun applyTo(view: View, radiusPx: Float, tint: Int = Ios.CARD, elevationDp: Float = 1.5f) {
        view.background = panel(view.context, radiusPx, tint)
        view.clipToOutline = true
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, radiusPx)
            }
        }
        view.elevation = view.context.resources.displayMetrics.density * elevationDp
    }

    fun supportsWindowBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

object GlassPainter {

    fun track(canvas: Canvas, rect: RectF, radius: Float, topColor: Int, bottomColor: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, rect.top, 0f, rect.bottom, topColor, bottomColor, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    fun edge(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        topAlpha: Int = 0x66,
        bottomAlpha: Int = 0x14,
        topColor: Int = Color.WHITE,
        bottomColor: Int = Color.BLACK
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            shader = LinearGradient(
                0f, rect.top, 0f, rect.bottom,
                withAlpha(topColor, topAlpha),
                withAlpha(bottomColor, bottomAlpha),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    fun innerTopShadow(canvas: Canvas, rect: RectF, radius: Float, height: Float, alpha: Int = 0x1A) {
        val band = RectF(rect.left, rect.top, rect.right, rect.top + height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, band.top, 0f, band.bottom,
                Color.argb(alpha, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
        }
        canvas.save()
        canvas.clipPath(android.graphics.Path().apply {
            addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
        })
        canvas.drawRect(band, paint)
        canvas.restore()
    }

    fun sheen(canvas: Canvas, rect: RectF, radius: Float, strength: Int = 0x22) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                Color.argb(strength, 255, 255, 255), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.save()
        canvas.clipPath(android.graphics.Path().apply {
            addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
        })
        canvas.drawRect(rect, paint)
        canvas.restore()
    }

    fun shadow(canvas: Canvas, cx: Float, cy: Float, radius: Float, alpha: Int = 0x33) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(Color.argb(alpha, 0, 0, 0), Color.TRANSPARENT),
                floatArrayOf(0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    fun knob(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float) {
        shadow(canvas, cx, cy + ry * 0.30f, ry * 1.55f, 0x40)
        val rect = RectF(cx - rx, cy - ry, cx + rx, cy + ry)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            cx, cy - ry, cx, cy + ry,
            0xFFFFFFFF.toInt(), 0xFFF2F2F5.toInt(), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, ry, ry, paint)
        paint.shader = RadialGradient(
            cx - rx * 0.30f, cy - ry * 0.45f, ry * 1.0f,
            intArrayOf(0x50FFFFFF, Color.TRANSPARENT), null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, ry, ry, paint)
    }

    fun fill(canvas: Canvas, rect: RectF, radius: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    fun fillGradient(canvas: Canvas, rect: RectF, radius: Float, colors: IntArray) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.top, colors, null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    fun innerBottomGlow(canvas: Canvas, rect: RectF, radius: Float, height: Float, alpha: Int = 0x55) {
        val band = RectF(rect.left, rect.bottom - height, rect.right, rect.bottom)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, band.top, 0f, band.bottom,
                Color.TRANSPARENT, Color.argb(alpha, 255, 255, 255), Shader.TileMode.CLAMP
            )
        }
        canvas.save()
        canvas.clipPath(android.graphics.Path().apply {
            addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
        })
        canvas.drawRect(band, paint)
        canvas.restore()
    }

    fun topRim(canvas: Canvas, rect: RectF, radius: Float, inset: Float = 1.2f, alpha: Int = 0x99) {
        val r = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            shader = LinearGradient(
                0f, r.top, 0f, r.bottom,
                Color.argb(alpha, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(r, radius, radius, paint)
    }

    fun caustic(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, alpha: Int = 0x66) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, rx,
                intArrayOf(Color.argb(alpha, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.save()
        canvas.scale(1f, ry / rx.coerceAtLeast(0.001f), cx, cy)
        canvas.drawCircle(cx, cy, rx, paint)
        canvas.restore()
    }
}

fun View.setGlassBackground(radiusPx: Float, tint: Int = Ios.CARD) =
    LiquidGlass.applyTo(this, radiusPx, tint)
