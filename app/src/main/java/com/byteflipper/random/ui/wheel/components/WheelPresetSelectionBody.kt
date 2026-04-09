package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset

@Composable
internal fun WheelPresetSelectionBody(
    preset: ListPreset,
    selectedIndices: Set<Int>,
    itemLimit: Int,
    selectedCount: Int,
    listState: LazyListState,
    onSelectionChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WheelPresetSelectionHeader(
            presetName = preset.name,
            selectedCount = selectedCount,
            itemLimit = itemLimit
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 176.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = preset.items,
                    key = { index, item -> "${preset.id}_${index}_$item" }
                ) { index, item ->
                    val isSelected = index in selectedIndices
                    val isEnabled = isSelected || selectedCount < itemLimit

                    WheelPresetSelectableItem(
                        index = index + 1,
                        label = item,
                        selected = isSelected,
                        enabled = isEnabled,
                        onClick = { onSelectionChange(index) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun WheelPresetSelectionHeader(
    presetName: String,
    selectedCount: Int,
    itemLimit: Int
) {
    Text(
        text = stringResource(R.string.wheel_preset_manual_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = presetName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = lerp(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
                0.25f
            )
        ) {
            Text(
                text = stringResource(
                    R.string.wheel_preset_manual_message,
                    selectedCount,
                    itemLimit
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = lerp(
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    0.2f
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                maxLines = 1
            )
        }
    }
}
