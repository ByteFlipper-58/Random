package com.byteflipper.random.ui.numbers

import androidx.compose.ui.graphics.Color

fun computeCardBaseSizeDp(count: Int): Int {
    val base = 280
    val scale = when {
        count <= 10 -> 1.0
        count <= 25 -> 1.15
        count <= 50 -> 1.3
        else -> 1.5
    }
    return (base * scale).toInt()
}

fun computeHeightScale(count: Int): Float = when {
    count <= 10 -> 1.0f
    count <= 25 -> 1.2f
    count <= 50 -> 1.4f
    count <= 100 -> 1.6f
    else -> 1.8f
}

fun pickStableColor(seed: Long?, palette: List<Color>): Color {
    if (palette.isEmpty()) return Color.Unspecified
    val random = seed?.let { kotlin.random.Random(it) } ?: kotlin.random.Random
    return palette[random.nextInt(palette.size)]
}


