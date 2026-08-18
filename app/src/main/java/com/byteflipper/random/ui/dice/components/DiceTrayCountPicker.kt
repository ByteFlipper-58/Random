package com.byteflipper.random.ui.dice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * How many dice are in the tray, in as little room as it can be asked in.
 *
 * The flat dice ask the same question down the middle of the screen, which is space the tray does not
 * have to give: that is where the dice themselves land. Two rows of five sit above them instead, small
 * and half-transparent, so the tray reads through the gaps.
 */
@Composable
fun DiceTrayCountPicker(
    count: Int,
    onCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        COUNT_ROWS.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { value ->
                    CountButton(
                        value = value,
                        selected = value == count,
                        onClick = { onCountChange(value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CountButton(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = UNSELECTED_ALPHA)
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(TOUCH_TARGET_SIZE)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(BUTTON_SIZE)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}

/** Five and five: the widest row that still fits across the narrowest phone the app supports. */
private val COUNT_ROWS = listOf((1..5).toList(), (6..10).toList())

/** Visual circle inside the full accessibility touch target. */
private val BUTTON_SIZE = 44.dp
private val TOUCH_TARGET_SIZE = 48.dp

/** Enough to read as a button, sheer enough to read as sitting on the tray rather than over it. */
private const val UNSELECTED_ALPHA = 0.55f
