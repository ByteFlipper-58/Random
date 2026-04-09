package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun FortuneWheelPointer(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val shadowPath = Path().apply {
            moveTo(w / 2 + 3f, h)
            lineTo(3f, 10f)
            lineTo(w / 2 + 3f, 0f)
            lineTo(w, 10f)
            close()
        }
        drawPath(shadowPath, Color.Black.copy(alpha = 0.3f))

        val pointerPath = Path().apply {
            moveTo(w / 2, h)
            lineTo(3f, 10f)
            lineTo(w / 2, 0f)
            lineTo(w - 3f, 10f)
            close()
        }
        drawPath(
            path = pointerPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF5252),
                    Color(0xFFE53935),
                    Color(0xFFB71C1C)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        val leftHighlight = Path().apply {
            moveTo(w / 2, h)
            lineTo(3f, 10f)
            lineTo(w / 2, 0f)
            lineTo(w / 2 - 2f, 5f)
            close()
        }
        drawPath(leftHighlight, Color.White.copy(alpha = 0.25f))

        drawPath(
            path = pointerPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFB8860B),
                    Color(0xFFFFD700)
                )
            ),
            style = Stroke(width = 2.5f)
        )

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
