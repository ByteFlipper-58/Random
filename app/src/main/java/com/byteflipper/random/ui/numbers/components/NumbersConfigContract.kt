package com.byteflipper.random.ui.numbers.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.domain.numbers.SortingMode
import com.byteflipper.random.ui.components.CountConfig
import com.byteflipper.random.ui.components.DelayConfig
import com.byteflipper.random.ui.components.GeneratorConfigContract
import com.byteflipper.random.ui.components.RepetitionConfig
import com.byteflipper.random.ui.components.SortingConfig
import com.byteflipper.random.ui.numbers.NumbersUiEvent
import com.byteflipper.random.ui.numbers.NumbersUiState
import com.byteflipper.random.ui.settings.components.RadioOption
import com.byteflipper.random.utils.Constants.DEFAULT_DELAY_MS
import com.byteflipper.random.utils.Constants.MAX_DELAY_MS
import com.byteflipper.random.utils.Constants.MIN_DELAY_MS

@Composable
internal fun NumbersUiState.toGeneratorConfigContract(
    onEvent: (NumbersUiEvent) -> Unit
): GeneratorConfigContract {
    return GeneratorConfigContract(
        visible = showConfigDialog,
        onDismissRequest = { onEvent(NumbersUiEvent.SetConfigDialogVisible(false)) },
        countConfig = CountConfig(
            text = countText,
            onChange = { onEvent(NumbersUiEvent.UpdateCountText(it)) }
        ),
        repetitionConfig = RepetitionConfig(
            allowRepetitions = allowRepetitions,
            onAllowRepetitionsChange = { onEvent(NumbersUiEvent.UpdateAllowRepetitions(it)) },
            usedNumbers = usedNumbers,
            availableRange = run {
                val from = fromText.trim().toIntOrNull()
                val to = toText.trim().toIntOrNull()
                if (from != null && to != null) {
                    if (from <= to) from..to else to..from
                } else {
                    null
                }
            },
            onResetUsedNumbers = { onEvent(NumbersUiEvent.ResetUsedNumbers) }
        ),
        delayConfig = DelayConfig(
            useDelay = useDelay,
            onUseDelayChange = { onEvent(NumbersUiEvent.UpdateUseDelay(it)) },
            delayText = delayText,
            onDelayChange = { onEvent(NumbersUiEvent.UpdateDelayText(it)) },
            minDelayMs = MIN_DELAY_MS,
            maxDelayMs = MAX_DELAY_MS,
            defaultDelayMs = DEFAULT_DELAY_MS
        ),
        sortingConfig = SortingConfig(
            options = listOf(
                RadioOption(key = SortingMode.Random.name, title = stringResource(R.string.random_order)),
                RadioOption(key = SortingMode.Ascending.name, title = stringResource(R.string.ascending)),
                RadioOption(key = SortingMode.Descending.name, title = stringResource(R.string.descending))
            ),
            selectedKey = sortingMode.name,
            onChange = { key ->
                onEvent(NumbersUiEvent.UpdateSortingMode(SortingMode.valueOf(key)))
            }
        )
    )
}
