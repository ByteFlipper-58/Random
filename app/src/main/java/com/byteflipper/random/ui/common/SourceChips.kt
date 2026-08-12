package com.byteflipper.random.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.RoundedDropdownMenuShape

/** Chip that opens the quick templates menu. */
@Composable
fun QuickTemplatesChip(
    modifier: Modifier = Modifier,
    onTemplateSelected: (QuickTemplate) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(stringResource(R.string.wheel_templates), fontSize = 12.sp) }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedDropdownMenuShape
        ) {
            QuickTemplateMenuItems { template ->
                expanded = false
                onTemplateSelected(template)
            }
        }
    }
}
