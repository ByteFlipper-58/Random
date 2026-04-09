package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.byteflipper.random.ui.wheel.WheelUiState
import kotlinx.coroutines.launch

@Composable
internal fun WheelMainContent(
    modifier: Modifier = Modifier,
    uiState: WheelUiState,
    currentSectorText: String,
    rotation: Float,
    onSpinRequest: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val visibleItems = uiState.items.filterIndexed { index, _ -> index !in uiState.excludedIndices }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WheelResultPanel(
            isSpinning = uiState.isSpinning,
            lastResult = uiState.lastResult,
            currentSectorText = currentSectorText,
            modifier = Modifier.weight(1f)
        )

        FortuneWheel(
            items = uiState.items,
            excludedIndices = uiState.excludedIndices,
            rotation = rotation,
            size = 320.dp,
            onClick = {
                if (!uiState.isSpinning && uiState.items.size >= 2) {
                    scope.launch { onSpinRequest() }
                }
            }
        )

        WheelStatusPanel(
            visibleItemsCount = visibleItems.size,
            noRepeats = uiState.noRepeats,
            excludedCount = uiState.excludedIndices.size,
            modifier = Modifier.weight(1f)
        )
    }
}
