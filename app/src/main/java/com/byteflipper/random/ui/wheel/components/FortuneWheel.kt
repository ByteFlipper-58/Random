package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// 16 vibrant colors with better contrast
private val wheelColors = listOf(
    Color(0xFFE53935), // Red
    Color(0xFF1E88E5), // Blue
    Color(0xFF43A047), // Green
    Color(0xFFFB8C00), // Orange
    Color(0xFF8E24AA), // Purple
    Color(0xFF00ACC1), // Cyan
    Color(0xFFD81B60), // Pink
    Color(0xFFFDD835), // Yellow
    Color(0xFF5E35B1), // Deep Purple
    Color(0xFF039BE5), // Light Blue
    Color(0xFF7CB342), // Light Green
    Color(0xFFFF7043), // Deep Orange
    Color(0xFF3949AB), // Indigo
    Color(0xFF26A69A), // Teal
    Color(0xFFAB47BC), // Light Purple
    Color(0xFF66BB6A), // Green 400
)

@Composable
fun FortuneWheel(
    items: List<String>,
    excludedIndices: Set<Int>,
    rotation: Float,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    onClick: () -> Unit = {}
) {
    val density = LocalDensity.current
    
    val visibleItems = items.mapIndexedNotNull { index, item ->
        if (index in excludedIndices) null else index to item
    }
    
    val textPaint = remember(visibleItems.size) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            val shadowRadius = if (visibleItems.size > 10) 2f else 4f
            setShadowLayer(shadowRadius, 1f, 1f, android.graphics.Color.argb(180, 0, 0, 0))
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasSize = min(this.size.width, this.size.height)
            val radius = canvasSize / 2f * 0.88f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer glow effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.3f),
                        Color(0xFFFFD700).copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.25f
                ),
                radius = radius * 1.2f,
                center = center
            )

            // Drop shadow (larger, softer)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.25f),
                        Color.Black.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(center.x + 6f, center.y + 8f),
                    radius = radius * 1.1f
                ),
                radius = radius + 15f,
                center = Offset(center.x + 6f, center.y + 8f)
            )

            if (visibleItems.isEmpty()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.2f)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
                return@Canvas
            }

            val sweepAngle = 360f / visibleItems.size

            // Outer decorative ring - premium gold with shimmer effect
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFFE55C), // Bright gold
                        Color(0xFFDAA520), // Goldenrod
                        Color(0xFFFFD700), // Gold
                        Color(0xFFB8860B), // Dark goldenrod
                        Color(0xFFFFE55C),
                        Color(0xFFFFF8DC), // Cornsilk highlight
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

            // Inner gold ring
            drawCircle(
                color = Color(0xFFB8860B),
                radius = radius + 1f,
                center = center,
                style = Stroke(width = 3f)
            )

            rotate(degrees = rotation, pivot = center) {
                visibleItems.forEachIndexed { visualIndex, (_, item) ->
                    val startAngle = visualIndex * sweepAngle - 90f
                    val baseColor = wheelColors[visualIndex % wheelColors.size]
                    
                    // Darker edge color for depth
                    val darkColor = Color(
                        red = (baseColor.red * 0.7f).coerceIn(0f, 1f),
                        green = (baseColor.green * 0.7f).coerceIn(0f, 1f),
                        blue = (baseColor.blue * 0.7f).coerceIn(0f, 1f),
                        alpha = 1f
                    )

                    // Draw sector with gradient for 3D effect
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

                    // Divider lines with gold tips
                    val lineAngle = Math.toRadians((startAngle).toDouble())
                    val innerRadius = if (visibleItems.size > 8) 25f else 30f
                    val lineStartX = center.x + innerRadius * cos(lineAngle).toFloat()
                    val lineStartY = center.y + innerRadius * sin(lineAngle).toFloat()
                    val lineEndX = center.x + radius * cos(lineAngle).toFloat()
                    val lineEndY = center.y + radius * sin(lineAngle).toFloat()
                    
                    // Main divider line
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
                        strokeWidth = if (visibleItems.size > 12) 1.5f else 2f,
                        cap = StrokeCap.Round
                    )

                    // Draw text - adaptive sizing
                    val textAngle = startAngle + sweepAngle / 2
                    val textRadius = when {
                        visibleItems.size <= 4 -> radius * 0.52f
                        visibleItems.size <= 8 -> radius * 0.55f
                        visibleItems.size <= 12 -> radius * 0.58f
                        else -> radius * 0.60f
                    }
                    val textX = center.x + textRadius * cos(Math.toRadians(textAngle.toDouble())).toFloat()
                    val textY = center.y + textRadius * sin(Math.toRadians(textAngle.toDouble())).toFloat()

                    val maxTextWidth = when {
                        visibleItems.size <= 4 -> radius * 0.45f
                        visibleItems.size <= 6 -> radius * 0.40f
                        visibleItems.size <= 8 -> radius * 0.35f
                        visibleItems.size <= 12 -> radius * 0.28f
                        else -> radius * 0.22f
                    }
                    
                    val textSizePx = with(density) { 
                        val baseDp = when {
                            visibleItems.size <= 3 -> 16.dp
                            visibleItems.size <= 5 -> 14.dp
                            visibleItems.size <= 8 -> 12.dp
                            visibleItems.size <= 12 -> 10.dp
                            else -> 8.dp
                        }
                        baseDp.toPx()
                    }
                    textPaint.textSize = textSizePx

                    val displayText = run {
                        if (textPaint.measureText(item) <= maxTextWidth) {
                            item
                        } else {
                            var truncated = item
                            while (textPaint.measureText("$truncated…") > maxTextWidth && truncated.length > 1) {
                                truncated = truncated.dropLast(1)
                            }
                            if (truncated.isEmpty()) item.take(2) + "…" else "$truncated…"
                        }
                    }

                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.rotate(
                        textAngle + 90f,
                        textX,
                        textY
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        displayText,
                        textX,
                        textY + textSizePx / 3,
                        textPaint
                    )
                    drawContext.canvas.nativeCanvas.restore()
                }
            }

            // Center hub - multi-layered premium look
            // Outer hub ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFFE55C),
                        Color(0xFFDAA520),
                        Color(0xFFFFD700),
                        Color(0xFFB8860B),
                        Color(0xFFFFE55C)
                    ),
                    center = center
                ),
                radius = radius * 0.20f,
                center = center
            )
            
            // Hub shadow ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = radius * 0.17f,
                center = Offset(center.x + 1f, center.y + 2f)
            )
            
            // Main hub body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF8DC), // Light center
                        Color(0xFFFFD700), // Gold
                        Color(0xFFB8860B), // Dark gold edge
                    ),
                    center = center,
                    radius = radius * 0.16f
                ),
                radius = radius * 0.15f,
                center = center
            )
            
            // Inner white highlight (centered)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        Color(0xFFFFF8E1).copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.10f
                ),
                radius = radius * 0.08f,
                center = center
            )
            
            // Small center dot
            drawCircle(
                color = Color(0xFFB8860B),
                radius = radius * 0.025f,
                center = center
            )
        }

        // Premium Pointer with 3D effect
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 8.dp)
                .size(32.dp, 44.dp)
        ) {
            val w = this.size.width
            val h = this.size.height
            
            // Shadow
            val shadowPath = Path().apply {
                moveTo(w / 2 + 3f, h)
                lineTo(3f, 10f)
                lineTo(w / 2 + 3f, 0f)
                lineTo(w - 3f + 3f, 10f)
                close()
            }
            drawPath(shadowPath, Color.Black.copy(alpha = 0.3f))
            
            // Main pointer body with gradient
            val pointerPath = Path().apply {
                moveTo(w / 2, h)
                lineTo(3f, 10f)
                lineTo(w / 2, 0f)
                lineTo(w - 3f, 10f)
                close()
            }
            drawPath(
                pointerPath, 
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF5252), // Light red
                        Color(0xFFE53935), // Red
                        Color(0xFFB71C1C)  // Dark red
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )
            
            // Left edge highlight
            val leftHighlight = Path().apply {
                moveTo(w / 2, h)
                lineTo(3f, 10f)
                lineTo(w / 2, 0f)
                lineTo(w / 2 - 2f, 5f)
                close()
            }
            drawPath(leftHighlight, Color.White.copy(alpha = 0.25f))
            
            // Border
            drawPath(
                pointerPath, 
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFB8860B),
                        Color(0xFFFFD700)
                    )
                ),
                style = Stroke(width = 2.5f)
            )
            
            // Inner bright line
            val innerLine = Path().apply {
                moveTo(w / 2, h - 8f)
                lineTo(8f, 12f)
                lineTo(w / 2, 4f)
                lineTo(w - 8f, 12f)
                close()
            }
            drawPath(innerLine, Color.White.copy(alpha = 0.15f), style = Stroke(width = 1f))
        }
    }
}
