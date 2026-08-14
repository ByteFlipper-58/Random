package com.byteflipper.random.domain.finger.usecase

import com.byteflipper.random.ui.finger.FingerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GenerateFingerOutcomeUseCaseTest {

    private val useCase = GenerateFingerOutcomeUseCase()

    @Test
    fun `empty input returns empty outcome list`() {
        val result = useCase(
            GenerateFingerOutcomeUseCase.Params(
                participantIds = emptyList(),
                mode = FingerMode.WINNER
            )
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `winner mode selects exact requested number of winners`() {
        val ids = listOf(1L, 2L, 3L, 4L, 5L)
        val winnerCount = 2

        val result = useCase(
            GenerateFingerOutcomeUseCase.Params(
                participantIds = ids,
                mode = FingerMode.WINNER,
                winnerCount = winnerCount,
                random = Random(42)
            )
        )

        assertEquals(ids.size, result.size)
        val winners = result.filter { it.isWinner }
        assertEquals(winnerCount, winners.size)

        val nonWinners = result.filter { !it.isWinner }
        assertEquals(ids.size - winnerCount, nonWinners.size)

        assertEquals(ids.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun `winner mode clamps requested winners to participant count`() {
        val ids = listOf(10L, 20L)
        val result = useCase(
            GenerateFingerOutcomeUseCase.Params(
                participantIds = ids,
                mode = FingerMode.WINNER,
                winnerCount = 5,
                random = Random(123)
            )
        )

        assertEquals(2, result.size)
        assertTrue(result.all { it.isWinner })
    }

    @Test
    fun `teams mode divides participants across specified team count`() {
        val ids = (1L..6L).toList()
        val teamCount = 3

        val result = useCase(
            GenerateFingerOutcomeUseCase.Params(
                participantIds = ids,
                mode = FingerMode.TEAMS,
                teamCount = teamCount,
                random = Random(99)
            )
        )

        assertEquals(6, result.size)
        assertTrue(result.all { it.isWinner }) // in teams mode, all active fingers are assigned
        assertTrue(result.all { it.teamIndex != null && it.teamIndex in 1..teamCount })

        val teamCounts = result.groupBy { it.teamIndex }.mapValues { it.value.size }
        assertEquals(3, teamCounts.size)
        assertEquals(listOf(2, 2, 2), teamCounts.values.toList())
    }

    @Test
    fun `teams mode distributes uneven participant counts properly`() {
        val ids = (1L..5L).toList()
        val teamCount = 2

        val result = useCase(
            GenerateFingerOutcomeUseCase.Params(
                participantIds = ids,
                mode = FingerMode.TEAMS,
                teamCount = teamCount,
                random = Random(7)
            )
        )

        assertEquals(5, result.size)
        val teamCounts = result.groupBy { it.teamIndex }.mapValues { it.value.size }
        assertEquals(setOf(2, 3), teamCounts.values.toSet())
    }

    @Test
    fun `order mode assigns unique sequence numbers 1 to N`() {
        val ids = listOf(101L, 102L, 103L, 104L)

        val result = useCase(
            GenerateFingerOutcomeUseCase.Params(
                participantIds = ids,
                mode = FingerMode.ORDER,
                random = Random(55)
            )
        )

        assertEquals(4, result.size)
        assertTrue(result.all { it.isWinner })
        val orderIndices = result.mapNotNull { it.orderIndex }
        assertEquals(listOf(1, 2, 3, 4), orderIndices.sorted())
    }
}
