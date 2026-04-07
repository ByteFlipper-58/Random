package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WheelSheetDragHandle() {
    Surface(
        modifier = Modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(modifier = Modifier.size(width = 48.dp, height = 5.dp))
    }
}
