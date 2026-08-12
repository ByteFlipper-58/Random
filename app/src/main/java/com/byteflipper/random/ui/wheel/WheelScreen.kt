package com.byteflipper.random.ui.wheel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.confetti.ConfettiEffect
import com.byteflipper.confetti.ConfettiMode
import com.byteflipper.random.data.settings.WheelUsedSectorStyle
import com.byteflipper.random.ui.common.rememberAnimationsEnabled
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.wheel.components.WheelEditorSheet
import com.byteflipper.random.ui.wheel.components.WheelFabControls
import com.byteflipper.random.ui.wheel.components.WheelMainContent
import com.byteflipper.random.ui.wheel.components.WheelSettingsSheet
import com.byteflipper.random.utils.findActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.res.stringResource
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(onBack: () -> Unit) {
    val view = LocalView.current
    val hapticsManager = LocalHapticsManager.current
    val viewModel: WheelViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val controller = rememberWheelScreenController(
        initialRotation = remember { viewModel.uiState.value.rotation }
    )
    val visibleItems = rememberWheelVisibleItems(uiState)
    // One layout for drawing, for the label under the pointer and for the ticks.
    val sectors = remember(uiState.items, uiState.excludedIndices, uiState.usedSectorStyle) {
        wheelSectors(
            items = uiState.items,
            excludedIndices = uiState.excludedIndices,
            usedSectorStyle = uiState.usedSectorStyle
        )
    }
    // A leaving sector stays in the drawn layout from exclusion until the collapse is over.
    val ghostIndex = pendingRemovalIndex(uiState, controller.completedRemovalIndex)
    val drawnSectors = rememberDrawnSectors(
        items = uiState.items,
        excludedIndices = uiState.excludedIndices,
        usedSectorStyle = uiState.usedSectorStyle,
        ghostIndex = ghostIndex
    )
    val currentSectorText = rememberCurrentSectorText(
        sectors = sectors,
        rotationProvider = { controller.rotation }
    )

    val screenScope = rememberCoroutineScope()

    // On stop the winner is highlighted; what follows depends on the used sector style.
    LaunchedEffect(uiState.lastResult, uiState.lastResultIndex, uiState.isSpinning) {
        val winnerIndex = uiState.lastResultIndex
        if (uiState.isSpinning || winnerIndex == null) return@LaunchedEffect

        val isPendingRemoval = winnerIndex in uiState.excludedIndices &&
            uiState.usedSectorStyle == WheelUsedSectorStyle.Remove

        if (isPendingRemoval) {
            // Flash, color back, grey out and leave as one sequence, so the order is guaranteed.
            controller.startRemoval(
                scope = screenScope,
                index = winnerIndex,
                sectorPosition = drawnSectors.indexOfFirst { it.index == winnerIndex },
                sectorCount = drawnSectors.size
            )
        } else {
            // "Keep as grey" mode: the winner stays colored and dims when the next spin starts.
            if (winnerIndex in uiState.excludedIndices) {
                controller.holdExcludeFade(winnerIndex)
            }
            controller.flashWinner(winnerIndex)
        }
    }

    val allOptionsUsedText = stringResource(R.string.wheel_all_options_used)
    val minItemsText = stringResource(R.string.wheel_min_items)
    val resetActionText = stringResource(R.string.reset)
    val animationsEnabled by rememberAnimationsEnabled()

    WheelSpinTicks(
        isSpinning = uiState.isSpinning,
        isDragging = controller.isDragging,
        sectorCount = sectors.size,
        hapticsEnabled = settings.hapticsEnabled,
        hapticsIntensity = settings.hapticsIntensity,
        hapticsManager = hapticsManager,
        rotationProvider = { controller.rotation }
    )

    WheelScaffold(
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            WheelFabControls(
                fabSize = settings.fabSize,
                onSettingsClick = { viewModel.onEvent(WheelUiEvent.ToggleSettingsSheet) },
                onEditClick = { viewModel.onEvent(WheelUiEvent.ToggleEditorSheet) }
            )
        }
    ) { innerPadding ->
        WheelMainContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            uiState = uiState,
            sectors = drawnSectors,
            currentSectorText = currentSectorText,
            rotationProvider = { controller.rotation },
            // While a sector is leaving its color is driven by excludeFade, otherwise the ghost
            // would appear grey in the same frame, before the flash.
            fadingIndexProvider = { if (ghostIndex >= 0) ghostIndex else controller.fadingIndex },
            fadeProgressProvider = { controller.excludeFade.value },
            collapsingIndexProvider = { ghostIndex },
            collapseWeightProvider = { controller.sectorCollapse.value },
            highlightIndexProvider = { controller.highlightIndex },
            highlightStrengthProvider = { controller.winnerHighlight.value },
            onDragStateChange = { dragging -> controller.isDragging = dragging },
            // Under the finger the rotation is written directly: this follows a gesture, not an
            // animation.
            onRotate = { delta -> controller.rotateBy(delta) },
            onFlingSpin = { velocity ->
                if (abs(velocity) >= WHEEL_FLING_THRESHOLD_DEG_PER_SEC) {
                    controller.spin(
                        uiState = uiState,
                        settings = settings,
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        view = view,
                        hapticsManager = hapticsManager,
                        allOptionsUsedText = allOptionsUsedText,
                        minItemsText = minItemsText,
                        resetActionText = resetActionText,
                        animationsEnabled = animationsEnabled,
                        pendingRemovalIndex = ghostIndex,
                        flingVelocityDegreesPerSecond = velocity
                    )
                } else {
                    // Too slow to be a spin: just fold the angle back into a single turn.
                    controller.normalizeRotation()
                }
            },
            onSpinRequest = {
                controller.spin(
                    uiState = uiState,
                    settings = settings,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    view = view,
                    hapticsManager = hapticsManager,
                    allOptionsUsedText = allOptionsUsedText,
                    minItemsText = minItemsText,
                    resetActionText = resetActionText,
                    animationsEnabled = animationsEnabled,
                    pendingRemovalIndex = ghostIndex
                )
            }
        )
    }

    WheelEditorSheet(
        visible = uiState.showEditorSheet,
        onDismiss = { viewModel.onEvent(WheelUiEvent.ToggleEditorSheet) },
        items = uiState.items,
        excludedIndices = uiState.excludedIndices,
        onUpdateItems = { viewModel.onEvent(WheelUiEvent.UpdateItems(it)) },
        presets = presets,
        onLoadPreset = { preset -> viewModel.onEvent(WheelUiEvent.LoadPreset(preset)) },
        hapticsEnabled = settings.hapticsEnabled,
        hapticsIntensity = settings.hapticsIntensity,
        onSaveAsPreset = { name -> viewModel.saveAsPreset(name) }
    )

    WheelSettingsSheet(
        visible = uiState.showSettingsSheet,
        onDismiss = { viewModel.onEvent(WheelUiEvent.ToggleSettingsSheet) },
        noRepeats = uiState.noRepeats,
        onNoRepeatsChange = { viewModel.onEvent(WheelUiEvent.SetNoRepeats(it)) },
        usedSectorStyle = uiState.usedSectorStyle,
        onUsedSectorStyleChange = { viewModel.onEvent(WheelUiEvent.SetUsedSectorStyle(it)) },
        spinDuration = uiState.spinDuration,
        onSpinDurationChange = { viewModel.onEvent(WheelUiEvent.SetSpinDuration(it)) },
        excludedCount = uiState.excludedIndices.size,
        totalCount = uiState.items.size,
        onReset = { viewModel.onEvent(WheelUiEvent.Reset) }
    )

    ConfettiEffect(
        trigger = controller.showConfetti,
        mode = ConfettiMode.SIDE_CANNONS,
        particleCount = 300,
        durationMs = 3500L,
        onComplete = { controller.showConfetti = false }
    )
}
