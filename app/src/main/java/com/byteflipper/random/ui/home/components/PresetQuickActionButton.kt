package com.byteflipper.random.ui.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
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
                painter = painterResource(id = R.drawable.play_arrow_24px),
                contentDescription = stringResource(R.string.use_preset),
                modifier = Modifier.size(18.dp)
            )
        }

        FilledTonalIconButton(
            onClick = { onSharePreset(preset) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.share_24px),
                contentDescription = stringResource(R.string.share_preset),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
