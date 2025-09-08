package com.byteflipper.random.domain.lot.usecase

import kotlin.random.Random

class GenerateMarkedIndicesUseCase {
    /**
     * Возвращает множество из [marked] уникальных индексов в диапазоне [0, total).
     */
    operator fun invoke(total: Int, marked: Int): Set<Int> {
        if (total <= 0 || marked <= 0) return emptySet()
        if (marked >= total) return (0 until total).toSet()
        val indices = (0 until total).toMutableList()
        indices.shuffle(Random)
        return indices.take(marked).toSet()
    }
}


