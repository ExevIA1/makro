package com.makro.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var startX = 0f
    private var startY = 0f
    private var endX   = 0f
    private var endY   = 0f
    private var isDrawing = false

    // Seçim dikdörtgeni boyası
    private val rectPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // Karartma boyası
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
    }

    // Köşe tutamaç boyası
    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                endX   = event.x
                endY   = event.y
                isDrawing = true
            }
            MotionEvent.ACTION_MOVE -> {
                endX = event.x
                endY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                endX = event.x
                endY = event.y
                isDrawing = false
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        if (startX == endX && startY == endY) return

        val left   = minOf(startX, endX)
        val top    = minOf(startY, endY)
        val right  = maxOf(startX, endX)
        val bottom = maxOf(startY, endY)

        // Seçim dışını karart
        canvas.drawRect(0f, 0f, width.toFloat(), top, dimPaint)
        canvas.drawRect(0f, top, left, bottom, dimPaint)
        canvas.drawRect(right, top, width.toFloat(), bottom, dimPaint)
        canvas.drawRect(0f, bottom, width.toFloat(), height.toFloat(), dimPaint)

        // Seçim çerçevesi
        canvas.drawRect(left, top, right, bottom, rectPaint)

        // 4 köşede tutamaç noktaları
        val r = 10f
        canvas.drawCircle(left, top, r, cornerPaint)
        canvas.drawCircle(right, top, r, cornerPaint)
        canvas.drawCircle(left, bottom, r, cornerPaint)
        canvas.drawCircle(right, bottom, r, cornerPaint)
    }

    // Seçilen alanı RectF olarak döndür
    fun getSelection(): RectF? {
        if (startX == endX || startY == endY) return null
        return RectF(
            minOf(startX, endX),
            minOf(startY, endY),
            maxOf(startX, endX),
            maxOf(startY, endY)
        )
    }

    fun hasSelection() = startX != endX && startY != endY
}
