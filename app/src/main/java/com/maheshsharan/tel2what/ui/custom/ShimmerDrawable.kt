package com.maheshsharan.tel2what.ui.custom

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

class ShimmerDrawable : Drawable(), Animatable {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shimmerOffset = 0f
    private val animator: ValueAnimator = ValueAnimator.ofFloat(-1f, 2f).apply {
        duration = 1200
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            shimmerOffset = it.animatedValue as Float
            invalidateSelf()
        }
    }

    private var baseColor = Color.parseColor("#1C1C1C")
    private var highlightColor = Color.parseColor("#2D2D2D")

    fun setColors(base: Int, highlight: Int) {
        baseColor = base
        highlightColor = highlight
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        if (width <= 0f || height <= 0f) return

        val shimmerWidth = width * 0.4f
        val startX = (width + shimmerWidth) * shimmerOffset - shimmerWidth
        val endX = startX + shimmerWidth

        val shader = LinearGradient(
            startX, 0f, endX, 0f,
            intArrayOf(baseColor, highlightColor, baseColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader

        canvas.drawRect(bounds, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun start() {
        if (!animator.isStarted) {
            animator.start()
        }
    }

    override fun stop() {
        if (animator.isStarted) {
            animator.cancel()
        }
    }

    override fun isRunning(): Boolean = animator.isRunning
}
