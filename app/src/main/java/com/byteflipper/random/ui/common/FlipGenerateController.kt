package com.byteflipper.random.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.byteflipper.random.ui.components.flip.FlipCardController
import com.byteflipper.random.ui.components.flip.FlipCardControls
import com.byteflipper.random.ui.components.flip.FlipCardState
import com.byteflipper.random.ui.components.flip.rememberFlipCardState

internal class FlipGenerateController(
    val flipState: FlipCardState,
    val flipController: FlipCardController
) {
    var fabCenterInRoot by mutableStateOf(Offset.Zero)
    var isGenerating by mutableStateOf(false)
}

@Composable
internal fun rememberFlipGenerateController(): FlipGenerateController {
    val flipState = rememberFlipCardState()
    val flipController = FlipCardControls(flipState)
    return remember(flipState, flipController) {
        FlipGenerateController(
            flipState = flipState,
            flipController = flipController
        )
    }
}
