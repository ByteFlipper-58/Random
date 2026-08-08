package com.byteflipper.random.ui.teams.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.team.TeamPresetWithCount

@Composable
fun TeamPresetPickerSheetTrigger(
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(stringResource(R.string.open_team_preset))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamPresetPickerSheet(
    visible: Boolean,
    presets: List<TeamPresetWithCount>,
    onDismiss: () -> Unit,
    onOpenPreset: (Long) -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.open_team_preset),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(presets, key = { it.preset.id }) { preset ->
                TeamPresetRow(
                    item = preset,
                    onClick = {
                        onDismiss()
                        onOpenPreset(preset.preset.id)
                    }
                )
            }
        }
    }
}
