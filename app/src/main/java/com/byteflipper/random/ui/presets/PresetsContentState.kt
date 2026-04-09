package com.byteflipper.random.ui.presets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.transfer.ParsedPresetImport
import com.byteflipper.random.data.preset.transfer.PresetTransferPayload

@Composable
internal fun rememberPresetsSelectionController(): PresetsSelectionController {
    return remember { PresetsSelectionController() }
}

internal class PresetsSelectionController {
    var selectionMode by mutableStateOf(false)
    var selectedPresetIds by mutableStateOf<Set<Long>>(emptySet())

    fun reset() {
        selectionMode = false
        selectedPresetIds = emptySet()
    }

    fun enter(presetId: Long) {
        selectionMode = true
        selectedPresetIds = selectedPresetIds + presetId
    }

    fun toggle(presetId: Long) {
        val updatedSelection = if (presetId in selectedPresetIds) {
            selectedPresetIds - presetId
        } else {
            selectedPresetIds + presetId
        }
        selectedPresetIds = updatedSelection
        selectionMode = updatedSelection.isNotEmpty()
    }
}

@Composable
internal fun rememberPresetsTransferController(): PresetsTransferController {
    return remember { PresetsTransferController() }
}

internal class PresetsTransferController {
    var renameTarget by mutableStateOf<ListPreset?>(null)
    var importPreview by mutableStateOf<ParsedPresetImport?>(null)
    var formatSelectionTarget by mutableStateOf<FormatSelectionTarget?>(null)
    var pendingExportPayload by mutableStateOf<PresetTransferPayload?>(null)
    var pendingExportIsBundle by mutableStateOf(false)
    var mergeDialogInitialName by mutableStateOf<String?>(null)

    fun prepareExport(payload: PresetTransferPayload, isBundle: Boolean) {
        pendingExportPayload = payload
        pendingExportIsBundle = isBundle
    }

    fun clearPendingExport() {
        pendingExportPayload = null
        pendingExportIsBundle = false
    }
}
