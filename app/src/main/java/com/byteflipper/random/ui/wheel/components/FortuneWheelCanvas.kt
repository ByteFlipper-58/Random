package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.byteflipper.random.ui.wheel.WheelSector
import kotlin.math.min

@Composable
internal fun FortuneWheelCanvas(
    sectors: List<WheelSector>,
    rotationProvider: () -> Float,
    fadingIndexProvider: () -> Int,
    fadeProgressProvider: () -> Float,
    collapsingIndexProvider: () -> Int,
    collapseWeightProvider: () -> Float,
    highlightIndexProvider: () -> Int,
    highlightStrengthProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val textPaint = rememberWheelTextPaint(sectors.size)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Rotation and the transition values are read here, in the draw phase, so animation frames
        // never trigger recomposition.
        val rotation = rotationProvider()
        val canvasSize = min(size.width, size.height)
        val radius = canvasSize / 2f * 0.88f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawWheelBackdrop(center = center, radius = radius)

        if (sectors.isEmpty()) {
            drawEmptyWheel(center = center, radius = radius)
            return@Canvas
        }

        drawWheelSectors(
            sectors = sectors,
            center = center,
            radius = radius,
            rotation = rotation,
            textPaint = textPaint,
            fadingIndex = fadingIndexProvider(),
            fadeProgress = fadeProgressProvider(),
            collapsingIndex = collapsingIndexProvider(),
            collapseWeight = collapseWeightProvider(),
            highlightIndex = highlightIndexProvider(),
            highlightStrength = highlightStrengthProvider()
        )

        drawWheelHub(center = center, radius = radius)
    }
}
