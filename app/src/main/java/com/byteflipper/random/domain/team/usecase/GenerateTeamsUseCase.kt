package com.byteflipper.random.domain.team.usecase

import com.byteflipper.random.data.person.Person
import com.byteflipper.random.data.team.TeamSplitMode
import com.byteflipper.random.domain.team.GeneratedTeam
import com.byteflipper.random.domain.team.TeamGenerationResult

class GenerateTeamsUseCase {
    data class Params(
        val participants: List<Person>,
        val splitMode: TeamSplitMode,
        val teamCount: Int? = null,
        val groupSize: Int? = null,
        val equalTeamSizesOnly: Boolean = false,
        val balanceByGender: Boolean = false,
        val balanceByAge: Boolean = false
    )

    operator fun invoke(params: Params): TeamGenerationResult {
        val sorted = params.participants.shuffled()

        val bucketCount = when (params.splitMode) {
            TeamSplitMode.TeamCount -> requireNotNull(params.teamCount)
            TeamSplitMode.GroupSize -> {
                val groupSize = requireNotNull(params.groupSize)
                kotlin.math.ceil(sorted.size / groupSize.toDouble()).toInt()
            }
        }.coerceAtLeast(1)

        val buckets = MutableList(bucketCount) { mutableListOf<Person>() }
        val distributedParticipants = when {
            params.splitMode == TeamSplitMode.TeamCount && params.equalTeamSizesOnly -> {
                val assignableCount = (sorted.size / bucketCount) * bucketCount
                sorted.take(assignableCount)
            }
            else -> sorted
        }
        if (params.balanceByGender || params.balanceByAge) {
            distributedParticipants.forEach { person ->
                val target = buckets.indices.minWithOrNull(
                    compareBy<Int> { index -> balanceScore(buckets[index], person, params) }
                        .thenBy { index -> buckets[index].size }
                ) ?: 0
                buckets[target].add(person)
            }
        } else {
            distributedParticipants.forEachIndexed { index, person ->
                buckets[index % bucketCount].add(person)
            }
        }
        val leftOutMembers = sorted.drop(distributedParticipants.size)

        return TeamGenerationResult(
            splitMode = params.splitMode,
            teams = buckets.mapIndexed { index, members ->
                GeneratedTeam(index = index, members = members.toList())
            },
            leftOutMembers = leftOutMembers
        )
    }

    private fun balanceScore(team: List<Person>, person: Person, params: Params): Int {
        var score = 0
        if (params.balanceByGender) {
            score += team.count { it.gender == person.gender } * 100
        }
        if (params.balanceByAge && person.birthYear != null) {
            val decade = person.birthYear / 10
            score += team.count { it.birthYear?.div(10) == decade } * 10
        }
        return score
    }
}
