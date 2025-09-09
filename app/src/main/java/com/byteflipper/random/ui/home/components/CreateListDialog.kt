package com.byteflipper.random.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R

@Composable
fun CreateListDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    presetCount: Int,
    onCreate: (name: String, defaultItems: List<String>) -> Unit,
    onPresetCreated: () -> Unit
) {
    val context = LocalContext.current
    var createName by rememberSaveable { mutableStateOf("") }

    if (showDialog) {
        val nextNumber = presetCount + 1
        if (createName.isBlank()) createName = "${context.getString(R.string.new_list)} $nextNumber"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.new_list)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.list_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = createName.trim().ifEmpty { "${context.getString(R.string.new_list)} $nextNumber" }
                    val items = listOf(
                        context.getString(R.string.item_1),
                        context.getString(R.string.item_2),
                        context.getString(R.string.item_3)
                    )
                    onCreate(name, items)
                    onPresetCreated()
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
