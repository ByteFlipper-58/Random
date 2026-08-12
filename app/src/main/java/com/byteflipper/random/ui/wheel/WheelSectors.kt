package com.byteflipper.random.ui.wheel

import com.byteflipper.random.data.settings.WheelUsedSectorStyle

/**
 * A wheel sector. [index] is the index of the item in the list and is used both for exclusion and
 * for the palette, so a sector keeps its color from spin to spin.
 */
data class WheelSector(
    val index: Int,
    val label: String,
    val isExcluded: Boolean
)

/**
 * The sector layout: the only place that decides what is present on the wheel.
 *
 * [WheelUsedSectorStyle] affects drawing, the resting angle and the tick count alike, so all of
 * them have to ask for the layout here. Otherwise the wheel stops somewhere other than where it
 * is drawn.
 */
fun wheelSectors(
    items: List<String>,
    excludedIndices: Set<Int>,
    usedSectorStyle: WheelUsedSectorStyle
): List<WheelSector> = items.mapIndexedNotNull { index, label ->
    val isExcluded = index in excludedIndices

    when {
        !isExcluded -> WheelSector(index = index, label = label, isExcluded = false)
        usedSectorStyle == WheelUsedSectorStyle.Dim ->
            WheelSector(index = index, label = label, isExcluded = true)
        // Remove: a used sector is simply not on the wheel.
        else -> null
    }
}
