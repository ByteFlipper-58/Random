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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.components.RoundedDropdownMenuShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun WheelEditorSheetContent(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    items: List<String>,
    excludedIndices: Set<Int>,
    presets: List<ListPreset>,
    wheelPresetLimit: Int,
    newItemText: String,
    onNewItemTextChange: (String) -> Unit,
    showPresetMenu: Boolean,
    onShowPresetMenuChange: (Boolean) -> Unit,
    showTemplatesMenu: Boolean,
    onShowTemplatesMenuChange: (Boolean) -> Unit,
    editingIndex: Int,
    editingText: String,
    onEditingTextChange: (String) -> Unit,
    quickTemplates: List<Pair<String, List<String>>>,
    onShowClearConfirm: () -> Unit,
    onShowSaveDialog: () -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onStartEdit: (Int) -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onLoadPresetClick: (ListPreset) -> Unit,
    onLoadTemplate: (List<String>) -> Unit,
    canSaveAsPreset: Boolean
) {
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
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
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
                    IconButton(
                        onClick = onShowClearConfirm,
                        enabled = items.size > 1
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.delete_sweep_24px),
                            contentDescription = stringResource(R.string.wheel_clear_all),
                            tint = if (items.size > 1) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    }

                    if (canSaveAsPreset) {
                        IconButton(
                            onClick = onShowSaveDialog,
                            enabled = items.isNotEmpty()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.save_24px),
                                contentDescription = stringResource(R.string.wheel_save_as_preset)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    AssistChip(
                        onClick = { onShowPresetMenuChange(true) },
                        label = { Text(stringResource(R.string.wheel_presets), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.folder_open_24px),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        enabled = presets.isNotEmpty()
                    )

                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { onShowPresetMenuChange(false) },
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
                                onClick = { onLoadPresetClick(preset) }
                            )
                        }
                    }
                }

                Box {
                    AssistChip(
                        onClick = { onShowTemplatesMenuChange(true) },
                        label = { Text(stringResource(R.string.wheel_templates), fontSize = 12.sp) }
                    )

                    DropdownMenu(
                        expanded = showTemplatesMenu,
                        onDismissRequest = { onShowTemplatesMenuChange(false) },
                        shape = RoundedDropdownMenuShape
                    ) {
                        quickTemplates.forEach { (name, template) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(name, fontWeight = FontWeight.Medium)
                                        Text(
                                            template.take(4).joinToString(", ") +
                                                if (template.size > 4) "..." else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = { onLoadTemplate(template) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            if (editingIndex < 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { if (it.length <= 30) onNewItemTextChange(it) },
                        placeholder = {
                            Text(
                                stringResource(R.string.wheel_add_new_item),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onAddItem() }),
                        enabled = items.size < wheelPresetLimit,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalIconButton(
                        onClick = onAddItem,
                        enabled = newItemText.isNotBlank() && items.size < wheelPresetLimit
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.add_24px),
                            contentDescription = stringResource(R.string.add)
                        )
                    }
                }
            }

            if (editingIndex >= 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editingText,
                        onValueChange = { if (it.length <= 30) onEditingTextChange(it) },
                        label = { Text(stringResource(R.string.wheel_edit_item, editingIndex + 1)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onConfirmEdit() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalIconButton(
                        onClick = onConfirmEdit,
                        enabled = editingText.isNotBlank()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.check_24px),
                            contentDescription = stringResource(R.string.confirm)
                        )
                    }

                    IconButton(onClick = onCancelEdit) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_24px),
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            }

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
                                onStartEdit(index)
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
                                    painter = painterResource(id = R.drawable.edit_24px),
                                    contentDescription = stringResource(R.string.edit),
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            null
                        },
                        trailingIcon = {
                            if (!isExcluded && items.size > 1 && editingIndex < 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onRemoveItem(index) }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.close_24px),
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
