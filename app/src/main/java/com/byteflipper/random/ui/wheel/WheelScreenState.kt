package com.byteflipper.random.ui.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal class WheelScreenController(
    val rotationAnim: Animatable<Float, *>
) {
    var showConfetti by mutableStateOf(false)
}

@Composable
internal fun rememberWheelScreenController(): WheelScreenController {
    return remember {
        WheelScreenController(rotationAnim = Animatable(0f))
    }
}

@Composable
internal fun rememberWheelVisibleItems(uiState: WheelUiState): List<String> {
    val visibleItems by remember(uiState.items, uiState.excludedIndices) {
        derivedStateOf {
            uiState.items.filterIndexed { index, _ -> index !in uiState.excludedIndices }
        }
    }
    return visibleItems
}

@Composable
internal fun rememberCurrentSectorText(
    rotation: Float,
    visibleItems: List<String>
): String {
    val currentSectorText by remember(rotation, visibleItems) {
        derivedStateOf {
            if (visibleItems.isEmpty()) return@derivedStateOf ""
            val itemCount = visibleItems.size
            val anglePerItem = 360f / itemCount
            val normalizedRotation = ((rotation % 360f) + 360f) % 360f
            val adjustedRotation = ((360f - normalizedRotation) % 360f + 360f) % 360f
            val sectorIndex = (adjustedRotation / anglePerItem).toInt() % itemCount
            visibleItems.getOrNull(sectorIndex) ?: ""
        }
    }
    return currentSectorText
}
