package com.byteflipper.random.ui.home

import androidx.compose.runtime.Composable
import com.byteflipper.random.ui.presets.PresetsExternalAction
import com.byteflipper.random.ui.presets.PresetsExternalActionType
import com.byteflipper.random.ui.presets.PresetsSelectionUiState
import com.byteflipper.random.ui.presets.PresetsSearchTopBar
import com.byteflipper.random.ui.presets.PresetsUiState
import com.byteflipper.random.ui.presets.components.PresetsFabControls
import com.byteflipper.random.ui.presets.components.PresetsSelectionTopBar

@Composable
internal fun HomePresetsSearchBar(
    selectedTab: HomeTab,
    showPresetsSearch: Boolean,
    presetsUiState: PresetsUiState,
    onFilterChange: (com.byteflipper.random.ui.presets.PresetFilter) -> Unit,
    onToggleSortOrder: () -> Unit,
    onFilterInteractionChanged: (Boolean) -> Unit,
    onOpenPreset: (com.byteflipper.random.data.preset.ListPreset) -> Unit,
    onDismiss: () -> Unit
): (@Composable () -> Unit)? {
    if (selectedTab != HomeTab.Presets || !showPresetsSearch) return null

    return {
        PresetsSearchTopBar(
            uiState = presetsUiState,
            onFilterChange = onFilterChange,
            onToggleSortOrder = onToggleSortOrder,
            onFilterInteractionChanged = onFilterInteractionChanged,
            onOpenPreset = onOpenPreset,
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun HomePresetsSelectionTopBar(
    selectedTab: HomeTab,
    showPresetsSearch: Boolean,
    selectionState: PresetsSelectionUiState,
    onAction: (PresetsExternalActionType) -> Unit
): (@Composable () -> Unit)? {
    if (selectedTab != HomeTab.Presets || showPresetsSearch || !selectionState.active) return null

    return {
        PresetsSelectionTopBar(
            selectedCount = selectionState.selectedCount,
            hasSelection = selectionState.hasSelection,
            canMerge = selectionState.canMerge,
            onClose = { onAction(PresetsExternalActionType.ExitSelection) },
            onShare = { onAction(PresetsExternalActionType.ShareSelected) },
            onExport = { onAction(PresetsExternalActionType.ExportSelected) },
            onDelete = { onAction(PresetsExternalActionType.DeleteSelected) },
            onMerge = { onAction(PresetsExternalActionType.MergeSelected) }
        )
    }
}

@Composable
internal fun HomePresetsFab(
    selectedTab: HomeTab,
    showPresetsSearch: Boolean,
    selectionState: PresetsSelectionUiState,
    expanded: Boolean,
    hasPresets: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreatePreset: () -> Unit,
    onAction: (PresetsExternalActionType) -> Unit
): (@Composable () -> Unit) {
    if (selectedTab != HomeTab.Presets || showPresetsSearch || selectionState.active) {
        return {}
    }

    return {
        PresetsFabControls(
            expanded = expanded,
            hasPresets = hasPresets,
            onExpandedChange = onExpandedChange,
            onCreatePreset = onCreatePreset,
            onImportFile = { onAction(PresetsExternalActionType.ImportFile) },
            onImportClipboard = { onAction(PresetsExternalActionType.ImportClipboard) },
            onExportAll = { onAction(PresetsExternalActionType.ExportAll) }
        )
    }
}
