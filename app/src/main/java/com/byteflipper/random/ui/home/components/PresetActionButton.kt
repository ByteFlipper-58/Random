package com.byteflipper.random.ui.home.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import com.byteflipper.random.data.preset.ListPreset

@Composable
fun PresetActionButton(
    preset: ListPreset,
    onRenameClick: (ListPreset) -> Unit,
    onDeleteClick: (ListPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Переименовать") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            onClick = {
                expanded = false
                onRenameClick(preset)
            }
        )
        DropdownMenuItem(
            text = { Text("Удалить") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = {
                expanded = false
                onDeleteClick(preset)
            }
        )
    }

    IconButton(
        onClick = { expanded = true },
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = "Действия с пресетом",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
