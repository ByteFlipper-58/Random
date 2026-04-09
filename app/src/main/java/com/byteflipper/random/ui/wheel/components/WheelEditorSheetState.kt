package com.byteflipper.random.ui.wheel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset

@Composable
internal fun rememberWheelEditorSheetController(
    items: List<String>,
    onUpdateItems: (List<String>) -> Unit,
    onLoadPreset: (ListPreset) -> Unit,
    wheelPresetLimit: Int
): WheelEditorSheetController {
    val defaultItems = listOf(
        stringResource(R.string.item_1),
        stringResource(R.string.item_2)
    )
    val quickTemplates = listOf(
        stringResource(R.string.wheel_template_yes_no) to listOf(
            stringResource(R.string.wheel_yes),
            stringResource(R.string.wheel_no)
        ),
        stringResource(R.string.wheel_template_days) to listOf(
            stringResource(R.string.wheel_monday),
            stringResource(R.string.wheel_tuesday),
            stringResource(R.string.wheel_wednesday),
            stringResource(R.string.wheel_thursday),
            stringResource(R.string.wheel_friday),
            stringResource(R.string.wheel_saturday),
            stringResource(R.string.wheel_sunday)
        ),
        stringResource(R.string.wheel_template_colors) to listOf(
            stringResource(R.string.wheel_red),
            stringResource(R.string.wheel_blue),
            stringResource(R.string.wheel_green),
            stringResource(R.string.wheel_yellow),
            stringResource(R.string.wheel_orange),
            stringResource(R.string.wheel_purple)
        )
    )

    return remember(defaultItems, quickTemplates, onUpdateItems, onLoadPreset, wheelPresetLimit) {
        WheelEditorSheetController(
            defaultItems = defaultItems,
            quickTemplates = quickTemplates,
            wheelPresetLimit = wheelPresetLimit,
            onUpdateItems = onUpdateItems,
            onLoadPreset = onLoadPreset
        )
    }.also { controller ->
        controller.syncItems(items)
    }
}

internal class WheelEditorSheetController(
    val defaultItems: List<String>,
    val quickTemplates: List<Pair<String, List<String>>>,
    private val wheelPresetLimit: Int,
    private val onUpdateItems: (List<String>) -> Unit,
    private val onLoadPreset: (ListPreset) -> Unit
) {
    var newItemText by mutableStateOf("")
    var showPresetMenu by mutableStateOf(false)
    var showTemplatesMenu by mutableStateOf(false)
    var showClearConfirm by mutableStateOf(false)
    var showSaveDialog by mutableStateOf(false)
    var savePresetName by mutableStateOf("")
    var oversizedPreset by mutableStateOf<ListPreset?>(null)
    var manualSelectionPreset by mutableStateOf<ListPreset?>(null)
    var manualSelectionIndices by mutableStateOf<Set<Int>>(emptySet())
    var editingIndex by mutableIntStateOf(-1)
    var editingText by mutableStateOf("")

    private var currentItems: List<String> = emptyList()

    fun syncItems(items: List<String>) {
        currentItems = items
        if (editingIndex !in items.indices) {
            editingIndex = -1
            editingText = ""
        }
    }

    fun addItem() {
        val text = newItemText.trim()
        if (text.isNotEmpty() && currentItems.size < wheelPresetLimit) {
            onUpdateItems(currentItems + text)
            newItemText = ""
        }
    }

    fun removeItem(index: Int) {
        if (currentItems.size > 1) {
            onUpdateItems(currentItems.toMutableList().apply { removeAt(index) })
            if (editingIndex == index) {
                editingIndex = -1
                editingText = ""
            }
        }
    }

    fun startEdit(index: Int) {
        editingIndex = index
        editingText = currentItems[index]
    }

    fun confirmEdit() {
        if (editingIndex >= 0 && editingText.isNotBlank()) {
            onUpdateItems(
                currentItems.toMutableList().apply {
                    this[editingIndex] = editingText.trim()
                }
            )
        }
        editingIndex = -1
        editingText = ""
    }

    fun cancelEdit() {
        editingIndex = -1
        editingText = ""
    }

    fun clearAll() {
        onUpdateItems(defaultItems)
        showClearConfirm = false
    }

    fun loadTemplate(template: List<String>) {
        onUpdateItems(template.take(wheelPresetLimit))
        showTemplatesMenu = false
    }

    fun beginLoadPreset(preset: ListPreset) {
        if (preset.items.size <= wheelPresetLimit) {
            loadPresetIntoWheel(preset, preset.items)
        } else {
            oversizedPreset = preset
        }
        showPresetMenu = false
    }

    fun loadPresetFirstItems(preset: ListPreset) {
        loadPresetIntoWheel(preset, preset.items.take(wheelPresetLimit))
    }

    fun loadPresetRandomItems(preset: ListPreset) {
        loadPresetIntoWheel(preset, preset.items.shuffled().take(wheelPresetLimit))
    }

    fun startManualPresetSelection(preset: ListPreset) {
        oversizedPreset = null
        manualSelectionPreset = preset
        manualSelectionIndices = preset.items.indices.take(wheelPresetLimit).toSet()
    }

    fun toggleManualSelection(index: Int) {
        manualSelectionIndices = when {
            index in manualSelectionIndices -> manualSelectionIndices - index
            manualSelectionIndices.size < wheelPresetLimit -> manualSelectionIndices + index
            else -> manualSelectionIndices
        }
    }

    fun dismissManualSelection() {
        manualSelectionPreset = null
        manualSelectionIndices = emptySet()
    }

    fun confirmManualPresetSelection() {
        val preset = manualSelectionPreset ?: return
        val selectedItems = preset.items.filterIndexed { index, _ -> index in manualSelectionIndices }
        if (selectedItems.size in 2..wheelPresetLimit) {
            loadPresetIntoWheel(preset, selectedItems)
        }
    }

    fun prepareSaveDialog() {
        savePresetName = ""
        showSaveDialog = true
    }

    private fun loadPresetIntoWheel(preset: ListPreset, loadedItems: List<String>) {
        onLoadPreset(preset.copy(items = loadedItems))
        oversizedPreset = null
        manualSelectionPreset = null
        manualSelectionIndices = emptySet()
    }
}
