package com.byteflipper.random.ui.lists

import androidx.compose.material3.SnackbarHostState
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.common.FlipGenerateController
import com.byteflipper.random.ui.common.runGeneratorExecution
import com.byteflipper.random.ui.common.showGeneratorActionSnackbar
import com.byteflipper.random.ui.common.showGeneratorSnackbar
import kotlinx.coroutines.CoroutineScope

internal fun FlipGenerateController.handleListGeneration(
    uiState: ListUiState,
    viewModel: ListViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context
) {
    val adapter = viewModel.toGeneratorExecutionAdapter()
    val base = viewModel.getBaseItems()
    if (base.isEmpty()) {
        scope.showGeneratorSnackbar(
            snackbarHostState = snackbarHostState,
            message = context.getString(R.string.list_empty)
        )
        return
    }

    if (!uiState.allowRepetitions) {
        val pool = base.filter { it !in uiState.usedItems }.distinct()
        if (pool.isEmpty()) {
            scope.showGeneratorActionSnackbar(
                snackbarHostState = snackbarHostState,
                message = context.getString(R.string.all_options_used),
                actionLabel = context.getString(R.string.reset),
                onAction = {
                    viewModel.onEvent(ListUiEvent.ResetUsedItems)
                }
            )
            return
        }
    }

    runGeneratorExecution(context = context, adapter = adapter)
}

internal fun ListScreenController.handleSaveDialogDismiss(viewModel: ListViewModel) {
    saveDialogUsesResults = false
    viewModel.onEvent(ListUiEvent.ToggleSaveDialog)
}

internal fun ListScreenController.handleTopSave(viewModel: ListViewModel, listLabel: String) {
    saveDialogUsesResults = false
    viewModel.updateSaveName(listLabel)
    viewModel.toggleSaveDialog()
}

internal fun ListScreenController.handleTopSaveResults(
    viewModel: ListViewModel,
    resultsPresetName: String
) {
    saveDialogUsesResults = true
    viewModel.updateSaveName(resultsPresetName)
    viewModel.toggleSaveDialog()
}

internal fun ListScreenController.handleSaveConfirm(
    viewModel: ListViewModel,
    name: String,
    shouldOpenAfterSave: Boolean
) {
    viewModel.saveAsNewPreset(
        name = name,
        openAfterSave = shouldOpenAfterSave,
        itemsOverride = if (saveDialogUsesResults) {
            viewModel.getCurrentResults().takeIf { it.isNotEmpty() }
        } else {
            null
        }
    ) { newId ->
        saveDialogUsesResults = false
        pendingOpenPresetId = newId
    }
}

internal fun ListScreenController.buildTopTitle(
    presetId: Long?,
    displayedPreset: ListPreset?,
    fallbackTitle: String
): String {
    return if (presetId == null) fallbackTitle else (displayedPreset?.name ?: fallbackTitle)
}
