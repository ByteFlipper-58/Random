package com.byteflipper.random.ui.wheel.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.WHEEL_SPIN_DURATION_MAX_MS
import com.byteflipper.random.data.settings.WHEEL_SPIN_DURATION_MIN_MS
import com.byteflipper.random.data.settings.WheelUsedSectorStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelSettingsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    noRepeats: Boolean,
    onNoRepeatsChange: (Boolean) -> Unit,
    usedSectorStyle: WheelUsedSectorStyle,
    onUsedSectorStyleChange: (WheelUsedSectorStyle) -> Unit,
    spinDuration: Int,
    onSpinDurationChange: (Int) -> Unit,
    excludedCount: Int,
    totalCount: Int,
    onReset: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.wheel_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // No repeats toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.wheel_no_repeats),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.wheel_no_repeats_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = noRepeats,
                        onCheckedChange = onNoRepeatsChange
                    )
                }

                // Reset button if there are excluded items
                if (noRepeats && excludedCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.wheel_excluded_count, excludedCount, totalCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = onReset,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.refresh_24px),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(stringResource(R.string.reset))
                        }
                    }
                }

                // The style only matters with "no repeats" on, since nothing gets used up without it.
                AnimatedVisibility(
                    visible = noRepeats,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.wheel_used_sector_style),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            WheelUsedSectorStyle.entries.forEachIndexed { index, style ->
                                SegmentedButton(
                                    selected = usedSectorStyle == style,
                                    onClick = { onUsedSectorStyleChange(style) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = WheelUsedSectorStyle.entries.size
                                    )
                                ) {
                                    Text(
                                        text = stringResource(
                                            when (style) {
                                                WheelUsedSectorStyle.Dim -> R.string.wheel_used_sector_dim
                                                WheelUsedSectorStyle.Remove -> R.string.wheel_used_sector_remove
                                            }
                                        ),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Spin duration
                Text(
                    text = stringResource(R.string.wheel_spin_duration),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                // The slider is dragged locally and only the final value is reported, otherwise
                // every step of the drag would hit DataStore.
                var sliderValue by remember(spinDuration) {
                    mutableFloatStateOf(spinDuration.toFloat())
                }

                Text(
                    text = stringResource(
                        R.string.wheel_spin_duration_value,
                        (sliderValue / 1000f).toInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onSpinDurationChange(sliderValue.toInt()) },
                    valueRange = WHEEL_SPIN_DURATION_MIN_MS.toFloat()..WHEEL_SPIN_DURATION_MAX_MS.toFloat(),
                    steps = 12,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "3s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "16s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
