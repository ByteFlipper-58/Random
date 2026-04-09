package com.byteflipper.random.ui.lists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
internal class ListScreenController(
) {
    var pendingOpenPresetId by mutableStateOf<Long?>(null)
    var saveDialogUsesResults by mutableStateOf(false)
}

@Composable
internal fun rememberListScreenController(): ListScreenController {
    return remember { ListScreenController() }
}
