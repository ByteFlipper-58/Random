package com.byteflipper.random.ui.teams.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.FabSizeSetting
import com.byteflipper.random.ui.components.SizedFab

@Composable
fun TeamsFabControls(
    size: FabSizeSetting,
    onConfigClick: () -> Unit,
    onGenerateClick: () -> Unit,
    onFabPositioned: (Offset) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onConfigClick,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Icon(
                painter = painterResource(R.drawable.settings_24px),
                contentDescription = stringResource(R.string.split_config_section)
            )
        }
        Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val sizePx = coordinates.size
                onFabPositioned(
                    Offset(
                        x = position.x + sizePx.width / 2f,
                        y = position.y + sizePx.height / 2f
                    )
                )
            }
        ) {
            SizedFab(
                size = size,
                onClick = onGenerateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle_24px),
                    contentDescription = stringResource(R.string.distribute)
                )
            }
        }
    }
}
