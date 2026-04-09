package com.byteflipper.random.ui.presets.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.components.RoundedDropdownMenuShape

@Composable
fun RowScope.PresetManagerActions(
    preset: ListPreset,
    onTogglePinned: () -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = !expanded }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.more_vert_24px),
                contentDescription = stringResource(R.string.preset_actions),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedDropdownMenuShape
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(if (preset.isPinned) R.string.unpin else R.string.pin)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(
                            id = if (preset.isPinned) {
                                R.drawable.keep_off_24px
                            } else {
                                R.drawable.keep_filled_24px
                            }
                        ),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onTogglePinned()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy_as)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.content_copy_24px),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onCopy()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.upload_24px),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onExport()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.share_24px),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onShare()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.duplicate)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.content_copy_24px),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onDuplicate()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.edit_24px),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onRename()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.delete_24px),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}
