package com.dailybible.lock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Circular progress ring for the home screen. */
class RingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val stroke = 12f * density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = 0xFFFFE28A.toInt()
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        color = 0xFF3B2412.toInt()
    }
    private val bounds = RectF()
    private var fraction = 0f

    fun setProgress(done: Int, total: Int) {
        fraction = if (total <= 0) 0f else (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val half = stroke / 2f
        bounds.set(half, half, width - half, height - half)
        canvas.drawArc(bounds, 0f, 360f, false, trackPaint)
        if (fraction > 0f) {
            canvas.drawArc(bounds, -90f, 360f * fraction, false, progressPaint)
        }
    }
}
