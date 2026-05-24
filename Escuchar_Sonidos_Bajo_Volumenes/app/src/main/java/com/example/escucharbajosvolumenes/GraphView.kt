package com.example.escucharbajosvolumenes

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GraphView(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {

    private val points =
        mutableListOf<Pair<Float, Float>>()

    private val graphPaint =
        Paint().apply {

            color = Color.GREEN

            strokeWidth = 5f

            style = Paint.Style.STROKE

            isAntiAlias = true
        }

    private val axisPaint =
        Paint().apply {

            color = Color.WHITE

            strokeWidth = 3f

            textSize = 28f
        }

    fun addPoint(
        time: Float,
        db: Float
    ) {

        points.add(
            Pair(time, db)
        )

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        canvas.drawColor(Color.BLACK)

        val padding = 80f

        val graphWidth =
            width - padding * 2

        val graphHeight =
            height - padding * 2

        canvas.drawLine(
            padding,
            padding,
            padding,
            height - padding,
            axisPaint
        )

        canvas.drawLine(
            padding,
            height - padding,
            width - padding,
            height - padding,
            axisPaint
        )

        for (i in 0..120 step 10) {

            val y =
                height - padding -
                        (
                                i / 120f
                                ) * graphHeight

            canvas.drawText(
                "$i dB",
                10f,
                y,
                axisPaint
            )
        }

        if (points.size < 2) return

        val maxTime =
            points.last().first

        val path = Path()

        for (i in points.indices) {

            val x =
                padding +
                        (
                                points[i].first /
                                        maxTime
                                ) * graphWidth

            val y =
                height - padding -
                        (
                                points[i].second / 120f
                                ) * graphHeight

            if (i == 0) {

                path.moveTo(x, y)

            } else {

                path.lineTo(x, y)
            }
        }

        canvas.drawPath(
            path,
            graphPaint
        )

        canvas.drawText(
            "Tiempo (s)",
            width / 2f,
            height - 20f,
            axisPaint
        )

        canvas.drawText(
            "dB",
            width - 100f,
            40f,
            axisPaint
        )
    }
}