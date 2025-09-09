package com.byteflipper.random.ui.numbers.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.FabSizeSetting
import com.byteflipper.random.ui.components.SizedFab

@Composable
fun NumbersFabControls(
    onConfigClick: () -> Unit,
    onGenerateClick: () -> Unit,
    onFabPositioned: (Offset, IntSize) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    var fabCenterInRoot by remember { mutableStateOf(Offset.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }

    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onConfigClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                painterResource(R.drawable.settings_24px),
                contentDescription = null
            )
        }

        Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                fabSize = coordinates.size
                val pos = coordinates.positionInRoot()
                fabCenterInRoot = Offset(pos.x + fabSize.width / 2f, pos.y + fabSize.height / 2f)
                onFabPositioned(fabCenterInRoot, fabSize)
            }
        ) {
            SizedFab(
                size = FabSizeSetting.Medium,
                onClick = onGenerateClick,
                containerColor = containerColor,
                contentColor = contentColor
            ) {
                Icon(
                    painterResource(R.drawable.autorenew_24px),
                    contentDescription = null
                )
            }
        }
    }
}


