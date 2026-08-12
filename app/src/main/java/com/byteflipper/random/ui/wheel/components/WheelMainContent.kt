package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.ui.wheel.WHEEL_MIN_ITEMS
import com.byteflipper.random.ui.wheel.WheelSector
import com.byteflipper.random.ui.wheel.WheelUiState
import kotlinx.coroutines.launch

private val MAX_WHEEL_SIZE = 320.dp
private val WHEEL_MARGIN = 16.dp

/** Keeps landscape text clear of the FAB column at the right edge. */
private val FAB_CLEARANCE = 88.dp

@Composable
internal fun WheelMainContent(
    modifier: Modifier = Modifier,
    uiState: WheelUiState,
    sectors: List<WheelSector>,
    currentSectorText: State<String>,
    rotationProvider: () -> Float,
    fadingIndexProvider: () -> Int,
    fadeProgressProvider: () -> Float,
    collapsingIndexProvider: () -> Int,
    collapseWeightProvider: () -> Float,
    highlightIndexProvider: () -> Int,
    highlightStrengthProvider: () -> Float,
    onDragStateChange: (Boolean) -> Unit,
    onRotate: (deltaDegrees: Float) -> Unit,
    onFlingSpin: suspend (velocityDegreesPerSecond: Float) -> Unit,
    onSpinRequest: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val visibleItems = remember(uiState.items, uiState.excludedIndices) {
        uiState.items.filterIndexed { index, _ -> index !in uiState.excludedIndices }
    }
    val wheelDescription = stringResource(R.string.wheel_a11y_wheel, visibleItems.size)
    val spinActionLabel = stringResource(R.string.wheel_a11y_spin)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // In landscape the wheel takes the whole height and the weighted panels around it collapse
        // to nothing, hiding the winner, so there the layout becomes a row.
        val isWide = maxWidth > maxHeight
        val availableForWheel = if (isWide) maxHeight else maxWidth
        val wheelSize = minOf(availableForWheel - WHEEL_MARGIN * 2, MAX_WHEEL_SIZE)

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SpinnableWheel(
                        uiState = uiState,
                        sectors = sectors,
                        rotationProvider = rotationProvider,
                        fadingIndexProvider = fadingIndexProvider,
                        fadeProgressProvider = fadeProgressProvider,
                        collapsingIndexProvider = collapsingIndexProvider,
                        collapseWeightProvider = collapseWeightProvider,
                        highlightIndexProvider = highlightIndexProvider,
                        highlightStrengthProvider = highlightStrengthProvider,
                        onDragStateChange = onDragStateChange,
                        wheelDescription = wheelDescription,
                        spinActionLabel = spinActionLabel,
                        size = wheelSize,
                        onRotate = onRotate,
                        onFling = { velocity -> scope.launch { onFlingSpin(velocity) } },
                        onSpin = { scope.launch { onSpinRequest() } }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = FAB_CLEARANCE),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WheelResultPanel(
                        isSpinning = uiState.isSpinning,
                        lastResult = uiState.lastResult,
                        currentSectorText = currentSectorText
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    WheelStatusPanel(
                        visibleItemsCount = visibleItems.size,
                        noRepeats = uiState.noRepeats,
                        excludedCount = uiState.excludedIndices.size
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WheelResultPanel(
                    isSpinning = uiState.isSpinning,
                    lastResult = uiState.lastResult,
                    currentSectorText = currentSectorText,
                    modifier = Modifier.weight(1f)
                )

                SpinnableWheel(
                    uiState = uiState,
                    sectors = sectors,
                    rotationProvider = rotationProvider,
                    fadingIndexProvider = fadingIndexProvider,
                    fadeProgressProvider = fadeProgressProvider,
                    collapsingIndexProvider = collapsingIndexProvider,
                    collapseWeightProvider = collapseWeightProvider,
                    highlightIndexProvider = highlightIndexProvider,
                    highlightStrengthProvider = highlightStrengthProvider,
                    onDragStateChange = onDragStateChange,
                    wheelDescription = wheelDescription,
                    spinActionLabel = spinActionLabel,
                    size = wheelSize,
                    onRotate = onRotate,
                    onFling = { velocity -> scope.launch { onFlingSpin(velocity) } },
                    onSpin = { scope.launch { onSpinRequest() } }
                )

                WheelStatusPanel(
                    visibleItemsCount = visibleItems.size,
                    noRepeats = uiState.noRepeats,
                    excludedCount = uiState.excludedIndices.size,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpinnableWheel(
    uiState: WheelUiState,
    sectors: List<WheelSector>,
    rotationProvider: () -> Float,
    fadingIndexProvider: () -> Int,
    fadeProgressProvider: () -> Float,
    collapsingIndexProvider: () -> Int,
    collapseWeightProvider: () -> Float,
    highlightIndexProvider: () -> Int,
    highlightStrengthProvider: () -> Float,
    onDragStateChange: (Boolean) -> Unit,
    wheelDescription: String,
    spinActionLabel: String,
    size: Dp,
    onRotate: (deltaDegrees: Float) -> Unit,
    onFling: (velocityDegreesPerSecond: Float) -> Unit,
    onSpin: () -> Unit
) {
    FortuneWheel(
        sectors = sectors,
        rotationProvider = rotationProvider,
        wheelDescription = wheelDescription,
        spinActionLabel = spinActionLabel,
        size = size,
        fadingIndexProvider = fadingIndexProvider,
        fadeProgressProvider = fadeProgressProvider,
        collapsingIndexProvider = collapsingIndexProvider,
        collapseWeightProvider = collapseWeightProvider,
        highlightIndexProvider = highlightIndexProvider,
        highlightStrengthProvider = highlightStrengthProvider,
        gesturesEnabled = !uiState.isSpinning,
        onDragStateChange = onDragStateChange,
        onRotate = onRotate,
        onFling = onFling,
        onClick = {
            if (!uiState.isSpinning && uiState.items.size >= WHEEL_MIN_ITEMS) {
                onSpin()
            }
        }
    )
}
