package com.byteflipper.random.domain.team

import com.byteflipper.random.data.person.Person
import com.byteflipper.random.data.team.TeamSplitMode

data class TeamGenerationResult(
    val splitMode: TeamSplitMode,
    val teams: List<GeneratedTeam>,
    val leftOutMembers: List<Person> = emptyList()
)
