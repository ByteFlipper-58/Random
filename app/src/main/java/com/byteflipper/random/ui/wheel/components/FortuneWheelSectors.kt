package com.byteflipper.random.ui.wheel.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

internal fun DrawScope.drawWheelSectors(
    visibleItems: List<Pair<Int, String>>,
    center: Offset,
    radius: Float,
    rotation: Float,
    textPaint: android.graphics.Paint
) {
    val sweepAngle = 360f / visibleItems.size

    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFFE55C),
                Color(0xFFDAA520),
                Color(0xFFFFD700),
                Color(0xFFB8860B),
                Color(0xFFFFE55C),
                Color(0xFFFFF8DC),
                Color(0xFFFFD700),
                Color(0xFFDAA520),
                Color(0xFFFFE55C)
            ),
            center = center
        ),
        radius = radius + 8f,
        center = center,
        style = Stroke(width = 16f)
    )

    drawCircle(
        color = Color(0xFFB8860B),
        radius = radius + 1f,
        center = center,
        style = Stroke(width = 3f)
    )

    rotate(degrees = rotation, pivot = center) {
        visibleItems.forEachIndexed { visualIndex, (_, item) ->
            drawWheelSector(
                visualIndex = visualIndex,
                item = item,
                itemCount = visibleItems.size,
                center = center,
                radius = radius,
                sweepAngle = sweepAngle,
                textPaint = textPaint
            )
        }
    }
}

private fun DrawScope.drawWheelSector(
    visualIndex: Int,
    item: String,
    itemCount: Int,
    center: Offset,
    radius: Float,
    sweepAngle: Float,
    textPaint: android.graphics.Paint
) {
    val startAngle = visualIndex * sweepAngle - 90f
    val baseColor = wheelColors[visualIndex % wheelColors.size]
    val darkColor = Color(
        red = (baseColor.red * 0.7f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.7f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.7f).coerceIn(0f, 1f),
        alpha = 1f
    )

    drawArc(
        brush = Brush.radialGradient(
            colors = listOf(baseColor, darkColor),
            center = center,
            radius = radius
        ),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )

    drawWheelDivider(
        itemCount = itemCount,
        center = center,
        radius = radius,
        startAngle = startAngle
    )

    drawWheelText(
        item = item,
        itemCount = itemCount,
        center = center,
        radius = radius,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        textPaint = textPaint
    )
}

private fun DrawScope.drawWheelDivider(
    itemCount: Int,
    center: Offset,
    radius: Float,
    startAngle: Float
) {
    val lineAngle = Math.toRadians(startAngle.toDouble())
    val innerRadius = if (itemCount > 8) 25f else 30f
    val lineStartX = center.x + innerRadius * cos(lineAngle).toFloat()
    val lineStartY = center.y + innerRadius * sin(lineAngle).toFloat()
    val lineEndX = center.x + radius * cos(lineAngle).toFloat()
    val lineEndY = center.y + radius * sin(lineAngle).toFloat()

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFD700).copy(alpha = 0.6f),
                Color.White.copy(alpha = 0.4f),
                Color(0xFFFFD700).copy(alpha = 0.6f)
            ),
            start = Offset(lineStartX, lineStartY),
            end = Offset(lineEndX, lineEndY)
        ),
        start = Offset(lineStartX, lineStartY),
        end = Offset(lineEndX, lineEndY),
        strokeWidth = if (itemCount > 12) 1.5f else 2f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawWheelText(
    item: String,
    itemCount: Int,
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    textPaint: android.graphics.Paint
) {
    val density = this
    val textAngle = startAngle + sweepAngle / 2
    val textRadius = when {
        itemCount <= 4 -> radius * 0.52f
        itemCount <= 8 -> radius * 0.55f
        itemCount <= 12 -> radius * 0.58f
        else -> radius * 0.60f
    }
    val textX = center.x + textRadius * cos(Math.toRadians(textAngle.toDouble())).toFloat()
    val textY = center.y + textRadius * sin(Math.toRadians(textAngle.toDouble())).toFloat()

    val maxTextWidth = when {
        itemCount <= 4 -> radius * 0.45f
        itemCount <= 6 -> radius * 0.40f
        itemCount <= 8 -> radius * 0.35f
        itemCount <= 12 -> radius * 0.28f
        else -> radius * 0.22f
    }

    textPaint.textSize = with(density) {
        when {
            itemCount <= 3 -> 16.dp.toPx()
            itemCount <= 5 -> 14.dp.toPx()
            itemCount <= 8 -> 12.dp.toPx()
            itemCount <= 12 -> 10.dp.toPx()
            else -> 8.dp.toPx()
        }
    }

    val displayText = truncateWheelText(
        text = item,
        maxTextWidth = maxTextWidth,
        textPaint = textPaint
    )

    drawContext.canvas.nativeCanvas.save()
    drawContext.canvas.nativeCanvas.rotate(textAngle + 90f, textX, textY)
    drawContext.canvas.nativeCanvas.drawText(
        displayText,
        textX,
        textY + textPaint.textSize / 3,
        textPaint
    )
    drawContext.canvas.nativeCanvas.restore()
}

private fun truncateWheelText(
    text: String,
    maxTextWidth: Float,
    textPaint: android.graphics.Paint
): String {
    if (textPaint.measureText(text) <= maxTextWidth) return text

    var truncated = text
    while (textPaint.measureText("$truncated…") > maxTextWidth && truncated.length > 1) {
        truncated = truncated.dropLast(1)
    }

    return if (truncated.isEmpty()) "${text.take(2)}…" else "$truncated…"
}
