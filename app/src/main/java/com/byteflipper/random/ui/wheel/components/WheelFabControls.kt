package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.FabSizeSetting
import com.byteflipper.random.ui.components.SizedFab

@Composable
fun WheelFabControls(
    fabSize: FabSizeSetting,
    onSettingsClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Settings FAB (top) - no repeats & duration
        SmallFloatingActionButton(
            onClick = onSettingsClick,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Icon(
                painter = painterResource(id = R.drawable.settings_24px),
                contentDescription = stringResource(R.string.settings)
            )
        }

        // Edit FAB (bottom) - main FAB with configurable size
        SizedFab(
            size = fabSize,
            onClick = onEditClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                painter = painterResource(id = R.drawable.edit_24px),
                contentDescription = stringResource(R.string.wheel_edit_items)
            )
        }
    }
}
