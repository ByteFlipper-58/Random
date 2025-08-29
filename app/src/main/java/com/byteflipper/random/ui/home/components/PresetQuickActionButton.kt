package com.byteflipper.random.ui.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.byteflipper.random.data.preset.ListPreset

@Composable
fun PresetQuickActionButton(
    preset: ListPreset,
    onUsePreset: (ListPreset) -> Unit,
    onSharePreset: (ListPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        FilledTonalIconButton(
            onClick = { onUsePreset(preset) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Использовать пресет",
                modifier = Modifier.size(18.dp)
            )
        }

        FilledTonalIconButton(
            onClick = { onSharePreset(preset) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Поделиться пресетом",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
