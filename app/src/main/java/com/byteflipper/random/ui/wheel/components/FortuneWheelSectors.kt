package com.byteflipper.random.ui.wheel.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.byteflipper.random.ui.wheel.WheelSector
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** What a used sector fades into. */
private val ExcludedSectorColor = Color(0xFF4A4A4A)

/** How much of the original color still shows through the grey. */
private const val EXCLUDED_COLOR_MIX = 0.88f

/** Lightening of the winning sector at the peak of the flash. */
private const val HIGHLIGHT_LIGHTEN = 0.55f

internal fun DrawScope.drawWheelSectors(
    sectors: List<WheelSector>,
    center: Offset,
    radius: Float,
    rotation: Float,
    textPaint: android.graphics.Paint,
    fadingIndex: Int,
    fadeProgress: Float,
    collapsingIndex: Int,
    collapseWeight: Float,
    highlightIndex: Int,
    highlightStrength: Float
) {
    // Angles come from weights rather than an equal split: a collapsing sector drops its weight to
    // zero and the rest take over its space smoothly, instead of the wheel snapping into a new
    // layout.
    val weights = sectors.map { sector ->
        if (sector.index == collapsingIndex) collapseWeight.coerceIn(0f, 1f) else 1f
    }
    val totalWeight = weights.sum().coerceAtLeast(0.0001f)

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
        radius = radius + radius * WheelDrawRatios.RIM_OFFSET,
        center = center,
        style = Stroke(width = radius * WheelDrawRatios.RIM_WIDTH)
    )

    drawCircle(
        color = Color(0xFFB8860B),
        radius = radius + radius * WheelDrawRatios.BORDER_OFFSET,
        center = center,
        style = Stroke(width = radius * WheelDrawRatios.BORDER_WIDTH)
    )

    rotate(degrees = rotation, pivot = center) {
        var cumulativeWeight = 0f

        sectors.forEachIndexed { sectorPosition, sector ->
            // The sector that has just been used greys out gradually; the older ones already are.
            val greyness = when {
                !sector.isExcluded -> 0f
                sector.index == fadingIndex -> fadeProgress
                else -> 1f
            }

            val startAngle = 360f * (cumulativeWeight / totalWeight) - 90f
            val sweepAngle = 360f * (weights[sectorPosition] / totalWeight)
            cumulativeWeight += weights[sectorPosition]

            drawWheelSector(
                sector = sector,
                sectorCount = sectors.size,
                greyness = greyness,
                highlight = if (sector.index == highlightIndex) highlightStrength else 0f,
                center = center,
                radius = radius,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                textPaint = textPaint
            )
        }
    }
}

private fun DrawScope.drawWheelSector(
    sector: WheelSector,
    sectorCount: Int,
    greyness: Float,
    highlight: Float,
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    textPaint: android.graphics.Paint
) {
    if (sweepAngle <= 0.01f) return

    // The color is keyed by item index, so the palette stays with the item even when used sectors
    // are taken off the wheel.
    val paletteColor = wheelColors[sector.index % wheelColors.size]
    val greyed = lerp(paletteColor, ExcludedSectorColor, greyness * EXCLUDED_COLOR_MIX)
    val baseColor = lerp(greyed, Color.White, highlight * HIGHLIGHT_LIGHTEN)
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
        itemCount = sectorCount,
        center = center,
        radius = radius,
        startAngle = startAngle
    )


    if (highlight > 0f) {
        drawWinnerArc(
            center = center,
            radius = radius,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            highlight = highlight
        )
    }

    // A label on a collapsing sector only gets in the way; it no longer fits anyway.
    if (sweepAngle < MIN_LABEL_SWEEP_DEGREES) return

    drawWheelText(
        item = sector.label,
        itemCount = sectorCount,
        greyness = greyness,
        center = center,
        radius = radius,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        textPaint = textPaint
    )
}

/** Bright arc along the outer edge of the winner: reads as a selection, not as a washed out color. */
private fun DrawScope.drawWinnerArc(
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    highlight: Float
) {
    val strokeWidth = radius * WheelDrawRatios.RIM_WIDTH * 0.75f
    val arcRadius = radius - strokeWidth / 2f

    drawArc(
        color = Color.White.copy(alpha = 0.85f * highlight),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = strokeWidth)
    )
}

private fun DrawScope.drawWheelDivider(
    itemCount: Int,
    center: Offset,
    radius: Float,
    startAngle: Float
) {
    val lineAngle = Math.toRadians(startAngle.toDouble())
    val innerRadius = radius * if (itemCount > 8) {
        WheelDrawRatios.DIVIDER_INNER_RADIUS_DENSE
    } else {
        WheelDrawRatios.DIVIDER_INNER_RADIUS
    }
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
        strokeWidth = radius * if (itemCount > 12) {
            WheelDrawRatios.DIVIDER_WIDTH_DENSE
        } else {
            WheelDrawRatios.DIVIDER_WIDTH
        },
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawWheelText(
    item: String,
    itemCount: Int,
    greyness: Float,
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
        maxTextWidth = availableTextWidth(radius, textRadius, sweepAngle),
        textPaint = textPaint
    )

    // A used label dims along with its sector but stays readable.
    textPaint.alpha = (255 * (1f - 0.45f * greyness)).toInt().coerceIn(0, 255)

    drawContext.canvas.nativeCanvas.save()
    drawContext.canvas.nativeCanvas.rotate(textAngle + 90f, textX, textY)
    drawContext.canvas.nativeCanvas.drawText(
        displayText,
        textX,
        textY + textPaint.textSize / 3,
        textPaint
    )
    drawContext.canvas.nativeCanvas.restore()
    textPaint.alpha = 255
}

/**
 * How much room a label actually has.
 *
 * The label runs across the radius, so it is bounded by two chords at [textRadius] from the center:
 * the angular width of the sector itself and the edge of the circle. A fixed fraction of the radius
 * would fit about seven characters even on a wheel of two sectors.
 */
private fun availableTextWidth(radius: Float, textRadius: Float, sweepAngle: Float): Float {
    val sectorChord = 2f * textRadius * sin(Math.toRadians(sweepAngle / 2.0)).toFloat()
    val circleChord = 2f * sqrt((radius * radius - textRadius * textRadius).coerceAtLeast(0f))

    return minOf(sectorChord, circleChord) * TEXT_WIDTH_MARGIN
}

/** Small margin so a label does not touch the dividers or the rim. */
private const val TEXT_WIDTH_MARGIN = 0.86f

/** Narrower than this a sector is left unlabeled: the text would be unreadable anyway. */
private const val MIN_LABEL_SWEEP_DEGREES = 6f

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
