package com.byteflipper.random.ui.presets.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.components.RoundedDropdownMenuShape

@Composable
fun RowScope.PresetManagerActions(
    preset: ListPreset,
    onTogglePinned: () -> Unit,
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
                imageVector = Icons.Outlined.MoreVert,
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
                text = { Text(stringResource(if (preset.isPinned) R.string.unpin_preset else R.string.pin_preset)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onTogglePinned()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.duplicate)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
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
                        imageVector = Icons.Outlined.Edit,
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
                        imageVector = Icons.Outlined.Delete,
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
