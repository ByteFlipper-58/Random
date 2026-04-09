package com.byteflipper.random.ui.presets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.transfer.ParsedPresetImport
import com.byteflipper.random.data.preset.transfer.PresetImportMode
import com.byteflipper.random.data.preset.transfer.PresetTransferFormat
import com.byteflipper.random.data.preset.transfer.PresetTransferPayload
import com.byteflipper.random.ui.app.PendingSharedImport
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.home.components.PresetCard
import com.byteflipper.random.ui.home.components.RenameListDialog
import com.byteflipper.random.ui.presets.components.PresetExportFormatDialog
import com.byteflipper.random.ui.presets.components.PresetFiltersBar
import com.byteflipper.random.ui.presets.components.PresetImportPreviewDialog
import com.byteflipper.random.ui.presets.components.PresetManagerActions
import com.byteflipper.random.ui.presets.components.PresetMergeDialog
import com.byteflipper.random.ui.presets.components.PresetSectionHeader
import com.byteflipper.random.ui.presets.components.PresetsEmptyState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private data class PresetSection(
    val titleRes: Int?,
    val presets: List<ListPreset>
)

private const val SHARE_COPY_MAX_CHARS = 4_096

private sealed class FormatSelectionTarget {
    abstract val presets: List<ListPreset>
    abstract val titleRes: Int

    data class Export(
        override val presets: List<ListPreset>,
        override val titleRes: Int
    ) : FormatSelectionTarget()

    data class Share(
        override val presets: List<ListPreset>,
        override val titleRes: Int
    ) : FormatSelectionTarget()

    data class Copy(
        override val presets: List<ListPreset>,
        override val titleRes: Int
    ) : FormatSelectionTarget()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetsContent(
    modifier: Modifier = Modifier,
    onOpenPreset: (ListPreset) -> Unit,
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
    var renameTarget by remember { mutableStateOf<ListPreset?>(null) }
    var importPreview by remember { mutableStateOf<ParsedPresetImport?>(null) }
    var formatSelectionTarget by remember { mutableStateOf<FormatSelectionTarget?>(null) }
    var pendingExportPayload by remember { mutableStateOf<PresetTransferPayload?>(null) }
    var pendingExportIsBundle by remember { mutableStateOf(false) }
    var selectedPresetIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectionMode by remember { mutableStateOf(false) }
    var mergeDialogInitialName by remember { mutableStateOf<String?>(null) }

    val selectedPresets = remember(uiState.presets, selectedPresetIds) {
        uiState.presets.filter { preset -> preset.id in selectedPresetIds }
    }
    val shareCopyText = remember(formatSelectionTarget) {
        val selection = formatSelectionTarget as? FormatSelectionTarget.Share
            ?: return@remember null
        selection.presets.singleOrNull()
            ?.items
            ?.joinToString(separator = "\n")
            ?.takeIf { text ->
                text.isNotBlank() && text.length <= SHARE_COPY_MAX_CHARS
            }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    importPreview = viewModel.parseImportFromUri(uri)
                } catch (error: IllegalStateException) {
                    snackbarHostState.showSnackbar(error.message.orEmpty())
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        handleExportResult(
            uri = uri,
            pendingPayload = pendingExportPayload,
            isBundleExport = pendingExportIsBundle,
            onComplete = {
                pendingExportPayload = null
                pendingExportIsBundle = false
            },
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }

    val exportTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        handleExportResult(
            uri = uri,
            pendingPayload = pendingExportPayload,
            isBundleExport = pendingExportIsBundle,
            onComplete = {
                pendingExportPayload = null
                pendingExportIsBundle = false
            },
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        handleExportResult(
            uri = uri,
            pendingPayload = pendingExportPayload,
            isBundleExport = pendingExportIsBundle,
            onComplete = {
                pendingExportPayload = null
                pendingExportIsBundle = false
            },
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }

    fun resetSelection() {
        selectionMode = false
        selectedPresetIds = emptySet()
    }

    fun enterSelection(presetId: Long) {
        selectionMode = true
        selectedPresetIds = selectedPresetIds + presetId
    }

    fun toggleSelection(presetId: Long) {
        val updatedSelection = if (presetId in selectedPresetIds) {
            selectedPresetIds - presetId
        } else {
            selectedPresetIds + presetId
        }
        selectedPresetIds = updatedSelection
        selectionMode = updatedSelection.isNotEmpty()
    }

    fun launchExport(payload: PresetTransferPayload, isBundle: Boolean) {
        pendingExportPayload = payload
        pendingExportIsBundle = isBundle
        when (payload.format) {
            PresetTransferFormat.Csv -> exportCsvLauncher.launch(payload.fileName)
            PresetTransferFormat.Json -> exportLauncher.launch(payload.fileName)
            PresetTransferFormat.Txt -> exportTextLauncher.launch(payload.fileName)
        }
    }

    suspend fun preparePayload(
        presets: List<ListPreset>,
        format: PresetTransferFormat
    ): PresetTransferPayload = viewModel.prepareExportPayload(presets, format)

    fun copyPayloadToClipboard(payload: PresetTransferPayload) {
        clipboard.setPrimaryClip(ClipData.newPlainText(payload.fileName, payload.content))
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
        }
    }

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
            resetSelection()
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

        scope.launch {
            try {
                importPreview = viewModel.parseImportFromClipboard(clipText)
            } catch (error: IllegalStateException) {
                snackbarHostState.showSnackbar(error.message.orEmpty())
            }
        }
    }

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
            importPreview = when {
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

    LaunchedEffect(selectionMode, selectedPresetIds, uiState.presets) {
        onSelectionStateChanged(
            PresetsSelectionUiState(
                active = selectionMode,
                selectedCount = selectedPresetIds.size,
                canMerge = selectedPresetIds.size >= 2,
                hasSelection = selectedPresetIds.isNotEmpty()
            )
        )
    }

    LaunchedEffect(externalAction?.id) {
        val action = externalAction ?: return@LaunchedEffect
        when (action.type) {
            PresetsExternalActionType.ImportFile -> {
                importLauncher.launch(arrayOf("application/json", "text/plain", "text/csv", "*/*"))
            }
            PresetsExternalActionType.ImportClipboard -> {
                handleClipboardImport()
            }
            PresetsExternalActionType.ExportAll -> {
                formatSelectionTarget = FormatSelectionTarget.Export(
                    presets = uiState.availablePresets,
                    titleRes = R.string.export_all_format_title
                )
            }
            PresetsExternalActionType.ExportSelected -> {
                if (selectedPresets.isNotEmpty()) {
                    formatSelectionTarget = FormatSelectionTarget.Export(
                        presets = selectedPresets,
                        titleRes = R.string.export_selected_format_title
                    )
                }
            }
            PresetsExternalActionType.ShareSelected -> {
                if (selectedPresets.isNotEmpty()) {
                    formatSelectionTarget = FormatSelectionTarget.Share(
                        presets = selectedPresets,
                        titleRes = R.string.share_selected_format_title
                    )
                }
            }
            PresetsExternalActionType.MergeSelected -> {
                if (selectedPresets.size >= 2) {
                    mergeDialogInitialName = context.getString(
                        R.string.merged_preset_name,
                        selectedPresets.first().name
                    )
                }
            }
            PresetsExternalActionType.DeleteSelected -> {
                handleDeleteSelected()
            }
            PresetsExternalActionType.ExitSelection -> {
                resetSelection()
            }
        }
        onExternalActionHandled(action.id)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "presets_controls") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    PresetFiltersBar(
                        selectedFilter = uiState.filter,
                        sortAscending = uiState.sortAscending,
                        onFilterChange = viewModel::updateFilter,
                        onToggleSortOrder = viewModel::toggleSortOrder,
                        onInteractionChanged = onFilterInteractionChanged
                    )
                }
            }

            if (uiState.presets.isEmpty()) {
                item(key = "empty_state") {
                    PresetsEmptyState(
                        hasAnyPresets = uiState.hasAnyPresets,
                        filter = uiState.filter,
                        query = "",
                        onCreatePreset = onCreatePreset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            } else {
                sections.forEachIndexed { sectionIndex, section ->
                    if (section.titleRes != null) {
                        item(key = "section_${sectionIndex}_${section.titleRes}") {
                            PresetSectionHeader(
                                title = stringResource(section.titleRes),
                                count = section.presets.size,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    items(
                        items = section.presets,
                        key = { preset -> preset.id }
                    ) { preset ->
                        val duplicateName = stringResource(R.string.preset_duplicate_name, preset.name)
                        val metaText = presetMetaText(preset)
                        val isLastUsed = uiState.lastUsedPresetId == preset.id

                        PresetCard(
                            preset = preset,
                            onPresetClick = { currentPreset ->
                                if (selectionMode) {
                                    toggleSelection(currentPreset.id)
                                } else {
                                    onOpenPreset(currentPreset)
                                }
                            },
                            onPresetLongClick = { currentPreset ->
                                if (!selectionMode || currentPreset.id !in selectedPresetIds) {
                                    enterSelection(currentPreset.id)
                                }
                            },
                            onRenameClick = { renameTarget = it },
                            onDeleteClick = ::handleDeletePreset,
                            subtitle = metaText,
                            highlightPinned = true,
                            emphasize = isLastUsed || preset.id in selectedPresetIds,
                            trailingContent = { currentPreset ->
                                if (selectionMode) {
                                    Checkbox(
                                        checked = currentPreset.id in selectedPresetIds,
                                        onCheckedChange = { toggleSelection(currentPreset.id) }
                                    )
                                } else {
                                    PresetManagerActions(
                                        preset = currentPreset,
                                        onTogglePinned = {
                                            performPresetHaptic()
                                            viewModel.togglePinned(currentPreset)
                                        },
                                        onCopy = {
                                            formatSelectionTarget = FormatSelectionTarget.Copy(
                                                presets = listOf(currentPreset),
                                                titleRes = R.string.copy_format_title
                                            )
                                        },
                                        onExport = {
                                            formatSelectionTarget = FormatSelectionTarget.Export(
                                                presets = listOf(currentPreset),
                                                titleRes = R.string.export_format_title
                                            )
                                        },
                                        onShare = {
                                            formatSelectionTarget = FormatSelectionTarget.Share(
                                                presets = listOf(currentPreset),
                                                titleRes = R.string.share_format_title
                                            )
                                        },
                                        onDuplicate = {
                                            scope.launch {
                                                val duplicatedPreset = viewModel.duplicatePreset(
                                                    currentPreset,
                                                    copyName = duplicateName
                                                )
                                                val result = snackbarHostState.showSnackbar(
                                                    message = context.getString(R.string.preset_duplicated),
                                                    actionLabel = context.getString(R.string.undo)
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.deletePreset(duplicatedPreset)
                                                }
                                            }
                                        },
                                        onRename = { renameTarget = currentPreset },
                                        onDelete = {
                                            performPresetHaptic()
                                            handleDeletePreset(currentPreset)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = if (section.titleRes == null && sectionIndex == 0) 2.dp else 0.dp
                                )
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

    if (renameTarget != null) {
        RenameListDialog(
            preset = renameTarget,
            onDismiss = { renameTarget = null },
            onRename = { preset, newName -> viewModel.renamePreset(preset, newName) },
            onPresetRenamed = { renameTarget = null }
        )
    }

    if (importPreview != null) {
        val parsedImport = importPreview
        PresetImportPreviewDialog(
            parsedImport = checkNotNull(parsedImport),
            onDismiss = { importPreview = null },
            onImportAsCopy = {
                scope.launch {
                    val preview = importPreview ?: return@launch
                    importPreview = null
                    snackbarHostState.showSnackbar(viewModel.commitImport(preview, PresetImportMode.Copy))
                }
            },
            onReplaceMatching = {
                scope.launch {
                    val preview = importPreview ?: return@launch
                    importPreview = null
                    snackbarHostState.showSnackbar(viewModel.commitImport(preview, PresetImportMode.ReplaceMatching))
                }
            }
        )
    }

    if (mergeDialogInitialName != null) {
        PresetMergeDialog(
            initialName = mergeDialogInitialName.orEmpty(),
            onDismiss = { mergeDialogInitialName = null },
            onConfirm = { mergedName ->
                val presetsToMerge = selectedPresets
                mergeDialogInitialName = null
                scope.launch {
                    val mergedPreset = viewModel.mergePresets(presetsToMerge, mergedName)
                    resetSelection()
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.merge_presets_done),
                        actionLabel = context.getString(R.string.open)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onOpenPreset(mergedPreset)
                    }
                }
            }
        )
    }

    if (formatSelectionTarget != null) {
        val selectionTarget = formatSelectionTarget
        PresetExportFormatDialog(
            titleRes = checkNotNull(selectionTarget).titleRes,
            preferredFormat = lastTransferFormat,
            showCopyAction = selectionTarget is FormatSelectionTarget.Share && shareCopyText != null,
            onDismiss = { formatSelectionTarget = null },
            onSelectFormat = { format ->
                val selection = formatSelectionTarget ?: return@PresetExportFormatDialog
                formatSelectionTarget = null
                scope.launch {
                    try {
                        val payload = preparePayload(selection.presets, format)
                        when (selection) {
                            is FormatSelectionTarget.Export -> {
                                launchExport(payload, isBundle = selection.presets.size > 1)
                            }
                            is FormatSelectionTarget.Share -> {
                                val subject = if (selection.presets.size == 1) {
                                    selection.presets.first().name
                                } else {
                                    context.getString(R.string.selected_presets_share_subject, selection.presets.size)
                                }
                                context.startActivity(viewModel.createShareIntent(payload, subject))
                            }
                            is FormatSelectionTarget.Copy -> {
                                copyPayloadToClipboard(payload)
                            }
                        }
                    } catch (error: IllegalStateException) {
                        snackbarHostState.showSnackbar(error.message.orEmpty())
                    }
                }
            },
            onCopyClick = if (selectionTarget is FormatSelectionTarget.Share) {
                {
                    val copyText = shareCopyText ?: return@PresetExportFormatDialog
                    val selection = formatSelectionTarget ?: return@PresetExportFormatDialog
                    formatSelectionTarget = null
                    val preset = selection.presets.singleOrNull() ?: return@PresetExportFormatDialog
                    clipboard.setPrimaryClip(ClipData.newPlainText(preset.name, copyText))
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
                    }
                }
            } else {
                null
            }
        )
    }
}

private fun handleExportResult(
    uri: Uri?,
    pendingPayload: PresetTransferPayload?,
    isBundleExport: Boolean,
    onComplete: () -> Unit,
    viewModel: PresetsViewModel,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val exportPayload = pendingPayload
    onComplete()

    if (uri != null && exportPayload != null) {
        scope.launch {
            try {
                val message = if (isBundleExport) {
                    viewModel.writeBundleExportToUri(exportPayload, uri)
                } else {
                    viewModel.writeExportToUri(exportPayload, uri)
                }
                snackbarHostState.showSnackbar(message)
            } catch (error: IllegalStateException) {
                snackbarHostState.showSnackbar(error.message.orEmpty())
            }
        }
    }
}

private fun buildSections(
    uiState: PresetsUiState
): List<PresetSection> {
    if (uiState.presets.isEmpty()) return emptyList()

    return when (uiState.filter) {
        PresetFilter.All -> {
            val pinned = uiState.presets.filter { it.isPinned }
            val others = uiState.presets.filterNot { it.isPinned }
            buildList {
                if (pinned.isNotEmpty()) {
                    add(PresetSection(R.string.pinned, pinned))
                }
                if (others.isNotEmpty()) {
                    add(
                        PresetSection(
                            titleRes = if (pinned.isNotEmpty()) R.string.more_presets else null,
                            presets = others
                        )
                    )
                }
            }
        }

        PresetFilter.Recent -> listOf(PresetSection(null, uiState.presets))
        PresetFilter.MostUsed -> listOf(PresetSection(null, uiState.presets))
    }
}
