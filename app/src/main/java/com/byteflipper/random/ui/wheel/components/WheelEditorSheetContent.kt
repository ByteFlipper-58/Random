package com.byteflipper.random.ui.wheel.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.common.MenuBackItem
import com.byteflipper.random.ui.common.PeopleSectionItem
import com.byteflipper.random.ui.common.PeopleSourceMenuItems
import com.byteflipper.random.ui.common.QuickTemplatesChip
import com.byteflipper.random.ui.common.SourceMenuSection
import com.byteflipper.random.ui.components.RoundedDropdownMenuShape
import com.byteflipper.random.ui.wheel.WHEEL_MIN_ITEMS
import kotlinx.coroutines.delay

private const val MAX_ITEM_LENGTH = 30

/** Recoloring of the field border when switching between add and edit modes. */
private const val EDIT_MODE_DURATION_MS = 320

/** Collapsing of a chip before the item actually leaves the list. */
private const val CHIP_EXIT_DURATION_MS = 220

/**
 * Width of the input field. A spring rather than a tween: with a tween the field jerked on the way
 * back from edit mode, because its growth was cut off exactly at the target width.
 */
private val FIELD_RESIZE_SPEC = spring<IntSize>(
    dampingRatio = 0.8f,
    stiffness = Spring.StiffnessMediumLow
)

private val ICON_SCALE_SPEC = spring<Float>(
    dampingRatio = 0.65f,
    stiffness = Spring.StiffnessMediumLow
)

private val ICON_FADE_SPEC = tween<Float>(durationMillis = 220)

private val CHIP_ENTER_SPEC = spring<Float>(
    dampingRatio = 0.72f,
    stiffness = Spring.StiffnessMediumLow
)

private val CHIP_ENTER_SIZE_SPEC = spring<IntSize>(
    dampingRatio = 0.85f,
    stiffness = Spring.StiffnessMediumLow
)

private val CHIP_EXIT_SPEC = tween<Float>(durationMillis = CHIP_EXIT_DURATION_MS)

private val CHIP_EXIT_SIZE_SPEC = tween<IntSize>(durationMillis = CHIP_EXIT_DURATION_MS)

private val CHIP_ENTER_SPEC_SIZE = spring<IntSize>(
    dampingRatio = 0.85f,
    stiffness = Spring.StiffnessMediumLow
)

private val CHIP_RELAYOUT_SPEC = spring<IntSize>(
    dampingRatio = 0.85f,
    stiffness = Spring.StiffnessMediumLow
)

/** Gap between chip rows. See the note at the FlowRow about the touch target. */
private val CHIP_ROW_SPACING = 6.dp

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
    editingIndex: Int,
    editingText: String,
    onEditingTextChange: (String) -> Unit,
    onShowClearConfirm: () -> Unit,
    onShowSaveDialog: () -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onStartEdit: (Int) -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onLoadPresetClick: (ListPreset) -> Unit,
    onLoadTemplate: (List<String>) -> Unit,
    onLoadPeople: (sourceName: String, names: List<String>) -> Unit,
    canSaveAsPreset: Boolean
) {
    val isEditing = editingIndex >= 0
    val canRemoveItems = items.size > WHEEL_MIN_ITEMS

    // Which section of the presets menu is open: the presets themselves or people.
    var presetMenuSection by remember { mutableStateOf(SourceMenuSection.Root) }

    // Chip that is collapsing before its item is removed.
    var removingIndex by remember { mutableIntStateOf(-1) }

    // Recreates the chip visibility states after a removal. Without it the chip that the next item
    // shifted into would animate in, as if the deleted item came back under a new name.
    var chipGeneration by remember { mutableIntStateOf(0) }

    // Only a genuinely appended chip animates in, so opening the sheet or loading a template does
    // not pop every chip at once.
    var knownItemCount by remember { mutableIntStateOf(items.size) }
    val appendedIndex = if (items.size == knownItemCount + 1) items.lastIndex else -1

    LaunchedEffect(items.size) {
        knownItemCount = items.size
    }

    LaunchedEffect(removingIndex) {
        val index = removingIndex
        if (index >= 0) {
            delay(CHIP_EXIT_DURATION_MS.toLong())
            onRemoveItem(index)
            removingIndex = -1
            chipGeneration++
        }
    }

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
                    AnimatedContent(
                        targetState = items.size,
                        transitionSpec = {
                            val forward = targetState > initialState
                            (slideInVertically(tween(EDIT_MODE_DURATION_MS)) { height ->
                                if (forward) height else -height
                            } + fadeIn(tween(EDIT_MODE_DURATION_MS)))
                                .togetherWith(
                                    slideOutVertically(tween(EDIT_MODE_DURATION_MS)) { height ->
                                        if (forward) -height else height
                                    } + fadeOut(tween(EDIT_MODE_DURATION_MS))
                                )
                        },
                        label = "items_count"
                    ) { count ->
                        Text(
                            text = stringResource(
                                R.string.wheel_items_limit,
                                count,
                                wheelPresetLimit
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Explains why the delete buttons went dim, otherwise they read as broken.
                    AnimatedVisibility(
                        visible = !canRemoveItems,
                        enter = fadeIn(ICON_FADE_SPEC) + expandVertically(CHIP_ENTER_SPEC_SIZE),
                        exit = fadeOut(ICON_FADE_SPEC) + shrinkVertically(CHIP_ENTER_SPEC_SIZE)
                    ) {
                        Text(
                            text = stringResource(R.string.wheel_min_items_locked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
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
                        }
                    )

                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = {
                            onShowPresetMenuChange(false)
                            presetMenuSection = SourceMenuSection.Root
                        },
                        shape = RoundedDropdownMenuShape
                    ) {
                        // People are a section of this same menu, with a way back to the root.
                        if (presetMenuSection == SourceMenuSection.People) {
                            MenuBackItem(onClick = { presetMenuSection = SourceMenuSection.Root })

                            PeopleSourceMenuItems(minItems = WHEEL_MIN_ITEMS) { sourceName, names ->
                                onShowPresetMenuChange(false)
                                presetMenuSection = SourceMenuSection.Root
                                onLoadPeople(sourceName, names)
                            }

                            return@DropdownMenu
                        }

                        PeopleSectionItem(
                            onClick = { presetMenuSection = SourceMenuSection.People }
                        )

                        if (presets.isNotEmpty()) {
                            HorizontalDivider()
                        }

                        presets.forEach { preset ->
                            // A single item preset does not fit the wheel: show why and block it.
                            val presetFitsWheel = preset.items.size >= WHEEL_MIN_ITEMS

                            DropdownMenuItem(
                                enabled = presetFitsWheel,
                                text = {
                                    Column {
                                        Text(preset.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            buildPresetSubtitle(
                                                count = preset.items.size,
                                                exceedsLimit = preset.items.size > wheelPresetLimit,
                                                belowMinimum = !presetFitsWheel
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

                QuickTemplatesChip(
                    onTemplateSelected = { template -> onLoadTemplate(template.items) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // One field for both modes: it stays in place and shrinks because the row of buttons to
            // its right changes width with an animation.
            val focusedBorderColor by animateColorAsState(
                targetValue = if (isEditing) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                animationSpec = tween(EDIT_MODE_DURATION_MS),
                label = "field_focused_border"
            )
            val unfocusedBorderColor by animateColorAsState(
                targetValue = if (isEditing) {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                animationSpec = tween(EDIT_MODE_DURATION_MS),
                label = "field_unfocused_border"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (isEditing) editingText else newItemText,
                    onValueChange = { value ->
                        if (value.length <= MAX_ITEM_LENGTH) {
                            if (isEditing) onEditingTextChange(value) else onNewItemTextChange(value)
                        }
                    },
                    label = if (isEditing) {
                        { Text(stringResource(R.string.wheel_edit_item, editingIndex + 1)) }
                    } else {
                        null
                    },
                    placeholder = if (isEditing) {
                        null
                    } else {
                        {
                            Text(
                                stringResource(R.string.wheel_add_new_item),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (isEditing) onConfirmEdit() else onAddItem() }
                    ),
                    enabled = isEditing || items.size < wheelPresetLimit,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = focusedBorderColor,
                        unfocusedBorderColor = unfocusedBorderColor
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    // The row animates its width and the field takes whatever is left through
                    // weight(1f), so it shrinks smoothly when the second button appears.
                    modifier = Modifier.animateContentSize(animationSpec = FIELD_RESIZE_SPEC)
                ) {
                    FilledTonalIconButton(
                        onClick = { if (isEditing) onConfirmEdit() else onAddItem() },
                        enabled = if (isEditing) {
                            editingText.isNotBlank()
                        } else {
                            newItemText.isNotBlank() && items.size < wheelPresetLimit
                        }
                    ) {
                        AnimatedContent(
                            targetState = isEditing,
                            transitionSpec = {
                                (fadeIn(ICON_FADE_SPEC) + scaleIn(ICON_SCALE_SPEC, initialScale = 0.7f))
                                    .togetherWith(
                                        fadeOut(ICON_FADE_SPEC) +
                                            scaleOut(ICON_SCALE_SPEC, targetScale = 0.7f)
                                    )
                            },
                            label = "primary_action_icon"
                        ) { editing ->
                            Icon(
                                painter = painterResource(
                                    id = if (editing) R.drawable.check_24px else R.drawable.add_24px
                                ),
                                contentDescription = stringResource(
                                    if (editing) R.string.confirm else R.string.add
                                )
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isEditing,
                        enter = fadeIn(ICON_FADE_SPEC) + scaleIn(ICON_SCALE_SPEC, initialScale = 0.6f),
                        exit = fadeOut(ICON_FADE_SPEC) + scaleOut(ICON_SCALE_SPEC, targetScale = 0.6f)
                    ) {
                        // At the minimum the button stays put and only dims: a disappearing button
                        // would read as a feature that went missing.
                        IconButton(
                            onClick = { onRemoveItem(editingIndex) },
                            enabled = canRemoveItems
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.delete_24px),
                                contentDescription = stringResource(R.string.delete),
                                tint = if (canRemoveItems) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            }

            // M3 chips inflate their layout to a 48.dp touch target, which leaves ~16.dp of empty
            // space between rows even at zero spacing. Dropping the minimum lets a row take its
            // real 32.dp, so rows sit exactly CHIP_ROW_SPACING apart. The cost is a touch target
            // below 48.dp, which is acceptable here because the chips are wide.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = CHIP_RELAYOUT_SPEC),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(CHIP_ROW_SPACING)
            ) {
                items.forEachIndexed { index, item ->
                    key(index) {
                    val isExcluded = index in excludedIndices
                    val isEditingThis = index == editingIndex

                    // A freshly appended chip starts hidden and animates in; the rest are visible
                    // straight away.
                    val chipVisibility = remember(chipGeneration) {
                        MutableTransitionState(index != appendedIndex)
                    }
                    chipVisibility.targetState = index != removingIndex

                    AnimatedVisibility(
                        visibleState = chipVisibility,
                        enter = fadeIn(CHIP_ENTER_SPEC) +
                            scaleIn(CHIP_ENTER_SPEC, initialScale = 0.8f) +
                            expandHorizontally(CHIP_ENTER_SIZE_SPEC, clip = false),
                        // One duration for the whole exit, so the item leaves the list exactly when
                        // the animation ends. A spring did not finish in time and the chip vanished
                        // mid-movement.
                        exit = fadeOut(CHIP_EXIT_SPEC) +
                            scaleOut(CHIP_EXIT_SPEC, targetScale = 0.8f) +
                            shrinkHorizontally(CHIP_EXIT_SIZE_SPEC)
                    ) {
                    InputChip(
                        selected = isEditingThis,
                        onClick = {
                            when {
                                // Tapping the edited chip again cancels the edit, since the delete
                                // button took the place of a cancel button.
                                isEditingThis -> onCancelEdit()
                                // Used items are editable too: having come up once is no reason to
                                // forbid renaming.
                                editingIndex < 0 -> onStartEdit(index)
                            }
                        },
                        label = {
                            Text(
                                text = item,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isEditingThis -> MaterialTheme.colorScheme.primary
                                    isExcluded -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        },
                        // Dot in the color of the sector this item is drawn with, which ties the
                        // chip to the wheel. The palette is keyed by item index, so it does not
                        // drift between spins.
                        leadingIcon = {
                            val sectorColor = wheelColors[index % wheelColors.size]
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (isExcluded) {
                                            sectorColor.copy(alpha = 0.3f)
                                        } else {
                                            sectorColor
                                        },
                                        shape = CircleShape
                                    )
                            )
                        },
                        trailingIcon = {
                            if (editingIndex < 0) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        // Removal is not instant: the chip collapses first and only
                                        // then does the item leave the list.
                                        .clickable(enabled = canRemoveItems) {
                                            removingIndex = index
                                        }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.delete_24px),
                                        contentDescription = stringResource(R.string.delete),
                                        modifier = Modifier.size(18.dp),
                                        tint = if (canRemoveItems) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.38f)
                                        }
                                    )
                                }
                            }
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = when {
                                isEditingThis -> MaterialTheme.colorScheme.primaryContainer
                                isExcluded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        border = InputChipDefaults.inputChipBorder(
                            enabled = true,
                            selected = isEditingThis,
                            borderColor = when {
                                isEditingThis -> MaterialTheme.colorScheme.primary
                                isExcluded -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            },
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    }
                    }
                }
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
    exceedsLimit: Boolean,
    belowMinimum: Boolean
): String {
    return when {
        belowMinimum -> stringResource(R.string.wheel_min_items_locked)
        exceedsLimit -> stringResource(R.string.wheel_preset_requires_selection, count)
        else -> stringResource(R.string.wheel_preset_items_count, count)
    }
}
