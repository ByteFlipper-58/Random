package com.byteflipper.random.domain.dice.usecase

import kotlin.random.Random

class RollDiceUseCase {
    /**
     * Бросок n шестигранных кубиков. Возвращает список значений [1..6].
     */
    operator fun invoke(count: Int): List<Int> {
        if (count <= 0) return emptyList()
        return List(count) { Random.nextInt(1, 7) }
    }
}


