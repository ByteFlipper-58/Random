package com.byteflipper.random.domain.lists.usecase

import com.byteflipper.random.domain.lists.ListSortingMode
import java.text.Normalizer
import java.util.Locale

class SortListResultsUseCase {
    data class Params(
        val input: List<String>,
        val mode: ListSortingMode
    )

    operator fun invoke(params: Params): List<String> {
        return when (params.mode) {
            ListSortingMode.Random -> params.input.shuffled()
            ListSortingMode.AlphabeticalAZ -> params.input.sortedWith(universalStringComparator())
            ListSortingMode.AlphabeticalZA -> params.input.sortedWith(universalStringComparator().reversed())
        }
    }

    private fun universalStringComparator(): Comparator<String> {
        return Comparator { a, b ->
            val ka = normalizeForSort(a)
            val kb = normalizeForSort(b)
            ka.compareTo(kb)
        }
    }

    private fun normalizeForSort(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        val lowerCased = trimmed.lowercase(Locale.ROOT)
        val decomposed = Normalizer.normalize(lowerCased, Normalizer.Form.NFKD)
        return decomposed.replace("\\p{M}+".toRegex(), "")
    }
}


