package com.byteflipper.random.ui.lists

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.showSaveDialog, controller.pendingOpenPresetId) {
        val presetToOpen = controller.pendingOpenPresetId
        if (!uiState.showSaveDialog && presetToOpen != null) {
            controller.pendingOpenPresetId = null
            onOpenListById(presetToOpen)
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.flushPendingSave()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.flushPendingSave()
        }
    }

    CollectFeedbackEffects(
        effects = viewModel.effects,
        snackbarHostState = snackbarHostState,
        context = context,
        hapticsManager = hapticsManager
    )
}
