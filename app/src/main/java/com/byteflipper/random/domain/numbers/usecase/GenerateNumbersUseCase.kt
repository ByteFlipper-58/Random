package com.byteflipper.random.domain.numbers.usecase

import com.byteflipper.random.domain.numbers.SortingMode

class GenerateNumbersUseCase {
    data class Params(
        val range: IntRange,
        val count: Int,
        val allowRepetitions: Boolean,
        val usedNumbers: Set<Int>,
        val sortingMode: SortingMode
    )

    data class Result(
        val values: List<Int>,
        val updatedUsedNumbers: Set<Int>
    )

    operator fun invoke(params: Params): Result {
        val values = if (params.allowRepetitions) {
            List(params.count) { params.range.random() }
        } else {
            val available = params.range.filter { it !in params.usedNumbers }
            available.shuffled().take(params.count)
        }

        val sorted = when (params.sortingMode) {
            SortingMode.Random -> values.shuffled()
            SortingMode.Ascending -> values.sorted()
            SortingMode.Descending -> values.sortedDescending()
        }

        val newUsed = if (params.allowRepetitions) params.usedNumbers else params.usedNumbers + sorted
        return Result(values = sorted, updatedUsedNumbers = newUsed)
    }
}


