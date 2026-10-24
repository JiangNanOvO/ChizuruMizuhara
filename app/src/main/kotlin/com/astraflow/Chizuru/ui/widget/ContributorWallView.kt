package com.astraflow.Chizuru.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.PathInterpolator
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.math.tanh

class ContributorWallView(context: Context) : View(context) {

    class Entry(val avatar: String)

    var onPick: ((Int) -> Unit)? = null

    private class Bubble {
        var entry = 0
        var bitmap: Bitmap? = null
        var shader: BitmapShader? = null
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = Matrix()
    }

    private val bubbles = ArrayList<Bubble>()
    private val centers = ArrayList<PointF>()
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val curve = PathInterpolator(0.2f, 0f, 0f, 1f)
    private var settleAnimation: ValueAnimator? = null

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val ratio = (zoom * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM) / zoom
                if (ratio == 1f) return true
                val focusX = detector.focusX - width / 2f
                val focusY = detector.focusY - height / 2f
                offsetX = (offsetX - focusX) * ratio + focusX
                offsetY = (offsetY - focusY) * ratio + focusY
                zoom *= ratio
                offsetX = resist(offsetX, limitX(), width.toFloat())
                offsetY = resist(offsetY, limitY(), height.toFloat())
                invalidate()
                return true
            }
        },
    )

    private var zoom = START_ZOOM
    private var offsetX = 0f
    private var offsetY = 0f
    private var avatarSize = 0f
    private var contentWidth = 0f
    private var contentHeight = 0f
    private var fitScale = 1f

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var pointerCount = 0
    private var moved = false

    fun setEntries(list: List<Entry>) {
        bubbles.clear()
        for ((index, entry) in list.withIndex()) {
            val square = decode(entry.avatar) ?: continue
            val bubble = Bubble()
            bubble.entry = index
            bubble.bitmap = square
            bubble.shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            bubble.paint.shader = bubble.shader
            bubbles.add(bubble)
        }
        geometryDirty = true
        invalidate()
    }

    private var geometryDirty = true

    private fun decode(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.assets.open("avatars/$path").use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
        }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 160 && bounds.outHeight / (sample * 2) >= 160) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val source = runCatching {
            context.assets.open("avatars/$path").use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }.getOrNull() ?: return null
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        geometryDirty = true
    }

    private fun buildGeometry() {
        geometryDirty = false
        centers.clear()
        if (bubbles.isEmpty() || width <= 0 || height <= 0) return
        avatarSize = AVATAR_DP * resources.displayMetrics.density
        val gap = GAP_DP * resources.displayMetrics.density
        val count = bubbles.size
        var radius = 0
        while (1 + 3 * radius * (radius + 1) < count) radius++
        val spacing = avatarSize + gap
        val points = ArrayList<Pair<Int, PointF>>()
        for (r in -radius..radius) {
            for (q in -radius..radius) {
                if (abs(q + r) <= radius && (r > 0 || r == 0 && q > 0)) {
                    val point = PointF(
                        (q + r / 2f) * spacing,
                        r * spacing * sqrt(3f) / 2f,
                    )
                    points.add((q * q + q * r + r * r) to point)
                }
            }
        }
        points.sortWith(
            compareBy<Pair<Int, PointF>> { it.first }
                .thenBy { atan2(it.second.y, it.second.x) },
        )
        val selected = ArrayList<PointF>()
        selected.add(PointF(0f, 0f))
        for ((_, point) in points) {
            selected.add(point)
            selected.add(PointF(-point.x, -point.y))
            if (selected.size >= count) break
        }
        while (selected.size < count) selected.add(PointF(0f, 0f))

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (point in selected) {
            minX = min(minX, point.x)
            maxX = max(maxX, point.x)
            minY = min(minY, point.y)
            maxY = max(maxY, point.y)
        }
        val centreX = (minX + maxX) / 2f
        val centreY = (minY + maxY) / 2f
        for (point in selected) {
            centers.add(PointF(point.x - centreX, point.y - centreY))
        }
        contentWidth = maxX - minX + avatarSize
        contentHeight = maxY - minY + avatarSize

        val inset = 8f * resources.displayMetrics.density
        val availableWidth = width - inset * 2
        val availableHeight = height - inset * 2
        fitScale = min(
            availableWidth / contentWidth,
            availableHeight / contentHeight,
        ).coerceAtMost(1f)
        offsetX = constrain(offsetX, limitX())
        offsetY = constrain(offsetY, limitY())
    }

    private fun limitX(): Float =
        max(0f, (contentWidth * fitScale * zoom - width) / 2f)

    private fun limitY(): Float =
        max(0f, (contentHeight * fitScale * zoom - height) / 2f)

    private fun constrain(value: Float, bound: Float): Float =
        if (bound <= 0f) 0f else value.coerceIn(-bound, bound)

    private fun resist(value: Float, bound: Float, extent: Float): Float {
        val overflow = abs(value) - bound
        if (overflow <= 0f) return value
        val travel = extent * 0.18f
        return sign(value) * (bound + travel * overflow / (travel + overflow))
    }

    private fun bubbleTransform(position: PointF): PointF {
        val distance = max(
            abs(position.x) / (width / 2f),
            abs(position.y) / (height / 2f),
        )
        if (distance <= 0.3f) return PointF(position.x, position.y)
        val edgeDistance = distance - 0.3f
        val curveValue = tanh(edgeDistance / 0.42f)
        val compressed = 0.3f + 0.35f * edgeDistance + 0.65f * 0.42f * curveValue
        val factor = if (distance == 0f) 1f else compressed / distance
        return PointF(position.x * factor, position.y * factor)
    }

    private fun bubbleScale(position: PointF): Float {
        val distance = max(
            abs(position.x) / (width / 2f),
            abs(position.y) / (height / 2f),
        )
        if (distance <= 0.3f) return 1f
        val edgeDistance = distance - 0.3f
        val curveValue = tanh(edgeDistance / 0.42f)
        return 0.35f + 0.65f * (1f - curveValue * curveValue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (geometryDirty) buildGeometry()
        if (centers.isEmpty()) return
        val scale = fitScale * zoom
        val halfAvatar = avatarSize / 2f
        for ((index, bubble) in bubbles.withIndex()) {
            val bitmap = bubble.bitmap ?: continue
            val centre = centers.getOrNull(index) ?: continue
            val rawX = centre.x * scale + offsetX
            val rawY = centre.y * scale + offsetY
            val position = PointF(rawX, rawY)
            val transform = bubbleTransform(position)
            val radius = halfAvatar * scale * bubbleScale(position)
            if (radius < 1f) continue
            val drawX = width / 2f + transform.x
            val drawY = height / 2f + transform.y
            if (drawX + radius < 0f || drawX - radius > width) continue
            if (drawY + radius < 0f || drawY - radius > height) continue
            bubble.matrix.setScale(radius * 2f / bitmap.width, radius * 2f / bitmap.height)
            bubble.matrix.postTranslate(drawX - radius, drawY - radius)
            bubble.shader?.setLocalMatrix(bubble.matrix)
            canvas.drawCircle(drawX, drawY, radius, bubble.paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                settleAnimation?.cancel()
                downX = event.x
                downY = event.y
                lastX = event.x
                lastY = event.y
                pointerCount = 1
                moved = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                pointerCount = event.pointerCount
                moved = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                    offsetX += event.x - lastX
                    offsetY += event.y - lastY
                    offsetX = resist(offsetX, limitX(), width.toFloat())
                    offsetY = resist(offsetY, limitY(), height.toFloat())
                    lastX = event.x
                    lastY = event.y
                    invalidate()
                }
                if (abs(event.x - downX) > slop || abs(event.y - downY) > slop) moved = true
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                pointerCount = event.pointerCount - 1
                lastX = event.getX(0)
                lastY = event.getY(0)
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!moved && !scaleDetector.isInProgress) {
                    pick(event.x, event.y)
                }
                settle()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                settle()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    private fun settle() {
        val targetX = constrain(offsetX, limitX())
        val targetY = constrain(offsetY, limitY())
        if (targetX == offsetX && targetY == offsetY) return
        settleAnimation?.cancel()
        val startX = offsetX
        val startY = offsetY
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 220L
        animator.interpolator = curve
        animator.addUpdateListener {
            val t = it.animatedValue as Float
            offsetX = startX + (targetX - startX) * t
            offsetY = startY + (targetY - startY) * t
            invalidate()
        }
        settleAnimation = animator
        animator.start()
    }

    private fun pick(x: Float, y: Float) {
        if (geometryDirty) buildGeometry()
        val scale = fitScale * zoom
        val halfAvatar = avatarSize / 2f
        var hit = -1
        var best = Float.MAX_VALUE
        for ((index, bubble) in bubbles.withIndex()) {
            val centre = centers.getOrNull(index) ?: continue
            val position = PointF(centre.x * scale + offsetX, centre.y * scale + offsetY)
            val transform = bubbleTransform(position)
            val radius = halfAvatar * scale * bubbleScale(position)
            val drawX = width / 2f + transform.x
            val drawY = height / 2f + transform.y
            val distance = hypot(x - drawX, y - drawY)
            if (distance <= radius + 2f && distance < best) {
                best = distance
                hit = bubble.entry
            }
        }
        if (hit >= 0) onPick?.invoke(hit)
    }

    private companion object {
        const val MIN_ZOOM = 0.6f
        const val MAX_ZOOM = 3f
        const val START_ZOOM = 1f
        const val AVATAR_DP = 52f
        const val GAP_DP = 6f
    }
}
