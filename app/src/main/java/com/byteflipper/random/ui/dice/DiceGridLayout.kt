package com.byteflipper.random.ui.dice

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Computes a balanced distribution of dice per row with a maximum of 3 per row.
 * Rows are filled from center to edges to keep the grid visually balanced.
 */
fun computeDiceRowSizes(total: Int): List<Int> {
    if (total <= 0) return emptyList()
    val maxPerRow = 3
    if (total <= 3) return listOf(total)
    if (total == 4) return listOf(2, 2)
    if (total == 5) return listOf(3, 2)

    val minRows = (total + maxPerRow - 1) / maxPerRow
    val desiredRows = sqrt(total.toDouble()).roundToInt().coerceAtLeast(2)
    var rowsCount = maxOf(minRows, desiredRows)

    fun distributeCenter(rowsCount: Int): List<Int> {
        val base = total / rowsCount
        var extra = total % rowsCount
        val rows = MutableList(rowsCount) { base }
        // order of distributing leftover items — from center to edges
        val order = buildList {
            if (rowsCount % 2 == 1) {
                val mid = rowsCount / 2
                add(mid)
                for (d in 1..mid) {
                    add(mid - d)
                    add(mid + d)
                }
            } else {
                val leftMid = rowsCount / 2 - 1
                val rightMid = rowsCount / 2
                add(leftMid)
                add(rightMid)
                for (d in 1..leftMid) {
                    add(leftMid - d)
                    add(rightMid + d)
                }
            }
        }
        var guard = 0
        while (extra > 0 && guard < order.size * 2) {
            for (idx in order) {
                if (extra == 0) break
                if (rows[idx] < maxPerRow) {
                    rows[idx] += 1
                    extra -= 1
                }
            }
            guard += 1
        }
        return rows
    }

    // Increase row count until all rows are within maxPerRow limit
    while (true) {
        val rows = distributeCenter(rowsCount)
        if (rows.all { it <= maxPerRow }) return rows
        rowsCount += 1
    }
}


