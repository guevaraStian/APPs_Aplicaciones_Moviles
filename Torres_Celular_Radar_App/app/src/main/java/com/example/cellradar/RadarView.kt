package com.example.cellradar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class RadarView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    data class Cell(
        val distance: Float,
        val angle: Float,
        val rsrp: Int,
        val name: String
    )

    var cells: List<Cell> = emptyList()

    private val paintGrid = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val paintPoint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val paintText = Paint().apply {
        color = Color.WHITE
        textSize = 22f
    }

    override fun onDraw(canvas: Canvas) {

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) * 0.85f

        for (i in 1..5) {
            canvas.drawCircle(cx, cy, radius * i / 5, paintGrid)
        }

        canvas.drawLine(cx - radius, cy, cx + radius, cy, paintGrid)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, paintGrid)

        if (cells.isEmpty()) {
            paintText.color = Color.YELLOW
            canvas.drawText("Sin celdas detectadas", 50f, 100f, paintText)
            return
        }

        cells.forEach {

            val r = min(it.distance / 12000f, 1f) * radius
            val rad = Math.toRadians(it.angle.toDouble())

            val x = cx + r * cos(rad).toFloat()
            val y = cy + r * sin(rad).toFloat()

            paintPoint.alpha = it.rsrp.coerceIn(40, 255)

            canvas.drawCircle(x, y, 10f, paintPoint)
            canvas.drawText("${it.name} (${it.rsrp})", x + 10, y, paintText)
        }
    }
}