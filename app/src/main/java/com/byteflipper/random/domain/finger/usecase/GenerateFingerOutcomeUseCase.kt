package com.byteflipper.random.domain.finger.usecase

import com.byteflipper.random.ui.finger.FingerMode
import kotlin.random.Random

data class FingerParticipantOutcome(
    val id: Long,
    val isWinner: Boolean = false,
    val teamIndex: Int? = null,
    val orderIndex: Int? = null
)

class GenerateFingerOutcomeUseCase {
    data class Params(
        val participantIds: List<Long>,
        val mode: FingerMode,
        val winnerCount: Int = 1,
        val teamCount: Int = 2,
        val random: Random = Random.Default
    )

    operator fun invoke(params: Params): List<FingerParticipantOutcome> {
        val ids = params.participantIds
        if (ids.isEmpty()) return emptyList()

        val shuffled = ids.shuffled(params.random)

        return when (params.mode) {
            FingerMode.WINNER -> {
                val winnersCount = params.winnerCount.coerceIn(1, ids.size)
                val winnerIds = shuffled.take(winnersCount).toSet()
                ids.map { id ->
                    FingerParticipantOutcome(
                        id = id,
                        isWinner = id in winnerIds
                    )
                }
            }
            FingerMode.TEAMS -> {
                val teamCount = params.teamCount.coerceIn(2, ids.size).coerceAtLeast(2)
                val teamAssignments = shuffled.mapIndexed { index, id ->
                    id to ((index % teamCount) + 1)
                }.toMap()

                ids.map { id ->
                    FingerParticipantOutcome(
                        id = id,
                        isWinner = true,
                        teamIndex = teamAssignments[id] ?: 1
                    )
                }
            }
            FingerMode.ORDER -> {
                val orderAssignments = shuffled.mapIndexed { index, id ->
                    id to (index + 1)
                }.toMap()

                ids.map { id ->
                    FingerParticipantOutcome(
                        id = id,
                        isWinner = true,
                        orderIndex = orderAssignments[id] ?: 1
                    )
                }
            }
        }
    }
}
