package com.example.psgamepad

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Analog stick virtual. onMove mengirim x,y dalam -1..1 (y positif = bawah). */
class AnalogStickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMove: ((Float, Float) -> Unit)? = null

    private var nx = 0f
    private var ny = 0f
    private var active = false

    private val density = resources.displayMetrics.density
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A2E3A.toInt() }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3C4252.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun radius() = min(width, height) / 2f - ringPaint.strokeWidth
    private fun travel() = max(radius() - radius() * 0.42f, 1f)

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = radius()
        val knobR = r * 0.42f
        val t = travel()
        canvas.drawCircle(cx, cy, r, basePaint)
        canvas.drawCircle(cx, cy, r, ringPaint)
        knobPaint.color = if (active) 0xFF8FA3D9.toInt() else 0xFF5A6275.toInt()
        canvas.drawCircle(cx + nx * t, cy + ny * t, knobR, knobPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                active = true
                val t = travel()
                var dx = (e.x - width / 2f) / t
                var dy = (e.y - height / 2f) / t
                val len = hypot(dx, dy)
                if (len > 1f) {
                    dx /= len
                    dy /= len
                }
                nx = dx
                ny = dy
                invalidate()
                onMove?.invoke(nx, ny)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                active = false
                nx = 0f
                ny = 0f
                invalidate()
                onMove?.invoke(0f, 0f)
            }
        }
        return true
    }
}
