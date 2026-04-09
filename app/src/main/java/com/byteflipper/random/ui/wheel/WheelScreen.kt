package com.byteflipper.random.ui.wheel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.confetti.ConfettiEffect
import com.byteflipper.confetti.ConfettiMode
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.wheel.components.WheelEditorSheet
import com.byteflipper.random.ui.wheel.components.WheelFabControls
import com.byteflipper.random.ui.wheel.components.WheelMainContent
import com.byteflipper.random.ui.wheel.components.WheelSettingsSheet
import com.byteflipper.random.utils.findActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.res.stringResource

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
    val controller = rememberWheelScreenController()
    val visibleItems = rememberWheelVisibleItems(uiState)
    val currentSectorText = rememberCurrentSectorText(
        rotation = controller.rotationAnim.value,
        visibleItems = visibleItems
    )

    val allOptionsUsedText = stringResource(R.string.wheel_all_options_used)
    val minItemsText = stringResource(R.string.wheel_min_items)

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
            currentSectorText = currentSectorText,
            rotation = controller.rotationAnim.value,
            onSpinRequest = {
                controller.spin(
                    uiState = uiState,
                    settings = settings,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    view = view,
                    hapticsManager = hapticsManager,
                    allOptionsUsedText = allOptionsUsedText,
                    minItemsText = minItemsText
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
