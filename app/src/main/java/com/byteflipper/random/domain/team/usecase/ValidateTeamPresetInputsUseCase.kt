package com.byteflipper.random.domain.team.usecase

import com.byteflipper.random.R
import com.byteflipper.random.data.team.TeamSplitMode

class ValidateTeamPresetInputsUseCase {
    companion object {
        const val MAX_TEAM_COUNT = 8
    }

    data class Params(
        val participantCount: Int,
        val splitMode: TeamSplitMode,
        val teamCountText: String,
        val groupSizeText: String
    )

    sealed interface Result {
        data class Success(
            val splitMode: TeamSplitMode,
            val teamCount: Int?,
            val groupSize: Int?
        ) : Result

        data class Error(val messageRes: Int) : Result
    }

    operator fun invoke(params: Params): Result {
        if (params.participantCount < 2) {
            return Result.Error(R.string.team_requires_two_people)
        }

        return when (params.splitMode) {
            TeamSplitMode.TeamCount -> {
                val teamCount = params.teamCountText.toIntOrNull()
                    ?: return Result.Error(R.string.team_invalid_team_count)
                if (teamCount < 2 || teamCount > params.participantCount || teamCount > MAX_TEAM_COUNT) {
                    return Result.Error(R.string.team_invalid_team_count)
                }
                Result.Success(
                    splitMode = params.splitMode,
                    teamCount = teamCount,
                    groupSize = null
                )
            }

            TeamSplitMode.GroupSize -> {
                val groupSize = params.groupSizeText.toIntOrNull()
                    ?: return Result.Error(R.string.team_invalid_group_size)
                if (groupSize < 2 || groupSize > params.participantCount) {
                    return Result.Error(R.string.team_invalid_group_size)
                }
                Result.Success(
                    splitMode = params.splitMode,
                    teamCount = null,
                    groupSize = groupSize
                )
            }
        }
    }
}
