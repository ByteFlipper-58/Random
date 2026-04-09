package com.byteflipper.random.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

@Composable
internal fun FlipGenerateScreenHost(
    innerPadding: PaddingValues,
    controller: FlipGenerateController,
    content: @Composable (Modifier) -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
    dialogs: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .blur((8f * controller.flipController.scrimProgress.value).dp)

        content(contentModifier)
        overlay()
        dialogs()
    }
}
