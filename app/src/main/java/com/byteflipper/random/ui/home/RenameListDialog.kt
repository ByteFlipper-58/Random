package com.byteflipper.random.ui.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset

@Composable
fun RenameListDialog(
    preset: ListPreset?,
    onDismiss: () -> Unit,
    onRename: (ListPreset, String) -> Unit,
    onPresetRenamed: () -> Unit
) {
    var renameName by rememberSaveable { mutableStateOf(preset?.name ?: "") }

    if (preset != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.rename_list)) },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.new_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = renameName.trim()
                    if (newName.isNotEmpty()) {
                        onRename(preset, newName)
                        onPresetRenamed()
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
