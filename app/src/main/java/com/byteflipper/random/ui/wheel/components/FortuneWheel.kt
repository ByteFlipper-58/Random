package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FortuneWheel(
    items: List<String>,
    excludedIndices: Set<Int>,
    rotation: Float,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    onClick: () -> Unit = {}
) {
    val visibleItems = remember(items, excludedIndices) {
        items.mapIndexedNotNull { index, item ->
            if (index in excludedIndices) null else index to item
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        FortuneWheelCanvas(
            visibleItems = visibleItems,
            rotation = rotation
        )

        FortuneWheelPointer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 8.dp)
                .size(32.dp, 44.dp)
        )
    }
}
