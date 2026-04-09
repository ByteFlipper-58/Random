package com.byteflipper.random.ui.presets

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.SnackbarHostState
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.app.PendingSharedImport
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun PresetsContentEffects(
    listState: LazyListState,
    pendingSharedImport: PendingSharedImport?,
    onSharedImportConsumed: (Long) -> Unit,
    externalAction: PresetsExternalAction?,
    onExternalActionHandled: (Long) -> Unit,
    selectionController: PresetsSelectionController,
    transferController: PresetsTransferController,
    uiState: PresetsUiState,
    selectedPresets: List<ListPreset>,
    context: Context,
    viewModel: PresetsViewModel,
    snackbarHostState: SnackbarHostState,
    onSelectionStateChanged: (PresetsSelectionUiState) -> Unit,
    onImportClipboard: () -> Unit,
    onDeleteSelected: () -> Unit,
    launchImportFile: () -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (firstVisibleItemIndex, firstVisibleItemScrollOffset) ->
                viewModel.updateListViewport(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset
                )
            }
    }

    LaunchedEffect(pendingSharedImport?.id) {
        val request = pendingSharedImport ?: return@LaunchedEffect
        try {
            transferController.importPreview = when {
                request.uri != null -> viewModel.parseImportFromUri(request.uri)
                !request.text.isNullOrBlank() -> viewModel.parseImportFromClipboard(request.text)
                else -> null
            }
        } catch (error: IllegalStateException) {
            snackbarHostState.showSnackbar(error.message.orEmpty())
        } finally {
            onSharedImportConsumed(request.id)
        }
    }

    LaunchedEffect(
        selectionController.selectionMode,
        selectionController.selectedPresetIds,
        uiState.presets
    ) {
        onSelectionStateChanged(
            PresetsSelectionUiState(
                active = selectionController.selectionMode,
                selectedCount = selectionController.selectedPresetIds.size,
                canMerge = selectionController.selectedPresetIds.size >= 2,
                hasSelection = selectionController.selectedPresetIds.isNotEmpty()
            )
        )
    }

    LaunchedEffect(externalAction?.id) {
        val action = externalAction ?: return@LaunchedEffect
        when (action.type) {
            PresetsExternalActionType.ImportFile -> launchImportFile()
            PresetsExternalActionType.ImportClipboard -> onImportClipboard()
            PresetsExternalActionType.ExportAll -> {
                transferController.formatSelectionTarget = FormatSelectionTarget.Export(
                    presets = uiState.availablePresets,
                    titleRes = R.string.export_all_format_title
                )
            }

            PresetsExternalActionType.ExportSelected -> {
                if (selectedPresets.isNotEmpty()) {
                    transferController.formatSelectionTarget = FormatSelectionTarget.Export(
                        presets = selectedPresets,
                        titleRes = R.string.export_selected_format_title
                    )
                }
            }

            PresetsExternalActionType.ShareSelected -> {
                if (selectedPresets.isNotEmpty()) {
                    transferController.formatSelectionTarget = FormatSelectionTarget.Share(
                        presets = selectedPresets,
                        titleRes = R.string.share_selected_format_title
                    )
                }
            }

            PresetsExternalActionType.MergeSelected -> {
                if (selectedPresets.size >= 2) {
                    transferController.mergeDialogInitialName = context.getString(
                        R.string.merged_preset_name,
                        selectedPresets.first().name
                    )
                }
            }

            PresetsExternalActionType.DeleteSelected -> onDeleteSelected()
            PresetsExternalActionType.ExitSelection -> selectionController.reset()
        }
        onExternalActionHandled(action.id)
    }
}
