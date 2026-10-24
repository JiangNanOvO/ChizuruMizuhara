package com.astraflow.Chizuru.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.view.View

class AvatarView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private var bitmap: Bitmap? = null

    fun load(assetPath: String) {
        val decoded = runCatching { decode(assetPath) }.getOrNull() ?: return
        val square = centerCrop(decoded)
        bitmap = square
        paint.shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        invalidate()
    }

    private fun decode(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 200 && bounds.outHeight / (sample * 2) >= 200) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun centerCrop(source: Bitmap): Bitmap {
        val side = minOf(source.width, source.height)
        if (side == source.width && side == source.height) return source
        return Bitmap.createBitmap(
            source,
            (source.width - side) / 2,
            (source.height - side) / 2,
            side,
            side,
        )
    }

    override fun onDraw(canvas: Canvas) {
        val source = bitmap ?: return
        val diameter = minOf(width, height).toFloat()
        if (diameter <= 0f) return
        val left = (width - diameter) / 2f
        val top = (height - diameter) / 2f
        matrix.setScale(diameter / source.width, diameter / source.height)
        matrix.postTranslate(left, top)
        paint.shader?.setLocalMatrix(matrix)
        canvas.drawCircle(left + diameter / 2f, top + diameter / 2f, diameter / 2f, paint)
    }
}
