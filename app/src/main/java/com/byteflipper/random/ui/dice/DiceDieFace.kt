package com.byteflipper.random.ui.dice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min

@Composable
fun DiceDieFace(value: Int, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = min(w, h)
        val corner = s * 0.15f

        val darkColor = Color(
            red = (color.red * 0.8f).coerceIn(0f, 1f),
            green = (color.green * 0.8f).coerceIn(0f, 1f),
            blue = (color.blue * 0.8f).coerceIn(0f, 1f)
        )

        val lightColor = Color(
            red = (color.red * 1.2f).coerceIn(0f, 1f),
            green = (color.green * 1.2f).coerceIn(0f, 1f),
            blue = (color.blue * 1.2f).coerceIn(0f, 1f)
        )

        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.3f),
                    Color.Black.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(w/2 + 6f, h/2 + 6f),
                radius = s * 0.7f
            ),
            topLeft = androidx.compose.ui.geometry.Offset(2f, 2f),
            size = androidx.compose.ui.geometry.Size(w + 4f, h + 4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner + 2f, corner + 2f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(lightColor, color, darkColor),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(w, h)
            ),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        drawRoundRect(
            color = darkColor.copy(alpha = 0.3f),
            topLeft = androidx.compose.ui.geometry.Offset(2f, 2f),
            size = androidx.compose.ui.geometry.Size(w - 4f, h - 4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner - 2f, corner - 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )

        drawDots(value, s, w, h)
    }
}

private fun DrawScope.drawDots(value: Int, s: Float, w: Float, h: Float) {
    val margin = s * 0.24f
    val cx = w / 2f
    val cy = h / 2f
    val left = margin
    val right = w - margin
    val top = margin
    val bottom = h - margin
    val pipR = s * 0.08f

    fun drawDot(x: Float, y: Float) {
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(x + 2f, y + 2f),
                radius = pipR * 1.2f
            ),
            radius = pipR * 1.2f,
            center = androidx.compose.ui.geometry.Offset(x + 2f, y + 2f)
        )

        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = pipR * 1.1f,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )

        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFAFAFA),
                    Color(0xFFE0E0E0),
                    Color(0xFFBDBDBD)
                ),
                center = androidx.compose.ui.geometry.Offset(x, y),
                radius = pipR
            ),
            radius = pipR,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )

        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(x - pipR * 0.3f, y - pipR * 0.3f),
                radius = pipR * 0.5f
            ),
            radius = pipR * 0.4f,
            center = androidx.compose.ui.geometry.Offset(x - pipR * 0.3f, y - pipR * 0.3f)
        )
    }

    when (value.coerceIn(1, 6)) {
        1 -> drawDot(cx, cy)
        2 -> { drawDot(left, top); drawDot(right, bottom) }
        3 -> { drawDot(left, top); drawDot(cx, cy); drawDot(right, bottom) }
        4 -> { drawDot(left, top); drawDot(right, top); drawDot(left, bottom); drawDot(right, bottom) }
        5 -> { drawDot(left, top); drawDot(right, top); drawDot(cx, cy); drawDot(left, bottom); drawDot(right, bottom) }
        6 -> { drawDot(left, top); drawDot(left, cy); drawDot(left, bottom); drawDot(right, top); drawDot(right, cy); drawDot(right, bottom) }
    }
}


