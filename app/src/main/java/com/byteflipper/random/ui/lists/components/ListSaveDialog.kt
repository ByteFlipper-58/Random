package com.byteflipper.random.ui.lists.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R

@Composable
fun ListSaveDialog(
    currentName: String,
    presetCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (name: String, openAfterSave: Boolean) -> Unit
) {
    var saveName by remember { mutableStateOf(currentName) }
    var openAfterSave by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_settings)) },
        text = {
            Column {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.list_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = openAfterSave,
                        onCheckedChange = { openAfterSave = it }
                    )
                    Text(
                        stringResource(R.string.open_after_save),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = saveName.trim()
                    if (trimmedName.isNotEmpty()) {
                        onConfirm(trimmedName, openAfterSave)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
