package com.byteflipper.random.ui.presets

data class PresetsSelectionUiState(
    val active: Boolean = false,
    val selectedCount: Int = 0,
    val canMerge: Boolean = false,
    val hasSelection: Boolean = false
)

enum class PresetsExternalActionType {
    ImportFile,
    ImportClipboard,
    ExportAll,
    ExportSelected,
    ShareSelected,
    MergeSelected,
    DeleteSelected,
    ExitSelection
}

data class PresetsExternalAction(
    val id: Long = System.currentTimeMillis(),
    val type: PresetsExternalActionType
)
