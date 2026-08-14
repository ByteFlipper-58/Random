package com.byteflipper.random.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background

@Composable
internal fun FlipGenerateScreenHost(
    innerPadding: PaddingValues,
    controller: FlipGenerateController,
    content: @Composable (Modifier, PaddingValues) -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
    dialogs: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val contentModifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .blur((8f * controller.flipController.scrimProgress.value).dp)
            .consumeWindowInsets(innerPadding)

        content(contentModifier, innerPadding)
        overlay()
        dialogs()
    }
}
