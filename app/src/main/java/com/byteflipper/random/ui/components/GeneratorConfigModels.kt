package com.byteflipper.random.ui.components

import com.byteflipper.random.ui.settings.components.RadioOption

data class GeneratorConfigContract(
    val visible: Boolean,
    val onDismissRequest: () -> Unit,
    val countConfig: CountConfig,
    val repetitionConfig: RepetitionConfig,
    val delayConfig: DelayConfig,
    val sortingConfig: SortingConfig? = null
)

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
