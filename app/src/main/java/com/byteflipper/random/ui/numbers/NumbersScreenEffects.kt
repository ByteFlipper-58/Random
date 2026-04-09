package com.byteflipper.random.ui.numbers

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.byteflipper.random.ui.common.CollectFeedbackEffects

@Composable
internal fun NumbersScreenEffects(
    uiState: NumbersUiState,
    viewModel: NumbersViewModel,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context,
    hapticsManager: com.byteflipper.random.ui.components.HapticsManager?
) {
    LaunchedEffect(uiState.fromText, uiState.toText, uiState.allowRepetitions) {
        if (uiState.allowRepetitions) {
            viewModel.resetUsedNumbers(silent = true)
        } else {
            val from = uiState.fromText.trim().toIntOrNull()
            val to = uiState.toText.trim().toIntOrNull()
            if (from != null && to != null) {
                val range = if (from <= to) from..to else to..from
                viewModel.pruneUsedNumbersToRange(range)
            }
        }
    }

    CollectFeedbackEffects(
        effects = viewModel.effects,
        snackbarHostState = snackbarHostState,
        context = context,
        hapticsManager = hapticsManager
    )
}
