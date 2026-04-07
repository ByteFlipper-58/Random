package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.ui.components.RoundedDropdownMenuShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val wheelPresetLimit = 16
    var newItemText by rememberSaveable { mutableStateOf("") }
    var showPresetMenu by remember { mutableStateOf(false) }
    var showTemplatesMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var savePresetName by remember { mutableStateOf("") }
    var oversizedPreset by remember { mutableStateOf<ListPreset?>(null) }
    var manualSelectionPreset by remember { mutableStateOf<ListPreset?>(null) }
    var manualSelectionIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val defaultItems = listOf(stringResource(R.string.item_1), stringResource(R.string.item_2))
    
    // Edit mode
    var editingIndex by remember { mutableIntStateOf(-1) }
    var editingText by remember { mutableStateOf("") }

    // Localized quick templates
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

    fun addItem() {
        val text = newItemText.trim()
        if (text.isNotEmpty() && items.size < wheelPresetLimit) {
            onUpdateItems(items + text)
            newItemText = ""
        }
    }

    fun removeItem(index: Int) {
        if (items.size > 1) {
            val newItems = items.toMutableList().apply { removeAt(index) }
            onUpdateItems(newItems)
            if (editingIndex == index) {
                editingIndex = -1
                editingText = ""
            }
        }
    }

    fun startEdit(index: Int) {
        editingIndex = index
        editingText = items[index]
    }

    fun confirmEdit() {
        if (editingIndex >= 0 && editingText.isNotBlank()) {
            val newItems = items.toMutableList().apply {
                this[editingIndex] = editingText.trim()
            }
            onUpdateItems(newItems)
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

    fun loadPresetIntoWheel(preset: ListPreset, loadedItems: List<String>) {
        onLoadPreset(preset.copy(items = loadedItems))
        oversizedPreset = null
        manualSelectionPreset = null
        manualSelectionIndices = emptySet()
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

    fun confirmManualPresetSelection() {
        val preset = manualSelectionPreset ?: return
        val selectedItems = preset.items.filterIndexed { index, _ -> index in manualSelectionIndices }
        if (selectedItems.size in 2..wheelPresetLimit) {
            loadPresetIntoWheel(preset, selectedItems)
        }
    }

    if (visible && oversizedPreset == null && manualSelectionPreset == null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.wheel_edit_items),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.wheel_items_limit, items.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Clear all button
                        IconButton(
                            onClick = { showClearConfirm = true },
                            enabled = items.size > 1
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.wheel_clear_all),
                                tint = if (items.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        
                        // Save as preset button
                        if (onSaveAsPreset != null) {
                            IconButton(
                                onClick = { 
                                    savePresetName = ""
                                    showSaveDialog = true 
                                },
                                enabled = items.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Save,
                                    contentDescription = stringResource(R.string.wheel_save_as_preset)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Load preset
                    Box {
                        AssistChip(
                            onClick = { showPresetMenu = true },
                            label = { Text(stringResource(R.string.wheel_presets), fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            enabled = presets.isNotEmpty()
                        )
                        
                        DropdownMenu(
                            expanded = showPresetMenu,
                            onDismissRequest = { showPresetMenu = false },
                            shape = RoundedDropdownMenuShape
                        ) {
                            presets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(preset.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                buildPresetSubtitle(
                                                    count = preset.items.size,
                                                    exceedsLimit = preset.items.size > wheelPresetLimit
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.list_alt_24px),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        beginLoadPreset(preset)
                                    }
                                )
                            }
                        }
                    }

                    // Quick templates
                    Box {
                        AssistChip(
                            onClick = { showTemplatesMenu = true },
                            label = { Text(stringResource(R.string.wheel_templates), fontSize = 12.sp) }
                        )
                        
                        DropdownMenu(
                            expanded = showTemplatesMenu,
                            onDismissRequest = { showTemplatesMenu = false },
                            shape = RoundedDropdownMenuShape
                        ) {
                            quickTemplates.forEach { (name, template) ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(name, fontWeight = FontWeight.Medium)
                                            Text(
                                                template.take(4).joinToString(", ") + if (template.size > 4) "..." else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = { loadTemplate(template) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Add item input (hidden while editing)
                if (editingIndex < 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { if (it.length <= 30) newItemText = it },
                        placeholder = { Text(stringResource(R.string.wheel_add_new_item), style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addItem() }),
                        enabled = items.size < wheelPresetLimit,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FilledTonalIconButton(
                        onClick = { addItem() },
                        enabled = newItemText.isNotBlank() && items.size < wheelPresetLimit
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add)
                        )
                    }
                    }
                }

                // Edit mode input (when editing)
                if (editingIndex >= 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editingText,
                            onValueChange = { if (it.length <= 30) editingText = it },
                            label = { Text(stringResource(R.string.wheel_edit_item, editingIndex + 1)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { confirmEdit() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        FilledTonalIconButton(
                            onClick = { confirmEdit() },
                            enabled = editingText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.confirm)
                            )
                        }
                        
                        IconButton(onClick = { cancelEdit() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    }
                }

                // Items as chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        val isExcluded = index in excludedIndices
                        val isEditing = index == editingIndex
                        
                        InputChip(
                            selected = isEditing,
                            onClick = { 
                                if (!isExcluded && editingIndex < 0) {
                                    startEdit(index)
                                }
                            },
                            label = { 
                                Text(
                                    text = item,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isEditing -> MaterialTheme.colorScheme.primary
                                        isExcluded -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            leadingIcon = if (!isExcluded && editingIndex < 0) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.edit),
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            } else null,
                            trailingIcon = {
                                if (!isExcluded && items.size > 1 && editingIndex < 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { removeItem(index) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.delete),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = when {
                                    isEditing -> MaterialTheme.colorScheme.primaryContainer
                                    isExcluded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            border = InputChipDefaults.inputChipBorder(
                                enabled = true,
                                selected = isEditing,
                                borderColor = when {
                                    isEditing -> MaterialTheme.colorScheme.primary
                                    isExcluded -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                },
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                if (items.isEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(R.string.wheel_no_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Clear confirmation dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.wheel_clear_confirm_title)) },
            text = { Text(stringResource(R.string.wheel_clear_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { clearAll() }) {
                    Text(stringResource(R.string.wheel_clear_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Save as preset dialog
    if (showSaveDialog && onSaveAsPreset != null) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.wheel_save_as_preset)) },
            text = {
                OutlinedTextField(
                    value = savePresetName,
                    onValueChange = { savePresetName = it },
                    label = { Text(stringResource(R.string.wheel_preset_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (savePresetName.isNotBlank()) {
                            onSaveAsPreset(savePresetName.trim())
                            showSaveDialog = false
                        }
                    },
                    enabled = savePresetName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    oversizedPreset?.let { preset ->
        WheelPresetImportSheet(
            preset = preset,
            itemLimit = wheelPresetLimit,
            onDismiss = { oversizedPreset = null },
            onLoadFirst = { loadPresetFirstItems(preset) },
            onLoadRandom = { loadPresetRandomItems(preset) },
            onSelectManually = { startManualPresetSelection(preset) }
        )
    }

    manualSelectionPreset?.let { preset ->
        WheelPresetSelectionSheet(
            preset = preset,
            selectedIndices = manualSelectionIndices,
            itemLimit = wheelPresetLimit,
            onSelectionChange = { index ->
                manualSelectionIndices = when {
                    index in manualSelectionIndices -> manualSelectionIndices - index
                    manualSelectionIndices.size < wheelPresetLimit -> manualSelectionIndices + index
                    else -> manualSelectionIndices
                }
            },
            hapticsEnabled = hapticsEnabled,
            hapticsIntensity = hapticsIntensity,
            onDismiss = {
                manualSelectionPreset = null
                manualSelectionIndices = emptySet()
            },
            onConfirm = { confirmManualPresetSelection() }
        )
    }
}

@Composable
private fun buildPresetSubtitle(
    count: Int,
    exceedsLimit: Boolean
): String {
    return if (exceedsLimit) {
        stringResource(R.string.wheel_preset_requires_selection, count)
    } else {
        stringResource(R.string.wheel_preset_items_count, count)
    }
}
