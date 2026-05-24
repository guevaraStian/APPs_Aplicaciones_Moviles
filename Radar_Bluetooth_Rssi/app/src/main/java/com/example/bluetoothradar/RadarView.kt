package com.example.bluetoothradar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class RadarView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    data class DevicePoint(
        val angle: Float,
        val distanceKm: Float,
        val name: String,
        val address: String
    )

    val devices = mutableListOf<DevicePoint>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {

        canvas.drawColor(Color.BLACK)

        val cx = width / 2f
        val cy = height / 2f

        val maxRadius = width.coerceAtMost(height) / 2f

        // ================= RADAR CÍRCULOS =================
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        paint.strokeWidth = 2f
        paint.textSize = 22f

        for (i in 1..10) {
            val r = maxRadius * (i / 10f)
            canvas.drawCircle(cx, cy, r, paint)
            canvas.drawText("${i} km", cx + 10f, cy - r, paint)
        }

        // ================= DISPOSITIVOS =================
        for (d in devices) {

            val r = maxRadius * (d.distanceKm / 10f)

            val x = cx + r * cos(d.angle)
            val y = cy + r * sin(d.angle)

            // punto
            paint.style = Paint.Style.FILL
            paint.color = Color.RED
            canvas.drawCircle(x, y, 10f, paint)

            // etiqueta
            paint.textSize = 26f
            paint.textAlign = Paint.Align.CENTER
            paint.style = Paint.Style.FILL

            val label = "${d.name}\n${d.address}"

            canvas.drawText(label, x, y - 25f, paint)
        }
    }
}