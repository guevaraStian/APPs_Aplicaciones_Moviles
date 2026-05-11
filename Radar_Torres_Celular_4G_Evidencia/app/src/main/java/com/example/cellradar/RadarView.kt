package com.example.cellradar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class RadarView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    data class Signal(
        val name: String,
        val distance: Int,
        val angle: Float
    )

    private var signals = listOf<Signal>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun update(data: List<Signal>) {
        signals = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        val cx = width / 2f
        val cy = height / 2f
        val maxR = min(cx, cy) * 0.9f

        canvas.drawColor(Color.BLACK)

        paint.style = Paint.Style.STROKE
        paint.color = Color.RED

        for (i in 1..7) {
            canvas.drawCircle(cx, cy, (i / 7f) * maxR, paint)
        }

        // 🔵 cardinales en blanco
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 42f

        canvas.drawText("N", cx - 10f, cy - maxR + 50f, paint)
        canvas.drawText("S", cx - 10f, cy + maxR - 10f, paint)
        canvas.drawText("E", cx + maxR - 30f, cy, paint)
        canvas.drawText("O", cx - maxR + 20f, cy, paint)

        // 🔴 antenas
        paint.color = Color.RED
        paint.textSize = 28f

        signals.forEach {

            val rad = Math.toRadians(it.angle.toDouble())
            val r = (it.distance.coerceAtMost(7000) / 7000f) * maxR

            val x = cx + (r * cos(rad)).toFloat()
            val y = cy + (r * sin(rad)).toFloat()

            canvas.drawCircle(x, y, 12f, paint)

            paint.textSize = 30f
            canvas.drawText(it.name, x + 15f, y, paint)
        }
    }
}


