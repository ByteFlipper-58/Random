package com.byteflipper.random.ui.presets

import androidx.compose.runtime.Composable
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.transfer.ParsedPresetImport
import com.byteflipper.random.data.preset.transfer.PresetTransferFormat
import com.byteflipper.random.ui.home.components.RenameListDialog
import com.byteflipper.random.ui.presets.components.PresetExportFormatDialog
import com.byteflipper.random.ui.presets.components.PresetImportPreviewDialog
import com.byteflipper.random.ui.presets.components.PresetMergeDialog

@Composable
internal fun PresetsDialogHost(
    renameTarget: ListPreset?,
    onRenameDismiss: () -> Unit,
    onRename: (ListPreset, String) -> Unit,
    importPreview: ParsedPresetImport?,
    onImportDismiss: () -> Unit,
    onImportAsCopy: () -> Unit,
    onReplaceMatching: () -> Unit,
    mergeDialogInitialName: String?,
    onMergeDismiss: () -> Unit,
    onMergeConfirm: (String) -> Unit,
    formatSelectionTarget: FormatSelectionTarget?,
    preferredFormat: PresetTransferFormat,
    shareCopyText: String?,
    onFormatDismiss: () -> Unit,
    onFormatSelected: (PresetTransferFormat) -> Unit,
    onShareCopyClick: (() -> Unit)? = null
) {
    if (renameTarget != null) {
        RenameListDialog(
            preset = renameTarget,
            onDismiss = onRenameDismiss,
            onRename = onRename,
            onPresetRenamed = onRenameDismiss
        )
    }

    if (importPreview != null) {
        PresetImportPreviewDialog(
            parsedImport = importPreview,
            onDismiss = onImportDismiss,
            onImportAsCopy = onImportAsCopy,
            onReplaceMatching = onReplaceMatching
        )
    }

    if (mergeDialogInitialName != null) {
        PresetMergeDialog(
            initialName = mergeDialogInitialName,
            onDismiss = onMergeDismiss,
            onConfirm = onMergeConfirm
        )
    }

    if (formatSelectionTarget != null) {
        PresetExportFormatDialog(
            titleRes = formatSelectionTarget.titleRes,
            preferredFormat = preferredFormat,
            showCopyAction = formatSelectionTarget is FormatSelectionTarget.Share && shareCopyText != null,
            onDismiss = onFormatDismiss,
            onSelectFormat = onFormatSelected,
            onCopyClick = onShareCopyClick
        )
    }
}
