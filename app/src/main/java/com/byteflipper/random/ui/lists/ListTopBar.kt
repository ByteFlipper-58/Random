package com.byteflipper.random.ui.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.common.MenuBackItem
import com.byteflipper.random.ui.common.PeopleSourceMenuItems
import com.byteflipper.random.ui.common.QuickTemplateMenuItems
import com.byteflipper.random.ui.common.SourceMenuSection
import com.byteflipper.random.ui.components.RoundedDropdownMenuShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListTopBar(
    onBack: () -> Unit,
    title: String,
    presets: List<ListPreset>,
    onAddItems: (List<String>) -> Unit,
    onShowSave: (() -> Unit)?,
    onShowSaveResults: (() -> Unit)?,
    onShowRename: (() -> Unit)?
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf(SourceMenuSection.Root) }

    fun closeMenu() {
        menuExpanded = false
        section = SourceMenuSection.Root
    }

    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            // Everything lives under the overflow, as on the Teams screen: the bar held too many icons.
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    painter = painterResource(R.drawable.more_vert_24px),
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { closeMenu() },
                shape = RoundedDropdownMenuShape
            ) {
                when (section) {
                    SourceMenuSection.People -> {
                        MenuBackItem(onClick = { section = SourceMenuSection.Root })
                        PeopleSourceMenuItems { _, names ->
                            closeMenu()
                            onAddItems(names)
                        }
                    }

                    SourceMenuSection.Templates -> {
                        MenuBackItem(onClick = { section = SourceMenuSection.Root })
                        QuickTemplateMenuItems { template ->
                            closeMenu()
                            onAddItems(template.items)
                        }
                    }

                    SourceMenuSection.Presets -> {
                        MenuBackItem(onClick = { section = SourceMenuSection.Root })
                        if (presets.isEmpty()) {
                            DropdownMenuItem(
                                enabled = false,
                                text = { Text(stringResource(R.string.wheel_no_items)) },
                                onClick = {}
                            )
                        }
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(preset.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = stringResource(
                                                R.string.wheel_preset_items_count,
                                                preset.items.size
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.list_alt_24px),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    closeMenu()
                                    onAddItems(preset.items)
                                }
                            )
                        }
                    }

                    SourceMenuSection.Root -> {
                        if (onShowSave != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save)) },
                                onClick = {
                                    closeMenu()
                                    onShowSave()
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.save_24px),
                                        contentDescription = null
                                    )
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.wheel_presets)) },
                            onClick = { section = SourceMenuSection.Presets },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.folder_open_24px),
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.people)) },
                            onClick = { section = SourceMenuSection.People },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.groups_24px),
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.wheel_templates)) },
                            onClick = { section = SourceMenuSection.Templates },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.list_alt_24px),
                                    contentDescription = null
                                )
                            }
                        )

                        // Rename and save-results are kept because nothing else leads to them, but
                        // separated so they stay out of the main four.
                        if (onShowRename != null || onShowSaveResults != null) {
                            HorizontalDivider()
                        }

                        if (onShowSaveResults != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save_results_as_preset)) },
                                onClick = {
                                    closeMenu()
                                    onShowSaveResults()
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.content_copy_24px),
                                        contentDescription = null
                                    )
                                }
                            )
                        }

                        if (onShowRename != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename)) },
                                onClick = {
                                    closeMenu()
                                    onShowRename()
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.edit_24px),
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
