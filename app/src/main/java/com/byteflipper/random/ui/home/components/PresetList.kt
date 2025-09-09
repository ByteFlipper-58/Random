package com.byteflipper.random.ui.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.byteflipper.random.data.preset.ListPreset

@Composable
fun PresetList(
    presets: List<ListPreset>,
    onPresetClick: (ListPreset) -> Unit,
    onRenamePreset: (ListPreset) -> Unit,
    onDeletePreset: (ListPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(presets, key = { it.id }) { preset ->
            PresetCard(
                preset = preset,
                onPresetClick = onPresetClick,
                onRenameClick = onRenamePreset,
                onDeleteClick = onDeletePreset
            )
        }
    }
}
