package com.example.escucharbajosvolumenes

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GraphView(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {

    private val amplitudes = mutableListOf<Float>()

    private val linePaint = Paint().apply {

        color = Color.RED

        strokeWidth = 5f

        style = Paint.Style.STROKE

        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {

        color = Color.DKGRAY

        strokeWidth = 1f
    }

    private val textPaint = Paint().apply {

        color = Color.RED

        textSize = 28f

        isAntiAlias = true
    }

    fun addAmplitude(value: Float) {

        amplitudes.add(value)

        if (amplitudes.size > 200) {

            amplitudes.removeAt(0)
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        canvas.drawColor(Color.BLACK)

        val w = width.toFloat()

        val h = height.toFloat()

        // eje Y decibeles
        for (i in 0..10) {

            val y = h * i / 10

            canvas.drawLine(
                0f,
                y,
                w,
                y,
                gridPaint
            )

            val db = 100 - (i * 10)

            canvas.drawText(
                "${db}dB",
                10f,
                y + 20,
                textPaint
            )
        }

        // eje X frecuencias
        val hzLabels = listOf(
            "20Hz",
            "50Hz",
            "100Hz",
            "500Hz",
            "1k",
            "5k",
            "10k",
            "20k"
        )

        for (i in hzLabels.indices) {

            val x =
                (w / (hzLabels.size - 1)) * i

            canvas.drawLine(
                x,
                0f,
                x,
                h,
                gridPaint
            )

            canvas.drawText(
                hzLabels[i],
                x,
                h - 10,
                textPaint
            )
        }

        if (amplitudes.size < 2) return

        val stepX = w / amplitudes.size

        val path = Path()

        for (i in amplitudes.indices) {

            val x = i * stepX

            val y =
                h - ((amplitudes[i] / 100f) * h)

            if (i == 0) {

                path.moveTo(x, y)

            } else {

                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, linePaint)
    }
}