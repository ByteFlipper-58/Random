package com.byteflipper.random.ui.lists.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.CountConfig
import com.byteflipper.random.ui.components.DelayConfig
import com.byteflipper.random.ui.components.GeneratorConfigContract
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.components.RepetitionConfig
import com.byteflipper.random.ui.components.SortingConfig
import com.byteflipper.random.ui.lists.ListUiEvent
import com.byteflipper.random.ui.lists.ListUiState
import com.byteflipper.random.ui.settings.components.RadioOption

private fun Set<String>.indicesPlaceholder(baseSize: Int): Set<Int> {
    return if (isEmpty()) emptySet() else (0 until minOf(size, baseSize)).toSet()
}

@Composable
internal fun ListUiState.toGeneratorConfigContract(
    onEvent: (ListUiEvent) -> Unit
): GeneratorConfigContract {
    return GeneratorConfigContract(
        visible = showConfigDialog,
        onDismissRequest = { onEvent(ListUiEvent.ToggleConfigDialog) },
        countConfig = CountConfig(
            text = countText,
            onChange = { onEvent(ListUiEvent.UpdateCountText(it)) }
        ),
        repetitionConfig = RepetitionConfig(
            allowRepetitions = allowRepetitions,
            onAllowRepetitionsChange = { onEvent(ListUiEvent.UpdateAllowRepetitions(it)) },
            usedNumbers = usedItems.indicesPlaceholder(baseSize = 1_000_000),
            availableRange = null,
            onResetUsedNumbers = { onEvent(ListUiEvent.ResetUsedItems) }
        ),
        delayConfig = DelayConfig(
            useDelay = useDelay,
            onUseDelayChange = { onEvent(ListUiEvent.UpdateUseDelay(it)) },
            delayText = delayText,
            onDelayChange = { onEvent(ListUiEvent.UpdateDelayText(it)) }
        ),
        sortingConfig = SortingConfig(
            options = listOf(
                RadioOption(
                    key = ListSortingMode.Random.name,
                    title = stringResource(R.string.random_order),
                    icon = painterResource(id = R.drawable.shuffle_24px)
                ),
                RadioOption(
                    key = ListSortingMode.AlphabeticalAZ.name,
                    title = stringResource(R.string.alphabetical_az),
                    icon = painterResource(id = R.drawable.sort_by_alpha_24px)
                ),
                RadioOption(
                    key = ListSortingMode.AlphabeticalZA.name,
                    title = stringResource(R.string.alphabetical_za),
                    icon = painterResource(id = R.drawable.sort_by_alpha_24px)
                )
            ),
            selectedKey = sortingMode.name,
            onChange = { key ->
                onEvent(ListUiEvent.UpdateSortingMode(ListSortingMode.valueOf(key)))
            }
        )
    )
}
