package com.byteflipper.random.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RadioOption(
    val key: String,
    val title: String,
    val summary: String? = null,
    val description: String? = null,
    val icon: Painter? = null,
    val summaryIcon: ImageVector? = null
)

@Composable
fun RadioButtonGroup(
    options: List<RadioOption>,
    selectedKey: String,
    activeIndicatorColor: Color = MaterialTheme.colorScheme.primary,
    inactiveIndicatorColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    onOptionSelected: (String) -> Unit
) {
    val selectedOption = options.find { it.key == selectedKey } ?: options.firstOrNull()

    Column {
        options.forEachIndexed { index, option ->
            // Assuming RadioButtonPreference will be available in this package after moving
            RadioButtonPreference(
                key = option.key,
                title = option.title,
                description = option.description,
                selected = selectedKey == option.key,
                icon = option.icon,
                activeIndicatorColor = activeIndicatorColor,
                inactiveIndicatorColor = inactiveIndicatorColor,
                onClick = { onOptionSelected(option.key) }
            )

            if (index < options.size - 1) {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }

        selectedOption?.let { option ->
            Spacer(modifier = Modifier.height(0.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                option.summaryIcon?.let { summaryIcon ->
                    Icon(
                        imageVector = summaryIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Ensure summary is not null before converting to string
                option.summary?.let { summaryText ->
                    Text(
                        text = summaryText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}