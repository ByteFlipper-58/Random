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

        // Every offset is a fraction of the pointer size rather than a pixel value, or the outline
        // and the bevel shrink to a hairline on dense screens.
        val edgeInset = w * WheelPointerRatios.EDGE_INSET_X
        val shoulderY = h * WheelPointerRatios.SHOULDER_Y
        val shadowOffset = w * WheelPointerRatios.SHADOW_OFFSET_X

        val shadowPath = Path().apply {
            moveTo(w / 2 + shadowOffset, h)
            lineTo(shadowOffset, shoulderY)
            lineTo(w / 2 + shadowOffset, 0f)
            lineTo(w, shoulderY)
            close()
        }
        drawPath(shadowPath, Color.Black.copy(alpha = 0.3f))

        val pointerPath = Path().apply {
            moveTo(w / 2, h)
            lineTo(edgeInset, shoulderY)
            lineTo(w / 2, 0f)
            lineTo(w - edgeInset, shoulderY)
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
            lineTo(edgeInset, shoulderY)
            lineTo(w / 2, 0f)
            lineTo(w / 2 - w * WheelPointerRatios.HIGHLIGHT_INSET_X, h * WheelPointerRatios.HIGHLIGHT_Y)
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
            style = Stroke(width = w * WheelPointerRatios.OUTLINE_WIDTH)
        )

        val innerInset = w * WheelPointerRatios.INNER_INSET_X
        val innerLine = Path().apply {
            moveTo(w / 2, h - h * WheelPointerRatios.INNER_BOTTOM_Y)
            lineTo(innerInset, h * WheelPointerRatios.INNER_SHOULDER_Y)
            lineTo(w / 2, h * WheelPointerRatios.INNER_TOP_Y)
            lineTo(w - innerInset, h * WheelPointerRatios.INNER_SHOULDER_Y)
            close()
        }
        drawPath(
            path = innerLine,
            color = Color.White.copy(alpha = 0.15f),
            style = Stroke(width = w * WheelPointerRatios.INNER_WIDTH)
        )
    }
}
