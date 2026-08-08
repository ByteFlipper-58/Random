package com.byteflipper.random.domain.team.usecase

import com.byteflipper.random.data.person.Person
import com.byteflipper.random.data.team.TeamSplitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateTeamsUseCaseTest {
    private val useCase = GenerateTeamsUseCase()

    @Test
    fun `team count split distributes members evenly`() {
        val result = useCase(
            GenerateTeamsUseCase.Params(
                participants = buildPeople(7),
                splitMode = TeamSplitMode.TeamCount,
                teamCount = 3
            )
        )

        assertEquals(3, result.teams.size)
        val sizes = result.teams.map { it.members.size }.sorted()
        assertEquals(listOf(2, 2, 3), sizes)
        assertEquals(7, result.teams.sumOf { it.members.size })
    }

    @Test
    fun `group size split creates expected number of groups`() {
        val result = useCase(
            GenerateTeamsUseCase.Params(
                participants = buildPeople(10),
                splitMode = TeamSplitMode.GroupSize,
                groupSize = 4
            )
        )

        assertEquals(3, result.teams.size)
        val sizes = result.teams.map { it.members.size }.sorted()
        assertEquals(listOf(3, 3, 4), sizes)
    }

    @Test
    fun `result keeps all original participants`() {
        val participants = buildPeople(8)
        val result = useCase(
            GenerateTeamsUseCase.Params(
                participants = participants,
                splitMode = TeamSplitMode.TeamCount,
                teamCount = 4
            )
        )

        val resultIds = result.teams.flatMap { team -> team.members }.map { it.id }.toSet()
        assertEquals(participants.map { it.id }.toSet(), resultIds)
        assertTrue(result.teams.all { it.members.isNotEmpty() })
    }

    @Test
    fun `equal team sizes keeps remainder outside teams`() {
        val result = useCase(
            GenerateTeamsUseCase.Params(
                participants = buildPeople(10),
                splitMode = TeamSplitMode.TeamCount,
                teamCount = 3,
                equalTeamSizesOnly = true
            )
        )

        assertEquals(3, result.teams.size)
        assertEquals(listOf(3, 3, 3), result.teams.map { it.members.size }.sorted())
        assertEquals(1, result.leftOutMembers.size)
        assertEquals(10, result.teams.sumOf { it.members.size } + result.leftOutMembers.size)
    }

    private fun buildPeople(count: Int): List<Person> {
        return List(count) { index ->
            Person(
                id = index.toLong() + 1,
                displayName = "Person ${index + 1}"
            )
        }
    }
}
