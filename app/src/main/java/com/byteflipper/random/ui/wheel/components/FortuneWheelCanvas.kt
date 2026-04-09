package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kotlin.math.min

@Composable
internal fun FortuneWheelCanvas(
    visibleItems: List<Pair<Int, String>>,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    val textPaint = rememberWheelTextPaint(visibleItems.size)

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasSize = min(size.width, size.height)
        val radius = canvasSize / 2f * 0.88f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawWheelBackdrop(center = center, radius = radius)

        if (visibleItems.isEmpty()) {
            drawEmptyWheel(center = center, radius = radius)
            return@Canvas
        }

        drawWheelSectors(
            visibleItems = visibleItems,
            center = center,
            radius = radius,
            rotation = rotation,
            textPaint = textPaint
        )

        drawWheelHub(center = center, radius = radius)
    }
}
