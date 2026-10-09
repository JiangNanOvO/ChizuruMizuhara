package com.astraflow.MysticSky.ui.widget

import android.graphics.Outline
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider

object Glass {

    const val RADIUS_CARD = 22f

    const val RADIUS_INNER = 16f

    const val RADIUS_PILL = 100f

    const val BLUR_RADIUS = 18f

    fun applyBlur(view: View, radiusDp: Float = BLUR_RADIUS) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val radius = radiusDp * view.resources.displayMetrics.density
        runCatching {
            view.setRenderEffect(
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            )
        }
    }

    fun clipRounded(view: View, radiusDp: Float) {
        val radius = radiusDp * view.resources.displayMetrics.density
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, radius)
            }
        }
        view.clipToOutline = true
    }
}
