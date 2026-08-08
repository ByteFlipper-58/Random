package com.byteflipper.random.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.byteflipper.random.ui.settings.components.RadioOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorConfigDialog(
    contract: GeneratorConfigContract
) {
    GeneratorConfigDialog(
        visible = contract.visible,
        onDismissRequest = contract.onDismissRequest,
        countConfig = contract.countConfig,
        repetitionConfig = contract.repetitionConfig,
        delayConfig = contract.delayConfig,
        sortingConfig = contract.sortingConfig
    )
}

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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

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
                androidx.compose.foundation.layout.Box(
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

            ResultCountSection(countConfig = countConfig)
            sortingConfig?.let {
                ConfigDivider()
                SortingSection(sortingConfig = it)
            }
            ConfigDivider()
            RepetitionSection(repetitionConfig = repetitionConfig)
            ConfigDivider()
            DelaySection(delayConfig = delayConfig)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorConfigDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    countText: String,
    onCountChange: (String) -> Unit,
    allowRepetitions: Boolean,
    onAllowRepetitionsChange: (Boolean) -> Unit,
    usedNumbers: Set<Int>,
    availableRange: IntRange?,
    onResetUsedNumbers: () -> Unit,
    useDelay: Boolean,
    onUseDelayChange: (Boolean) -> Unit,
    delayText: String,
    onDelayChange: (String) -> Unit,
    minDelayMs: Int = 1_000,
    maxDelayMs: Int = 60_000,
    defaultDelayMs: Int = 3_000,
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
        sortingConfig = if (
            sortingOptions != null &&
            selectedSortingKey != null &&
            onSortingChange != null
        ) {
            SortingConfig(
                options = sortingOptions,
                selectedKey = selectedSortingKey,
                onChange = onSortingChange
            )
        } else {
            null
        }
    )
}
