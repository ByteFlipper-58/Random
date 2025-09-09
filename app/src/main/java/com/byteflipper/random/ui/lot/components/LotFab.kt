package com.byteflipper.random.ui.lot.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.FabSizeSetting
import com.byteflipper.random.ui.components.SizedFab

@Composable
fun LotFab(
    size: FabSizeSetting,
    mode: LotFabMode,
    onClick: () -> Unit
) {
    SizedFab(
        size = size,
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        when (mode) {
            LotFabMode.RevealAll -> Icon(
                painterResource(R.drawable.check_24px),
                contentDescription = stringResource(R.string.show_all)
            )
            LotFabMode.Randomize -> Icon(
                painterResource(R.drawable.autorenew_24px),
                contentDescription = stringResource(R.string.reshuffle)
            )
        }
    }
}


