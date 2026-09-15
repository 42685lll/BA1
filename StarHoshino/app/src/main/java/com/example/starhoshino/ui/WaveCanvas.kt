package com.example.starhoshino.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Random

class WaveCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 1f * resources.displayMetrics.density
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val lineCount = 21
    private val lineThickness = 1f * resources.displayMetrics.density
    private val lineSpacing = 1f * resources.displayMetrics.density
    private val totalLineWidth = lineCount * lineThickness + (lineCount - 1) * lineSpacing
    private val random = Random()
    private val handler = Handler(Looper.getMainLooper())
    private var isSpeaking = false
    private var isUserInterrupt = false
    private var amplitudes = FloatArray(lineCount) { 6f + random.nextFloat() * 2f }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isUserInterrupt) {
                amplitudes.fill(6f + random.nextFloat() * 2f)
                isUserInterrupt = false
            } else if (isSpeaking) {
                for (i in 0 until lineCount) {
                    val target = 6f + random.nextFloat() * 16f
                    amplitudes[i] = amplitudes[i] * 0.6f + target * 0.4f
                }
            } else {
                for (i in 0 until lineCount) {
                    amplitudes[i] = 6f + random.nextFloat() * 2f
                }
            }
            invalidate()
            handler.postDelayed(this, 100)
        }
    }

    init { handler.post(tickRunnable) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        val startX = (width - totalLineWidth) / 2f
        for (i in 0 until lineCount) {
            val x = startX + i * (lineThickness + lineSpacing)
            val h = amplitudes[i]
            canvas.drawRect(x, centerY - h / 2f, x + lineThickness, centerY + h / 2f, paint)
        }
    }

    fun startSpeaking() { isSpeaking = true; isUserInterrupt = false }
    fun stopSpeaking() { isSpeaking = false }
    fun onUserInterrupt() { isUserInterrupt = true; isSpeaking = false }
    fun release() { handler.removeCallbacks(tickRunnable) }
}
