package com.byteflipper.random.ui.lot.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Returns a contrasting text color for the given background color.
 */
fun getContrastColor(backgroundColor: Color): Color {
    val luminance = backgroundColor.luminance()
    return if (luminance > 0.5f) Color.Black else Color.White
}

/**
 * Distributes colors across a grid avoiding repeats within the same and adjacent rows.
 */
fun distributeColorsSmartly(
    totalCards: Int,
    availableColors: List<Color>,
    rows: List<Int>
): List<Color> {
    if (availableColors.isEmpty()) return emptyList()

    val colors = mutableListOf<Color>()
    val usedColorsInCurrentRow = mutableSetOf<Color>()
    val usedColorsInPreviousRow = mutableSetOf<Color>()

    var cardIndex = 0

    for (row in rows) {
        usedColorsInCurrentRow.clear()

        for (i in 0 until row) {
            if (cardIndex >= totalCards) break

            val forbiddenColors = usedColorsInCurrentRow + usedColorsInPreviousRow
            val availableForThisCard = availableColors.filter { it !in forbiddenColors }

            val selectedColor = if (availableForThisCard.isNotEmpty()) {
                availableForThisCard.random()
            } else {
                availableColors.random()
            }

            colors.add(selectedColor)
            usedColorsInCurrentRow.add(selectedColor)
            cardIndex++
        }

        usedColorsInPreviousRow.clear()
        usedColorsInPreviousRow.addAll(usedColorsInCurrentRow)
    }

    return colors
}


