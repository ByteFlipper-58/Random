package com.byteflipper.random.ui.finger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.ConfigDivider
import com.byteflipper.random.ui.components.ConfigHeader
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerSettingsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    mode: FingerMode,
    winnerCount: Int,
    onWinnerCountChange: (Int) -> Unit,
    teamCount: Int,
    onTeamCountChange: (Int) -> Unit,
    holdDurationMs: Long,
    onHoldDurationChange: (Long) -> Unit,
    holdResultEnabled: Boolean,
    onHoldResultEnabledChange: (Boolean) -> Unit,
    resultHoldDurationSeconds: Int,
    onResultHoldDurationSecondsChange: (Int) -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    modifier = Modifier.size(width = 32.dp, height = 4.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ConfigHeader()
            ConfigDivider()

            // Winner Mode Settings
            AnimatedVisibility(
                visible = mode == FingerMode.WINNER,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FingerSectionLayout(
                        icon = painterResource(id = R.drawable.star_shine_24px),
                        title = stringResource(R.string.finger_winner_count),
                        description = stringResource(R.string.finger_winner_count)
                    ) {
                        var sliderVal by remember(winnerCount) { mutableFloatStateOf(winnerCount.toFloat()) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = winnerCount == 1,
                                onClick = {
                                    sliderVal = 1f
                                    onWinnerCountChange(1)
                                },
                                label = { Text("1") }
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text(
                                    text = sliderVal.roundToInt().toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }

                            FilterChip(
                                selected = winnerCount == 5,
                                onClick = {
                                    sliderVal = 5f
                                    onWinnerCountChange(5)
                                },
                                label = { Text("5") }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = sliderVal,
                            onValueChange = {
                                sliderVal = it
                                onWinnerCountChange(it.roundToInt().coerceIn(1, 5))
                            },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    ConfigDivider()
                }
            }

            // Teams Mode Settings
            AnimatedVisibility(
                visible = mode == FingerMode.TEAMS,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FingerSectionLayout(
                        icon = painterResource(id = R.drawable.groups_24px),
                        title = stringResource(R.string.finger_team_count),
                        description = stringResource(R.string.finger_team_count)
                    ) {
                        var sliderVal by remember(teamCount) { mutableFloatStateOf(teamCount.toFloat()) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = teamCount == 2,
                                onClick = {
                                    sliderVal = 2f
                                    onTeamCountChange(2)
                                },
                                label = { Text("2") }
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text(
                                    text = sliderVal.roundToInt().toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }

                            FilterChip(
                                selected = teamCount == 4,
                                onClick = {
                                    sliderVal = 4f
                                    onTeamCountChange(4)
                                },
                                label = { Text("4") }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = sliderVal,
                            onValueChange = {
                                sliderVal = it
                                onTeamCountChange(it.roundToInt().coerceIn(2, 4))
                            },
                            valueRange = 2f..4f,
                            steps = 1,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    ConfigDivider()
                }
            }

            // Hold Duration Section (Задержка удержания с "с")
            FingerSectionLayout(
                icon = painterResource(id = R.drawable.timer_24px),
                title = stringResource(R.string.finger_hold_duration),
                description = stringResource(R.string.finger_hold_duration)
            ) {
                val currentSec = (holdDurationMs / 1000).toInt().coerceIn(1, 5)
                var sliderVal by remember(currentSec) { mutableFloatStateOf(currentSec.toFloat()) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = currentSec == 1,
                        onClick = {
                            sliderVal = 1f
                            onHoldDurationChange(1000L)
                        },
                        label = { Text(stringResource(R.string.seconds_short_value, 1)) }
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = stringResource(R.string.seconds_short_value, sliderVal.roundToInt()),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    FilterChip(
                        selected = currentSec == 5,
                        onClick = {
                            sliderVal = 5f
                            onHoldDurationChange(5000L)
                        },
                        label = { Text(stringResource(R.string.seconds_short_value, 5)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = sliderVal,
                    onValueChange = {
                        sliderVal = it
                        val sec = it.roundToInt().coerceIn(1, 5)
                        onHoldDurationChange(sec * 1000L)
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.tertiary,
                        activeTrackColor = MaterialTheme.colorScheme.tertiary
                    )
                )
            }

            ConfigDivider()

            // Hold Result on Screen Section (Переключатель с полной кликабельностью строки)
            FingerSectionLayout(
                icon = painterResource(id = R.drawable.keep_filled_24px),
                title = stringResource(R.string.finger_hold_result),
                description = stringResource(R.string.finger_hold_result_desc),
                onRowClick = { onHoldResultEnabledChange(!holdResultEnabled) },
                action = {
                    Switch(
                        checked = holdResultEnabled,
                        onCheckedChange = onHoldResultEnabledChange
                    )
                }
            ) {
                AnimatedVisibility(
                    visible = holdResultEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        var sliderVal by remember(resultHoldDurationSeconds) {
                            mutableFloatStateOf(resultHoldDurationSeconds.toFloat())
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = resultHoldDurationSeconds == 1,
                                onClick = {
                                    sliderVal = 1f
                                    onResultHoldDurationSecondsChange(1)
                                },
                                label = { Text(stringResource(R.string.seconds_short_value, 1)) }
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text(
                                    text = stringResource(R.string.seconds_short_value, sliderVal.roundToInt()),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }

                            FilterChip(
                                selected = resultHoldDurationSeconds == 10,
                                onClick = {
                                    sliderVal = 10f
                                    onResultHoldDurationSecondsChange(10)
                                },
                                label = { Text(stringResource(R.string.seconds_short_value, 10)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = sliderVal,
                            onValueChange = {
                                sliderVal = it
                                onResultHoldDurationSecondsChange(it.roundToInt().coerceIn(1, 10))
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.tertiary,
                                activeTrackColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FingerSectionLayout(
    icon: Painter,
    title: String,
    description: String? = null,
    onRowClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onRowClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onRowClick
                        )
                    } else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                description?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            action?.let {
                Spacer(modifier = Modifier.width(8.dp))
                it()
            }
        }

        content?.let {
            Spacer(modifier = Modifier.height(16.dp))
            it()
        }
    }
}
