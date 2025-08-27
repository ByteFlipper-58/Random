package com.byteflipper.random.ui.components

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.byteflipper.random.data.settings.FabSizeSetting

@Composable
fun SizedFab(
    size: FabSizeSetting,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (size) {
        FabSizeSetting.Small -> SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier
        ) { content() }
        FabSizeSetting.Medium -> FloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier
        ) { content() }
        FabSizeSetting.Large -> LargeFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier
        ) { content() }
    }
}


