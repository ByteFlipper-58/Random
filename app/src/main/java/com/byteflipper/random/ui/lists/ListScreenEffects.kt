package com.byteflipper.random.ui.lists

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.byteflipper.random.ui.common.CollectFeedbackEffects

@Composable
internal fun ListScreenEffects(
    uiState: ListUiState,
    controller: ListScreenController,
    viewModel: ListViewModel,
    onOpenListById: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context,
    hapticsManager: com.byteflipper.random.ui.components.HapticsManager?
) {
    LaunchedEffect(uiState.showSaveDialog, controller.pendingOpenPresetId) {
        val presetToOpen = controller.pendingOpenPresetId
        if (!uiState.showSaveDialog && presetToOpen != null) {
            controller.pendingOpenPresetId = null
            onOpenListById(presetToOpen)
        }
    }

    CollectFeedbackEffects(
        effects = viewModel.effects,
        snackbarHostState = snackbarHostState,
        context = context,
        hapticsManager = hapticsManager
    )
}
