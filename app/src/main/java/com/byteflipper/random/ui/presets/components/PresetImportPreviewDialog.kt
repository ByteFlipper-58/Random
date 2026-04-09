package com.byteflipper.random.ui.presets.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.transfer.ParsedPresetImport

@Composable
fun PresetImportPreviewDialog(
    parsedImport: ParsedPresetImport,
    onDismiss: () -> Unit,
    onImportAsCopy: () -> Unit,
    onReplaceMatching: () -> Unit
) {
    val previewNames = parsedImport.presets.take(5).joinToString(separator = "\n") { preset ->
        "• ${preset.name} (${preset.items.size})"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_preview_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.import_preview_summary,
                        parsedImport.presets.size,
                        parsedImport.issues.size
                    )
                )

                if (parsedImport.sourceLabel != null) {
                    Text(
                        text = stringResource(R.string.import_preview_source, parsedImport.sourceLabel),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (previewNames.isNotBlank()) {
                    Text(
                        text = previewNames,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            if (parsedImport.presets.isNotEmpty()) {
                TextButton(onClick = onImportAsCopy) {
                    Text(stringResource(R.string.import_as_copy))
                }
            }
        },
        dismissButton = {
            Column(modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)) {
                if (parsedImport.presets.isNotEmpty()) {
                    TextButton(onClick = onReplaceMatching) {
                        Text(stringResource(R.string.replace_matching))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
