package com.byteflipper.random.ui.wheel.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawWheelBackdrop(center: Offset, radius: Float) {
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
}

internal fun DrawScope.drawEmptyWheel(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.2f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

internal fun DrawScope.drawWheelHub(center: Offset, radius: Float) {
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

    drawCircle(
        color = Color.Black.copy(alpha = 0.2f),
        radius = radius * 0.17f,
        center = Offset(center.x + 1f, center.y + 2f)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF8DC),
                Color(0xFFFFD700),
                Color(0xFFB8860B),
            ),
            center = center,
            radius = radius * 0.16f
        ),
        radius = radius * 0.15f,
        center = center
    )

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

    drawCircle(
        color = Color(0xFFB8860B),
        radius = radius * 0.025f,
        center = center
    )
}
