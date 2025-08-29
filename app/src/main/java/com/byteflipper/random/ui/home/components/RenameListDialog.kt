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
import com.byteflipper.random.data.preset.ListPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.byteflipper.random.data.preset.ListPresetRepository

@Composable
fun RenameListDialog(
    preset: ListPreset?,
    onDismiss: () -> Unit,
    repository: ListPresetRepository,
    coroutineScope: CoroutineScope,
    onPresetRenamed: () -> Unit
) {
    var renameName by rememberSaveable { mutableStateOf(preset?.name ?: "") }

    if (preset != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Переименовать список") },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    singleLine = true,
                    label = { Text("Новое название") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = renameName.trim()
                    if (newName.isNotEmpty()) {
                        coroutineScope.launch {
                            repository.upsert(preset.copy(name = newName))
                            onPresetRenamed()
                        }
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        )
    }
}
