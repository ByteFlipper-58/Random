package com.byteflipper.random.ui.presets.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.transfer.PresetTransferFormat

private data class SheetAction(
    val title: String,
    val emphasized: Boolean,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetExportFormatDialog(
    titleRes: Int,
    preferredFormat: PresetTransferFormat,
    showCopyAction: Boolean = false,
    onDismiss: () -> Unit,
    onSelectFormat: (PresetTransferFormat) -> Unit,
    onCopyClick: (() -> Unit)? = null
) {
    val formats = PresetTransferFormat.entries.sortedBy { format ->
        if (format == preferredFormat) 0 else 1
    }
    val actions = buildList {
        formats.forEach { format ->
            add(
                SheetAction(
                    title = stringResource(format.titleRes()),
                    emphasized = format == preferredFormat,
                    onClick = { onSelectFormat(format) }
                )
            )
        }

        if (showCopyAction && onCopyClick != null) {
            add(
                SheetAction(
                    title = stringResource(R.string.copy),
                    emphasized = false,
                    onClick = onCopyClick
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    actions.forEachIndexed { index, action ->
                        ActionCard(
                            title = action.title,
                            emphasized = action.emphasized && index != 0,
                            shape = actionShape(index, actions.lastIndex),
                            onClick = action.onClick
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

private fun PresetTransferFormat.titleRes(): Int = when (this) {
    PresetTransferFormat.Json -> R.string.format_json
    PresetTransferFormat.Txt -> R.string.format_txt
    PresetTransferFormat.Csv -> R.string.format_csv
}

@Composable
private fun ActionCard(
    title: String,
    emphasized: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (emphasized) {
        lerp(colorScheme.primaryContainer, colorScheme.secondaryContainer, 0.16f)
    } else {
        colorScheme.surfaceContainerLow
    }

    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        shape = shape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor
        )
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 18.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasized) {
                lerp(colorScheme.onPrimaryContainer, colorScheme.onSecondaryContainer, 0.18f)
            } else {
                colorScheme.onSurface
            }
        )
    }
}

private fun actionShape(index: Int, lastIndex: Int): RoundedCornerShape {
    return when {
        lastIndex <= 0 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        index == lastIndex -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(0.dp)
    }
}
