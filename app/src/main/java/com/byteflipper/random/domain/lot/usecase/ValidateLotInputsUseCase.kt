package com.byteflipper.random.domain.lot.usecase

class ValidateLotInputsUseCase {
    data class Params(
        val totalText: String,
        val markedText: String
    )

    /**
     * Возвращает (total, marked) при валидных данных или null.
     * Требования: total >= 3, marked in [1..total].
     */
    operator fun invoke(params: Params): Pair<Int, Int>? {
        val total = params.totalText.trim().toIntOrNull() ?: return null
        val marked = params.markedText.trim().toIntOrNull() ?: return null
        if (total < 3) return null
        if (marked < 1) return null
        if (marked > total) return null
        return total to marked
    }
}


