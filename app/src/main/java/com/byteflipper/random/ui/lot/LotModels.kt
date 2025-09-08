package com.byteflipper.random.ui.lot

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class LotCard(
    val id: Int,
    val isMarked: Boolean,
    val isRevealed: Boolean,
    val color: Color
)


