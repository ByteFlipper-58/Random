package com.byteflipper.random.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import kotlin.math.roundToInt

@Composable
internal fun ResultCountSection(countConfig: CountConfig) {
    val countVal = countConfig.text.toIntOrNull()?.coerceIn(1, 100) ?: 1

    ConfigSection(
        icon = painterResource(id = R.drawable.numbers_24px),
        title = stringResource(R.string.result_count),
        description = stringResource(R.string.result_count_description)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { countConfig.onChange("1") },
                    label = { Text("1") },
                    modifier = Modifier.alpha(if (countVal == 1) 1f else 0.6f)
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = countVal.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                AssistChip(
                    onClick = { countConfig.onChange("100") },
                    label = { Text("100") },
                    modifier = Modifier.alpha(if (countVal == 100) 1f else 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = countVal.toFloat(),
                onValueChange = { value ->
                    val nextValue = value.roundToInt().coerceIn(1, 100)
                    countConfig.onChange(nextValue.toString())
                },
                valueRange = 1f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
internal fun SortingSection(sortingConfig: SortingConfig) {
    ConfigSection(
        icon = painterResource(id = R.drawable.sort_24px),
        title = stringResource(R.string.sorting),
        description = stringResource(R.string.sorting_description)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sortingConfig.options.forEach { option ->
                val selected = option.key == sortingConfig.selectedKey

                Surface(
                    onClick = { sortingConfig.onChange(option.key) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun RepetitionSection(repetitionConfig: RepetitionConfig) {
    val availableRange = repetitionConfig.availableRange
    val hasUsedNumbers = !repetitionConfig.allowRepetitions &&
        repetitionConfig.usedNumbers.isNotEmpty() &&
        availableRange != null

    ConfigSection(
        icon = painterResource(id = R.drawable.repeat_24px),
        title = stringResource(R.string.allow_repetitions),
        description = if (hasUsedNumbers) {
            val totalCount = checkNotNull(availableRange).count()
            val usedCount = repetitionConfig.usedNumbers.count { it in availableRange }
            "${stringResource(R.string.used_count)}: $usedCount / $totalCount"
        } else {
            stringResource(R.string.repetitions_description)
        },
        action = {
            Switch(
                checked = repetitionConfig.allowRepetitions,
                onCheckedChange = repetitionConfig.onAllowRepetitionsChange
            )
        }
    ) {
        AnimatedVisibility(
            visible = hasUsedNumbers,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                TextButton(
                    onClick = repetitionConfig.onResetUsedNumbers,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.restart_alt_24px),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reset_history))
                }
            }
        }
    }
}

@Composable
internal fun DelaySection(delayConfig: DelayConfig) {
    ConfigSection(
        icon = painterResource(id = R.drawable.timer_24px),
        title = stringResource(R.string.use_delay),
        description = if (delayConfig.useDelay) {
            stringResource(R.string.custom_delay_enabled)
        } else {
            stringResource(R.string.fixed_delay_1_second)
        },
        action = {
            Switch(
                checked = delayConfig.useDelay,
                onCheckedChange = delayConfig.onUseDelayChange
            )
        }
    ) {
        AnimatedVisibility(
            visible = delayConfig.useDelay,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                val minSec = (delayConfig.minDelayMs / 1000).coerceAtLeast(1)
                val maxSec = (delayConfig.maxDelayMs / 1000).coerceAtLeast(minSec)
                val currentMs = delayConfig.delayText.toIntOrNull() ?: delayConfig.defaultDelayMs
                val currentSec = (currentMs / 1000).coerceIn(minSec, maxSec)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = currentSec == minSec,
                        onClick = { delayConfig.onDelayChange((minSec * 1000).toString()) },
                        label = { Text(stringResource(R.string.seconds_short_value, minSec)) }
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = stringResource(R.string.seconds_short_value, currentSec),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    FilterChip(
                        selected = currentSec == maxSec,
                        onClick = { delayConfig.onDelayChange((maxSec * 1000).toString()) },
                        label = { Text(stringResource(R.string.seconds_short_value, maxSec)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = currentSec.toFloat(),
                    onValueChange = { value ->
                        val nextValue = value.roundToInt().coerceIn(minSec, maxSec)
                        delayConfig.onDelayChange((nextValue * 1000).toString())
                    },
                    valueRange = minSec.toFloat()..maxSec.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.tertiary,
                        activeTrackColor = MaterialTheme.colorScheme.tertiary
                    )
                )
            }
        }
    }
}
