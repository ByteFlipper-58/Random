package com.byteflipper.random.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.byteflipper.random.data.preset.ListPreset

@Composable
fun PresetButton(
    preset: ListPreset,
    onPresetClick: (ListPreset) -> Unit,
    onRenameClick: (ListPreset) -> Unit,
    onDeleteClick: (ListPreset) -> Unit,
    onUsePreset: (ListPreset) -> Unit,
    onSharePreset: (ListPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PresetCard(
            preset = preset,
            onPresetClick = onPresetClick,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PresetQuickActionButton(
                preset = preset,
                onUsePreset = onUsePreset,
                onSharePreset = onSharePreset
            )
        }
    }
}
