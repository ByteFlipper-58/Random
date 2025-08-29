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
import androidx.compose.ui.unit.dp
import com.byteflipper.random.data.preset.ListPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.byteflipper.random.data.preset.ListPresetRepository

@Composable
fun CreateListDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    presetCount: Int,
    repository: ListPresetRepository,
    coroutineScope: CoroutineScope,
    onPresetCreated: () -> Unit
) {
    var createName by rememberSaveable { mutableStateOf("") }

    if (showDialog) {
        val nextNumber = presetCount + 1
        if (createName.isBlank()) createName = "Новый список $nextNumber"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Новый список") },
            text = {
                Column {
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        singleLine = true,
                        label = { Text("Название списка") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = createName.trim().ifEmpty { "Новый список $nextNumber" }
                    coroutineScope.launch {
                        repository.upsert(
                            ListPreset(
                                name = name,
                                items = listOf("Элемент 1", "Элемент 2", "Элемент 3")
                            )
                        )
                        onPresetCreated()
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        )
    }
}
