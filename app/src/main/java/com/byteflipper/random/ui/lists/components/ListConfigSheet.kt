package com.byteflipper.random.ui.lists.components

import androidx.compose.runtime.Composable
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.lists.ListUiEvent
import com.byteflipper.random.ui.lists.ListUiState

@Composable
fun ListConfigSheet(
    uiState: ListUiState,
    onEvent: (ListUiEvent) -> Unit
) {
    GeneratorConfigDialog(contract = uiState.toGeneratorConfigContract(onEvent))
}
