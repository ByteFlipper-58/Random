package com.byteflipper.random.ui.numbers

import androidx.compose.material3.SnackbarHostState
import com.byteflipper.random.R
import com.byteflipper.random.ui.common.FlipGenerateController
import com.byteflipper.random.ui.common.runGeneratorExecution
import com.byteflipper.random.ui.common.showGeneratorActionSnackbar
import com.byteflipper.random.ui.common.showGeneratorSnackbar
import kotlinx.coroutines.CoroutineScope

internal fun validateNumberGenerationInputs(
    uiState: NumbersUiState,
    viewModel: NumbersViewModel,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context,
    scope: CoroutineScope
): Pair<IntRange, Int>? {
    val validation = viewModel.validateInputs()
    if (validation != null) return validation

    val from = uiState.fromText.trim().toIntOrNull()
    val to = uiState.toText.trim().toIntOrNull()
    if (from != null && to != null && !uiState.allowRepetitions) {
        val range = if (from <= to) from..to else to..from
        val availableCount = range.count { it !in uiState.usedNumbers }
        if (availableCount <= 0) {
            scope.showGeneratorActionSnackbar(
                snackbarHostState = snackbarHostState,
                message = context.getString(R.string.all_numbers_used),
                actionLabel = context.getString(R.string.reset),
                onAction = viewModel::resetUsedNumbers
            )
            return null
        }
    }

    scope.showGeneratorSnackbar(
        snackbarHostState = snackbarHostState,
        message = context.getString(R.string.enter_valid_numbers)
    )
    return null
}

internal fun notifyNumbersOverlayClosed(
    settings: com.byteflipper.random.data.settings.Settings,
    hapticsManager: com.byteflipper.random.ui.components.HapticsManager?,
    view: android.view.View
) {
    if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
}

internal fun FlipGenerateController.handleNumberGeneration(
    uiState: NumbersUiState,
    viewModel: NumbersViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context,
    view: android.view.View
) {
    val adapter = viewModel.toGeneratorExecutionAdapter()

    validateNumberGenerationInputs(
        uiState = uiState,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        context = context,
        scope = scope
    ) ?: return

    runGeneratorExecution(context = context, adapter = adapter)
}
