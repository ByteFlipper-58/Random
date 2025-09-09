package com.byteflipper.random.ui.dice.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.FabSizeSetting
import com.byteflipper.random.ui.components.SizedFab

@Composable
fun DiceFabControls(
    size: FabSizeSetting,
    isRolling: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isRolling)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.primaryContainer

    val contentColor = if (isRolling)
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    SizedFab(
        size = size,
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        Icon(
            painter = painterResource(R.drawable.autorenew_24px),
            contentDescription = stringResource(R.string.roll_dice)
        )
    }
}


