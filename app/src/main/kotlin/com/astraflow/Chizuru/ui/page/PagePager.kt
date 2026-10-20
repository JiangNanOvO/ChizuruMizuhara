package com.astraflow.Chizuru.ui.page

import android.animation.ValueAnimator
import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import kotlin.math.abs

internal class PagePager(context: Context) : FrameLayout(context) {

    var onSettled: ((Int) -> Unit)? = null

    var onTarget: ((Int) -> Unit)? = null

    private val pages = ArrayList<View>()
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val curve = PathInterpolator(0.2f, 0f, 0f, 1f)

    private var index = 0
    private var offset = 0f
    private var downX = 0f
    private var downY = 0f
    private var downOffset = 0f
    private var tracking = false
    private var claimed = false
    private var animator: ValueAnimator? = null
    private var velocity: VelocityTracker? = null

    fun current(): Int = index

    fun addPage(page: View) {
        pages.add(page)
        addView(page, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        page.visibility = if (pages.size - 1 == index) View.VISIBLE else View.INVISIBLE
        page.translationX = (pages.size - 1 - offset) * width
    }

    fun replacePage(position: Int, page: View) {
        if (position !in pages.indices) return
        val old = pages[position]
        removeView(old)
        pages[position] = page
        addView(page, position, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        page.translationX = (position - offset) * width
        page.visibility = if (abs(position - offset) < 1f) View.VISIBLE else View.INVISIBLE
    }

    fun show(target: Int, animate: Boolean) {
        if (target !in pages.indices) return
        if (!animate) {
            animator?.cancel()
            index = target
            offset = target.toFloat()
            layoutPages()
            return
        }
        if (target == index && offset == index.toFloat()) return
        animateTo(target)
    }

    private fun animateTo(target: Int) {
        animator?.cancel()
        val start = offset
        val end = target.toFloat()
        if (start == end) {
            index = target
            layoutPages()
            onSettled?.invoke(target)
            return
        }
        val anim = ValueAnimator.ofFloat(start, end)
        anim.duration = (200 + 90 * abs(end - start)).toLong().coerceAtMost(340)
        anim.interpolator = curve
        anim.addUpdateListener {
            offset = it.animatedValue as Float
            layoutPages()
        }
        anim.addListener(object : android.animation.AnimatorListenerAdapter() {
            private var done = false

            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (done) return
                done = true
                index = target
                offset = target.toFloat()
                layoutPages()
                onSettled?.invoke(target)
            }
        })
        animator = anim
        anim.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutPages()
    }

    private fun layoutPages() {
        val w = width.toFloat()
        if (w <= 0f) return
        for (i in pages.indices) {
            val page = pages[i]
            val distance = i - offset
            page.translationX = distance * w
            page.visibility = if (abs(distance) < 1f) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animator?.cancel()
                downX = ev.x
                downY = ev.y
                downOffset = offset
                tracking = true
                claimed = false
                velocity?.recycle()
                velocity = VelocityTracker.obtain().also { it.addMovement(ev) }
            }

            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(ev)
                if (tracking && !claimed) {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    if (abs(dx) > slop * 1.5f && abs(dx) > abs(dy)) {
                        claimed = true
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> tracking = false
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(ev)
                val w = width.toFloat()
                if (w > 0f) {
                    val max = (pages.size - 1).toFloat()
                    offset = (downOffset - (ev.x - downX) / w).coerceIn(0f, max)
                    layoutPages()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocity?.addMovement(ev)
                settle(ev.actionMasked == MotionEvent.ACTION_UP)
            }
        }
        return true
    }

    private fun settle(allowFling: Boolean) {
        tracking = false
        claimed = false
        var fling = 0f
        if (allowFling) {
            velocity?.computeCurrentVelocity(1000)
            fling = velocity?.xVelocity ?: 0f
        }
        velocity?.recycle()
        velocity = null
        val delta = offset - index
        val target = when {
            delta < -0.18f || fling < -900f -> index + 1
            delta > 0.18f || fling > 900f -> index - 1
            else -> index
        }.coerceIn(0, pages.size - 1)
        onTarget?.invoke(target)
        animateTo(target)
    }
}
