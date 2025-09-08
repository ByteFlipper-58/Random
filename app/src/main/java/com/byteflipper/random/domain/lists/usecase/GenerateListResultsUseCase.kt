package com.byteflipper.random.domain.lists.usecase

class GenerateListResultsUseCase {
    data class Params(
        val baseItems: List<String>,
        val count: Int,
        val allowRepetitions: Boolean,
        val usedItems: Set<String>
    )

    operator fun invoke(params: Params): List<String> {
        val items = params.baseItems.map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isEmpty()) return emptyList()
        if (params.allowRepetitions) {
            return List(params.count.coerceAtLeast(0)) { items.random() }
        }
        val pool = items.filter { it !in params.usedItems }.distinct()
        if (pool.isEmpty()) return emptyList()
        val k = params.count.coerceAtLeast(0).coerceAtMost(pool.size)
        return pool.shuffled().take(k)
    }
}


