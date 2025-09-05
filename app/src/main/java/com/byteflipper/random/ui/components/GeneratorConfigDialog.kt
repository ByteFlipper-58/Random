package com.byteflipper.random.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import kotlin.math.roundToInt

// Data классы для группировки параметров
data class CountConfig(
    val text: String,
    val onChange: (String) -> Unit
)

data class RepetitionConfig(
    val allowRepetitions: Boolean,
    val onAllowRepetitionsChange: (Boolean) -> Unit,
    val usedNumbers: Set<Int>,
    val availableRange: IntRange?,
    val onResetUsedNumbers: () -> Unit
)

data class DelayConfig(
    val useDelay: Boolean,
    val onUseDelayChange: (Boolean) -> Unit,
    val delayText: String,
    val onDelayChange: (String) -> Unit,
    val minDelayMs: Int = 1_000,
    val maxDelayMs: Int = 60_000,
    val defaultDelayMs: Int = 3_000
)

data class SortingConfig(
    val options: List<RadioOption>,
    val selectedKey: String,
    val onChange: (String) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorConfigDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    countConfig: CountConfig,
    repetitionConfig: RepetitionConfig,
    delayConfig: DelayConfig,
    sortingConfig: SortingConfig? = null
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
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
            // Header с улучшенным дизайном
            ConfigHeader(onDismissRequest = onDismissRequest)

            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Секция количества результатов
                ResultCountSection(countConfig = countConfig)

                // Секция сортировки (если есть)
                sortingConfig?.let {
                    SortingSection(sortingConfig = it)
                }

                // Секция повторений
                RepetitionSection(repetitionConfig = repetitionConfig)

                // Секция задержки
                DelaySection(delayConfig = delayConfig)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConfigHeader(onDismissRequest: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.tune_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.generator_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.customize_generation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ResultCountSection(countConfig: CountConfig) {
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
                onValueChange = { v ->
                    val nv = v.roundToInt().coerceIn(1, 100)
                    countConfig.onChange(nv.toString())
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
private fun SortingSection(sortingConfig: SortingConfig) {
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
                    color = if (selected)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surface
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
                            color = if (selected)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepetitionSection(repetitionConfig: RepetitionConfig) {
    val hasUsedNumbers = !repetitionConfig.allowRepetitions &&
            repetitionConfig.usedNumbers.isNotEmpty() &&
            repetitionConfig.availableRange != null

    ConfigSection(
        icon = painterResource(id = R.drawable.repeat_24px),
        title = stringResource(R.string.allow_repetitions),
        description = if (hasUsedNumbers) {
            val totalCount = repetitionConfig.availableRange!!.count()
            val usedCount = repetitionConfig.usedNumbers.count {
                it in repetitionConfig.availableRange
            }
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
                        imageVector = Icons.Default.RestartAlt,
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
private fun DelaySection(delayConfig: DelayConfig) {
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
                        label = { Text("${minSec}s") }
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = "$currentSec ${stringResource(R.string.seconds_short)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    FilterChip(
                        selected = currentSec == maxSec,
                        onClick = { delayConfig.onDelayChange((maxSec * 1000).toString()) },
                        label = { Text("${maxSec}s") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = currentSec.toFloat(),
                    onValueChange = { v ->
                        val nv = v.roundToInt().coerceIn(minSec, maxSec)
                        delayConfig.onDelayChange((nv * 1000).toString())
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

@Composable
private fun ConfigSection(
    icon: Painter,
    title: String,
    description: String? = null,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                action?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    it()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// Альтернативная версия с оригинальной сигнатурой для обратной совместимости
@OptIn(ExperimentalMaterial3Api::class)
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
    defaultDelayMs: Int = 3_000,
    // Сортировка (опционально)
    sortingOptions: List<RadioOption>? = null,
    selectedSortingKey: String? = null,
    onSortingChange: ((String) -> Unit)? = null
) {
    GeneratorConfigDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        countConfig = CountConfig(
            text = countText,
            onChange = onCountChange
        ),
        repetitionConfig = RepetitionConfig(
            allowRepetitions = allowRepetitions,
            onAllowRepetitionsChange = onAllowRepetitionsChange,
            usedNumbers = usedNumbers,
            availableRange = availableRange,
            onResetUsedNumbers = onResetUsedNumbers
        ),
        delayConfig = DelayConfig(
            useDelay = useDelay,
            onUseDelayChange = onUseDelayChange,
            delayText = delayText,
            onDelayChange = onDelayChange,
            minDelayMs = minDelayMs,
            maxDelayMs = maxDelayMs,
            defaultDelayMs = defaultDelayMs
        ),
        sortingConfig = if (sortingOptions != null && selectedSortingKey != null && onSortingChange != null) {
            SortingConfig(
                options = sortingOptions,
                selectedKey = selectedSortingKey,
                onChange = onSortingChange
            )
        } else null
    )
}