package com.byteflipper.random.ui.lot.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.byteflipper.random.ui.lot.LotCard

@Composable
fun LotOverlay(
    cards: List<LotCard>,
    scrimAlpha: Float,
    onDismiss: () -> Unit,
    onCardClick: (Int) -> Unit
) {
    BackHandler(enabled = true) { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )

        // Grid
        LotGrid(cards = cards, onCardClick = onCardClick)
    }
}


