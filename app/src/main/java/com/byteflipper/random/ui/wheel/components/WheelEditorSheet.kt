package com.byteflipper.random.ui.wheel.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.ui.wheel.WHEEL_MAX_ITEMS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelEditorSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    items: List<String>,
    excludedIndices: Set<Int>,
    onUpdateItems: (List<String>) -> Unit,
    presets: List<ListPreset>,
    onLoadPreset: (ListPreset) -> Unit,
    hapticsEnabled: Boolean,
    hapticsIntensity: HapticsIntensity,
    onSaveAsPreset: ((String) -> Unit)? = null,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
) {
    val wheelPresetLimit = WHEEL_MAX_ITEMS
    val controller = rememberWheelEditorSheetController(
        items = items,
        onUpdateItems = onUpdateItems,
        onLoadPreset = onLoadPreset,
        wheelPresetLimit = wheelPresetLimit
    )

    if (visible && controller.oversizedPreset == null && controller.manualSelectionPreset == null) {
        WheelEditorSheetContent(
            onDismiss = onDismiss,
            sheetState = sheetState,
            items = items,
            excludedIndices = excludedIndices,
            presets = presets,
            wheelPresetLimit = wheelPresetLimit,
            newItemText = controller.newItemText,
            onNewItemTextChange = { controller.newItemText = it },
            showPresetMenu = controller.showPresetMenu,
            onShowPresetMenuChange = { controller.showPresetMenu = it },

            editingIndex = controller.editingIndex,
            editingText = controller.editingText,
            onEditingTextChange = { controller.editingText = it },

            onShowClearConfirm = { controller.showClearConfirm = true },
            onShowSaveDialog = controller::prepareSaveDialog,
            onAddItem = controller::addItem,
            onRemoveItem = controller::removeItem,
            onStartEdit = controller::startEdit,
            onConfirmEdit = controller::confirmEdit,
            onCancelEdit = controller::cancelEdit,
            onLoadPresetClick = controller::beginLoadPreset,
            onLoadTemplate = controller::loadTemplate,
            onLoadPeople = controller::loadPeople,
            canSaveAsPreset = onSaveAsPreset != null
        )
    }

    WheelClearConfirmDialog(
        visible = controller.showClearConfirm,
        onDismiss = { controller.showClearConfirm = false },
        onConfirm = controller::clearAll
    )

    WheelSavePresetDialog(
        visible = controller.showSaveDialog && onSaveAsPreset != null,
        presetName = controller.savePresetName,
        onPresetNameChange = { controller.savePresetName = it },
        onDismiss = { controller.showSaveDialog = false },
        onConfirm = {
            if (controller.savePresetName.isNotBlank()) {
                onSaveAsPreset?.invoke(controller.savePresetName.trim())
                controller.showSaveDialog = false
            }
        }
    )

    controller.oversizedPreset?.let { preset ->
        WheelPresetImportSheet(
            preset = preset,
            itemLimit = wheelPresetLimit,
            onDismiss = { controller.oversizedPreset = null },
            onLoadFirst = { controller.loadPresetFirstItems(preset) },
            onLoadRandom = { controller.loadPresetRandomItems(preset) },
            onSelectManually = { controller.startManualPresetSelection(preset) }
        )
    }

    controller.manualSelectionPreset?.let { preset ->
        WheelPresetSelectionSheet(
            preset = preset,
            selectedIndices = controller.manualSelectionIndices,
            itemLimit = wheelPresetLimit,
            onSelectionChange = controller::toggleManualSelection,
            hapticsEnabled = hapticsEnabled,
            hapticsIntensity = hapticsIntensity,
            onDismiss = controller::dismissManualSelection,
            onConfirm = controller::confirmManualPresetSelection
        )
    }
}
