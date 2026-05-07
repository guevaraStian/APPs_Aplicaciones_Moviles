package com.example.cellradar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ErrorOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var error: String? = null

    private val paint = Paint().apply {
        color = Color.RED
        textSize = 34f
    }

    override fun onDraw(canvas: Canvas) {
        error?.let {
            canvas.drawColor(Color.argb(150, 0, 0, 0))
            canvas.drawText("ERROR:", 50f, 120f, paint)
            canvas.drawText(it, 50f, 180f, paint)
        }
    }
}