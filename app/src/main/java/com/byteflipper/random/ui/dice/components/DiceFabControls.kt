package com.byteflipper.random.ui.dice.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.FabSizeSetting
import com.byteflipper.random.ui.components.SizedFab

@Composable
fun DiceFabControls(
    size: FabSizeSetting,
    isRolling: Boolean,
    onSettingsClick: () -> Unit,
    onRollClick: () -> Unit
) {
    val containerColor = if (isRolling)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.primaryContainer

    val contentColor = if (isRolling)
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onSettingsClick,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Icon(
                painter = painterResource(R.drawable.settings_24px),
                contentDescription = stringResource(R.string.settings)
            )
        }

        SizedFab(
            size = size,
            onClick = { if (!isRolling) onRollClick() },
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = Modifier.semantics {
                if (isRolling) disabled()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.autorenew_24px),
                contentDescription = stringResource(R.string.roll_dice)
            )
        }
    }
}
