package com.byteflipper.random.ui.presets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.transfer.PresetImportMode
import com.byteflipper.random.data.preset.transfer.PresetTransferFormat
import com.byteflipper.random.data.preset.transfer.PresetTransferPayload
import com.byteflipper.random.ui.app.PendingSharedImport
import com.byteflipper.random.ui.components.LocalHapticsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetsContent(
    modifier: Modifier = Modifier,
    onOpenPreset: (ListPreset) -> Unit,
    onOpenTeamPreset: (Long) -> Unit,
    onCreatePreset: () -> Unit = {},
    onFilterInteractionChanged: (Boolean) -> Unit = {},
    pendingSharedImport: PendingSharedImport? = null,
    onSharedImportConsumed: (Long) -> Unit = {},
    externalAction: PresetsExternalAction? = null,
    onExternalActionHandled: (Long) -> Unit = {},
    onSelectionStateChanged: (PresetsSelectionUiState) -> Unit = {},
    viewModel: PresetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lastTransferFormat by viewModel.lastTransferFormat.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = uiState.firstVisibleItemScrollOffset
    )
    val sections = remember(uiState.presets, uiState.filter) { buildSections(uiState) }
    val context = LocalContext.current
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val hapticsManager = LocalHapticsManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val transferController = rememberPresetsTransferController()
    val selectionController = rememberPresetsSelectionController()

    val selectedPresets = remember(uiState.presets, selectionController.selectedPresetIds) {
        uiState.presets.filter { preset -> preset.id in selectionController.selectedPresetIds }
    }
    val shareCopyText = remember(transferController.formatSelectionTarget) {
        val selection = transferController.formatSelectionTarget as? FormatSelectionTarget.Share
            ?: return@remember null
        selection.presets.singleOrNull()
            ?.items
            ?.joinToString(separator = "\n")
            ?.takeIf { text -> text.isNotBlank() && text.length <= SHARE_COPY_MAX_CHARS }
    }

    val transferLaunchers = rememberPresetsTransferLaunchers(
        controller = transferController,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        scope = scope
    )

    suspend fun preparePayload(
        presets: List<ListPreset>,
        format: PresetTransferFormat
    ): PresetTransferPayload = viewModel.prepareExportPayload(presets, format)

    fun performPresetHaptic() {
        if (settings.hapticsEnabled) {
            hapticsManager?.performPress(settings.hapticsIntensity)
        }
    }

    fun handleDeletePreset(preset: ListPreset) {
        scope.launch {
            viewModel.deletePreset(preset)
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.preset_deleted),
                actionLabel = context.getString(R.string.undo)
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restorePreset(preset)
            }
        }
    }

    fun handleDeleteSelected() {
        val presetsToDelete = selectedPresets
        if (presetsToDelete.isEmpty()) return

        scope.launch {
            presetsToDelete.forEach(viewModel::deletePreset)
            selectionController.reset()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.delete_selected_done, presetsToDelete.size),
                actionLabel = context.getString(R.string.undo)
            )
            if (result == SnackbarResult.ActionPerformed) {
                presetsToDelete.forEach(viewModel::restorePreset)
            }
        }
    }

    fun handleClipboardImport() {
        val clipText = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()

        if (clipText.isBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.clipboard_empty))
            }
            return
        }

        scope.launchPresetImport(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            onParsed = { transferController.importPreview = it }
        ) {
            viewModel.parseImportFromClipboard(clipText)
        }
    }

    PresetsContentEffects(
        listState = listState,
        pendingSharedImport = pendingSharedImport,
        onSharedImportConsumed = onSharedImportConsumed,
        externalAction = externalAction,
        onExternalActionHandled = onExternalActionHandled,
        selectionController = selectionController,
        transferController = transferController,
        uiState = uiState,
        selectedPresets = selectedPresets,
        context = context,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onSelectionStateChanged = onSelectionStateChanged,
        onImportClipboard = ::handleClipboardImport,
        onDeleteSelected = ::handleDeleteSelected,
        launchImportFile = transferLaunchers.importFile
    )

    PresetsContentList(
        modifier = modifier,
        uiState = uiState,
        sections = sections,
        listState = listState,
        selectionMode = selectionController.selectionMode,
        selectedPresetIds = selectionController.selectedPresetIds,
        onFilterInteractionChanged = onFilterInteractionChanged,
        onFilterChange = viewModel::updateFilter,
        onToggleSortOrder = viewModel::toggleSortOrder,
        onCreatePreset = onCreatePreset,
        onOpenPreset = onOpenPreset,
        onOpenTeamPreset = onOpenTeamPreset,
        onToggleSelection = selectionController::toggle,
        onEnterSelection = selectionController::enter,
        onRenameClick = { transferController.renameTarget = it },
        onDeleteClick = ::handleDeletePreset,
        onTogglePinned = { preset ->
            performPresetHaptic()
            viewModel.togglePinned(preset)
        },
        onPrepareCopy = { preset ->
            transferController.formatSelectionTarget = FormatSelectionTarget.Copy(presets = listOf(preset))
        },
        onPrepareExport = { preset ->
            transferController.formatSelectionTarget = FormatSelectionTarget.Export(
                presets = listOf(preset),
                titleRes = R.string.export_format_title
            )
        },
        onPrepareShare = { preset ->
            transferController.formatSelectionTarget = FormatSelectionTarget.Share(
                presets = listOf(preset),
                titleRes = R.string.share_format_title
            )
        },
        onDuplicate = { preset, copyName ->
            scope.launch {
                val duplicatedPreset = viewModel.duplicatePreset(preset, copyName = copyName)
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.preset_duplicated),
                    actionLabel = context.getString(R.string.undo)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.deletePreset(duplicatedPreset)
                }
            }
        },
        snackbarHostState = snackbarHostState
    )

    val handleFormatSelected: (PresetTransferFormat) -> Unit = { format ->
        val selection = transferController.formatSelectionTarget
        if (selection != null) {
            transferController.formatSelectionTarget = null
            scope.launch {
                try {
                    val payload = preparePayload(selection.presets, format)
                    when (selection) {
                        is FormatSelectionTarget.Export -> {
                            transferLaunchers.exportPayload(payload, selection.presets.size > 1)
                        }
                        is FormatSelectionTarget.Share -> {
                            val subject = if (selection.presets.size == 1) {
                                selection.presets.first().name
                            } else {
                                context.getString(
                                    R.string.selected_presets_share_subject,
                                    selection.presets.size
                                )
                            }
                            context.startActivity(viewModel.createShareIntent(payload, subject))
                        }
                        is FormatSelectionTarget.Copy -> {
                            clipboard.setPrimaryClip(ClipData.newPlainText(payload.fileName, payload.content))
                            snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
                        }
                    }
                } catch (error: IllegalStateException) {
                    snackbarHostState.showSnackbar(error.message.orEmpty())
                }
            }
        }
    }

    val handleShareCopyClick: (() -> Unit)? =
        if (transferController.formatSelectionTarget is FormatSelectionTarget.Share && shareCopyText != null) {
        {
            val selection = transferController.formatSelectionTarget
            val copyText = shareCopyText
            val preset = selection?.presets?.singleOrNull()
            if (selection != null && preset != null) {
                transferController.formatSelectionTarget = null
                clipboard.setPrimaryClip(ClipData.newPlainText(preset.name, copyText))
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
                }
            }
        }
    } else {
        null
    }

    PresetsDialogHost(
        renameTarget = transferController.renameTarget,
        onRenameDismiss = { transferController.renameTarget = null },
        onRename = { preset, newName -> viewModel.renamePreset(preset, newName) },
        importPreview = transferController.importPreview,
        onImportDismiss = { transferController.importPreview = null },
        onImportAsCopy = {
            scope.launch {
                val preview = transferController.importPreview ?: return@launch
                transferController.importPreview = null
                snackbarHostState.showSnackbar(viewModel.commitImport(preview, PresetImportMode.Copy))
            }
        },
        onReplaceMatching = {
            scope.launch {
                val preview = transferController.importPreview ?: return@launch
                transferController.importPreview = null
                snackbarHostState.showSnackbar(
                    viewModel.commitImport(preview, PresetImportMode.ReplaceMatching)
                )
            }
        },
        mergeDialogInitialName = transferController.mergeDialogInitialName,
        onMergeDismiss = { transferController.mergeDialogInitialName = null },
        onMergeConfirm = { mergedName ->
            val presetsToMerge = selectedPresets
            transferController.mergeDialogInitialName = null
            scope.launch {
                val mergedPreset = viewModel.mergePresets(presetsToMerge, mergedName)
                selectionController.reset()
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.merge_presets_done),
                    actionLabel = context.getString(R.string.open)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onOpenPreset(mergedPreset)
                }
            }
        },
        formatSelectionTarget = transferController.formatSelectionTarget,
        preferredFormat = lastTransferFormat,
        shareCopyText = shareCopyText,
        onFormatDismiss = { transferController.formatSelectionTarget = null },
        onFormatSelected = handleFormatSelected,
        onShareCopyClick = handleShareCopyClick
    )
}
