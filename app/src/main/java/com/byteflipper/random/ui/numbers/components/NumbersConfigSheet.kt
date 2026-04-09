package com.byteflipper.random.ui.numbers.components

import androidx.compose.runtime.Composable
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.numbers.NumbersUiEvent
import com.byteflipper.random.ui.numbers.NumbersUiState

@Composable
fun NumbersConfigSheet(
    uiState: NumbersUiState,
    onEvent: (NumbersUiEvent) -> Unit
) {
    GeneratorConfigDialog(contract = uiState.toGeneratorConfigContract(onEvent))
}
