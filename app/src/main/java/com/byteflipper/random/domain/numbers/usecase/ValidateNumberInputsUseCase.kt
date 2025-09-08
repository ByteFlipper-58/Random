package com.byteflipper.random.domain.numbers.usecase

class ValidateNumberInputsUseCase {
    data class Params(
        val fromText: String,
        val toText: String,
        val countText: String,
        val allowRepetitions: Boolean,
        val usedNumbers: Set<Int>
    )

    /**
     * Валидирует входные параметры и возвращает диапазон и количество, если всё корректно.
     * Возвращает null при невалидных данных или невозможной генерации без повторов.
     */
    operator fun invoke(params: Params): Pair<IntRange, Int>? {
        val from = params.fromText.trim().toIntOrNull()
        val to = params.toText.trim().toIntOrNull()
        val count = params.countText.trim().toIntOrNull() ?: 1

        if (from == null || to == null) return null
        if (count < 1) return null

        val range = if (from <= to) from..to else to..from
        if (!params.allowRepetitions) {
            val available = range.count { it !in params.usedNumbers }
            if (available < count) return null
        }
        return range to count
    }
}


