package com.example.cellradar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class RadarView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    data class Signal(

        val label: String,

        val rsrp: Int,

        val distance: Int
    )

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private var signals =
        listOf<Signal>()

    fun update(
        data: List<Signal>
    ) {

        signals = data

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        canvas.drawColor(Color.BLACK)

        val cx = width / 2f

        val cy = height / 2f

        val maxRadius =
            min(cx, cy) * 0.90f

        drawRadarGrid(
            canvas,
            cx,
            cy,
            maxRadius
        )

        drawSweep(
            canvas,
            cx,
            cy,
            maxRadius
        )

        drawSignals(
            canvas,
            cx,
            cy,
            maxRadius
        )
    }

    private fun drawRadarGrid(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        maxRadius: Float
    ) {

        paint.color = Color.RED

        paint.style = Paint.Style.STROKE

        paint.strokeWidth = 2f

        val ranges =
            listOf(
                500,
                1000,
                2000,
                3000,
                4000,
                5000
            )

        for (range in ranges) {

            val radius =
                (range / 5000f) * maxRadius

            canvas.drawCircle(
                cx,
                cy,
                radius,
                paint
            )

            paint.style = Paint.Style.FILL

            paint.textSize = 24f

            canvas.drawText(
                "${range}m",
                cx + radius - 70,
                cy,
                paint
            )

            paint.style = Paint.Style.STROKE
        }

        canvas.drawLine(
            cx - maxRadius,
            cy,
            cx + maxRadius,
            cy,
            paint
        )

        canvas.drawLine(
            cx,
            cy - maxRadius,
            cx,
            cy + maxRadius,
            paint
        )

        paint.style = Paint.Style.FILL

        canvas.drawCircle(
            cx,
            cy,
            10f,
            paint
        )
    }

    private fun drawSweep(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        maxRadius: Float
    ) {

        val angle =
            (
                    System.currentTimeMillis()
                            / 5 % 360
                    ).toDouble()

        paint.color = Color.RED

        paint.strokeWidth = 4f

        canvas.drawLine(
            cx,
            cy,
            cx + (
                    maxRadius *
                            cos(
                                Math.toRadians(angle)
                            )
                    ).toFloat(),
            cy + (
                    maxRadius *
                            sin(
                                Math.toRadians(angle)
                            )
                    ).toFloat(),
            paint
        )
    }

    private fun drawSignals(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        maxRadius: Float
    ) {

        paint.style = Paint.Style.FILL

        paint.textSize = 22f

        signals.forEachIndexed { index, signal ->

            val angle =
                index * (
                        360f /
                                max(
                                    1,
                                    signals.size
                                )
                        )

            val radians =
                Math.toRadians(
                    angle.toDouble()
                )

            val radius =
                (
                        signal.distance
                            .coerceAtMost(5000)
                                / 5000f
                        ) * maxRadius

            val x =
                cx + (
                        radius *
                                cos(radians)
                        ).toFloat()

            val y =
                cy + (
                        radius *
                                sin(radians)
                        ).toFloat()

            paint.color = Color.RED

            canvas.drawCircle(
                x,
                y,
                14f,
                paint
            )

            canvas.drawText(
                "${signal.distance}m",
                x + 16,
                y,
                paint
            )

            canvas.drawText(
                "${signal.rsrp}dBm",
                x + 16,
                y + 24,
                paint
            )
        }
    }
}