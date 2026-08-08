package com.byteflipper.random.domain.team.usecase

import com.byteflipper.random.R
import com.byteflipper.random.data.team.TeamSplitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTeamPresetInputsUseCaseTest {
    private val useCase = ValidateTeamPresetInputsUseCase()

    @Test
    fun `team count validation succeeds for valid input`() {
        val result = useCase(
            ValidateTeamPresetInputsUseCase.Params(
                participantCount = 6,
                splitMode = TeamSplitMode.TeamCount,
                teamCountText = "3",
                groupSizeText = ""
            )
        )

        assertTrue(result is ValidateTeamPresetInputsUseCase.Result.Success)
        val success = result as ValidateTeamPresetInputsUseCase.Result.Success
        assertEquals(3, success.teamCount)
        assertEquals(null, success.groupSize)
    }

    @Test
    fun `group size validation fails when bigger than participant count`() {
        val result = useCase(
            ValidateTeamPresetInputsUseCase.Params(
                participantCount = 4,
                splitMode = TeamSplitMode.GroupSize,
                teamCountText = "",
                groupSizeText = "5"
            )
        )

        assertEquals(
            ValidateTeamPresetInputsUseCase.Result.Error(R.string.team_invalid_group_size),
            result
        )
    }

    @Test
    fun `team count validation fails when bigger than max allowed`() {
        val result = useCase(
            ValidateTeamPresetInputsUseCase.Params(
                participantCount = 12,
                splitMode = TeamSplitMode.TeamCount,
                teamCountText = "9",
                groupSizeText = ""
            )
        )

        assertEquals(
            ValidateTeamPresetInputsUseCase.Result.Error(R.string.team_invalid_team_count),
            result
        )
    }

    @Test
    fun `validation fails when not enough participants`() {
        val result = useCase(
            ValidateTeamPresetInputsUseCase.Params(
                participantCount = 1,
                splitMode = TeamSplitMode.TeamCount,
                teamCountText = "2",
                groupSizeText = ""
            )
        )

        assertEquals(
            ValidateTeamPresetInputsUseCase.Result.Error(R.string.team_requires_two_people),
            result
        )
    }
}
