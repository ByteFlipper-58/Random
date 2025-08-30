package com.byteflipper.random.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import kotlin.math.roundToInt

@Composable
fun GeneratorConfigDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    // Количество результатов
    countText: String,
    onCountChange: (String) -> Unit,
    // Повторения
    allowRepetitions: Boolean,
    onAllowRepetitionsChange: (Boolean) -> Unit,
    usedNumbers: Set<Int>,
    availableRange: IntRange?,
    onResetUsedNumbers: () -> Unit,
    // Задержка
    useDelay: Boolean,
    onUseDelayChange: (Boolean) -> Unit,
    delayText: String,
    onDelayChange: (String) -> Unit,
    minDelayMs: Int = 1_000,
    maxDelayMs: Int = 60_000,
    defaultDelayMs: Int = 3_000
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.settings_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.generator_settings),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Количество результатов
                val countVal = countText.toIntOrNull()?.coerceIn(1, 100) ?: 1

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.numbers_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.result_count),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = countVal.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = countVal.toFloat(),
                        onValueChange = { v ->
                            val nv = v.roundToInt().coerceIn(1, 100)
                            onCountChange(nv.toString())
                        },
                        valueRange = 1f..100f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // Повторения
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.repeat_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.allow_repetitions),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (!allowRepetitions && usedNumbers.isNotEmpty() && availableRange != null) {
                                val totalCount = availableRange.count()
                                val usedCount = usedNumbers.count { it in availableRange }
                                Text(
                                    text = "${stringResource(R.string.used_count)}: $usedCount из $totalCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = allowRepetitions,
                            onCheckedChange = onAllowRepetitionsChange
                        )
                    }

                    if (!allowRepetitions && usedNumbers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onResetUsedNumbers,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.restart_alt_24px),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.reset_history))
                        }
                    }
                }

                HorizontalDivider()

                // Задержка
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.timer_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.use_delay),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = useDelay,
                            onCheckedChange = onUseDelayChange
                        )
                    }

                    if (useDelay) {
                        val minSec = (minDelayMs / 1000).coerceAtLeast(1)
                        val maxSec = (maxDelayMs / 1000).coerceAtLeast(minSec)
                        val currentMs = delayText.toIntOrNull() ?: defaultDelayMs
                        val currentSec = (currentMs / 1000).coerceIn(minSec, maxSec)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.delay),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currentSec ${stringResource(R.string.seconds_short)}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = currentSec.toFloat(),
                            onValueChange = { v ->
                                val nv = v.roundToInt().coerceIn(minSec, maxSec)
                                onDelayChange((nv * 1000).toString())
                            },
                            valueRange = minSec.toFloat()..maxSec.toFloat()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${minSec}${stringResource(R.string.seconds_short)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${maxSec}${stringResource(R.string.seconds_short)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.fixed_delay_1_second),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}