package edu.uph.m24si2.smartpondapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.util.Locale

class SensorChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataPoints: List<Float> = listOf(25.2f, 24.3f, 24.0f, 25.5f, 26.8f, 28.5f, 26.0f, 25.2f, 25.8f, 26.2f, 25.9f)
    private var xLabels: List<String> = listOf("00:00", "06:00", "12:00", "18:00", "24:00")
    private var unitSuffix: String = "°C"

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E4EBF1")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A99AD")
        textSize = spToPx(10f)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0066FF")
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun setData(points: List<Float>, labels: List<String> = listOf("00:00", "06:00", "12:00", "18:00", "24:00"), suffix: String = "°C") {
        if (points.isNotEmpty()) {
            this.dataPoints = points
        }
        this.xLabels = labels
        this.unitSuffix = suffix
        invalidate()
    }

    private fun spToPx(sp: Float): Float {
        return sp * context.resources.displayMetrics.scaledDensity
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val paddingLeft = 55f
        val paddingRight = 20f
        val paddingTop = 20f
        val paddingBottom = 45f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        if (chartWidth <= 0 || chartHeight <= 0) return

        val minData = dataPoints.minOrNull() ?: 20f
        val maxData = dataPoints.maxOrNull() ?: 32f

        val minVal = if (minData < 22f) (minData - 1f) else 22f
        val maxVal = if (maxData > 32f) (maxData + 1f) else 32f
        val range = if (maxVal <= minVal) 10f else maxVal - minVal

        // Draw horizontal grid lines & Y labels (6 lines)
        val numYLines = 6
        for (i in 0 until numYLines) {
            val ratio = i.toFloat() / (numYLines - 1)
            val y = paddingTop + chartHeight * (1f - ratio)
            val value = minVal + range * ratio

            // Grid line
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)

            // Y Label
            val yLabelStr = String.format(Locale.getDefault(), "%.0f", value)
            canvas.drawText(yLabelStr, 10f, y + 4f, labelPaint)
        }

        // Draw X labels
        val xLabelCount = xLabels.size
        for (i in 0 until xLabelCount) {
            val ratio = if (xLabelCount > 1) i.toFloat() / (xLabelCount - 1) else 0f
            val x = paddingLeft + chartWidth * ratio
            val text = xLabels[i]
            val textWidth = labelPaint.measureText(text)
            canvas.drawText(text, x - textWidth / 2f, height - 10f, labelPaint)
        }

        if (dataPoints.size < 2) return

        // Compute point coordinates
        val pointsX = FloatArray(dataPoints.size)
        val pointsY = FloatArray(dataPoints.size)

        for (i in dataPoints.indices) {
            val xRatio = i.toFloat() / (dataPoints.size - 1)
            val yRatio = (dataPoints[i] - minVal) / range
            pointsX[i] = paddingLeft + chartWidth * xRatio
            pointsY[i] = paddingTop + chartHeight * (1f - yRatio)
        }

        // Build cubic path for smooth line
        val linePath = Path()
        val fillPath = Path()

        linePath.moveTo(pointsX[0], pointsY[0])
        fillPath.moveTo(pointsX[0], paddingTop + chartHeight)
        fillPath.lineTo(pointsX[0], pointsY[0])

        for (i in 0 until dataPoints.size - 1) {
            val p1x = pointsX[i]
            val p1y = pointsY[i]
            val p2x = pointsX[i + 1]
            val p2y = pointsY[i + 1]

            val controlX1 = p1x + (p2x - p1x) / 2f
            val controlY1 = p1y
            val controlX2 = p1x + (p2x - p1x) / 2f
            val controlY2 = p2y

            linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2x, p2y)
            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2x, p2y)
        }

        fillPath.lineTo(pointsX.last(), paddingTop + chartHeight)
        fillPath.close()

        // Gradient shader for fill
        val fillShader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + chartHeight,
            Color.parseColor("#4D0066FF"),
            Color.parseColor("#050066FF"),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = fillShader

        // Draw fill & line
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
    }
}
