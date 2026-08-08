package com.byteflipper.random.data.team

import androidx.room.Embedded

data class TeamPresetWithCount(
    @Embedded val preset: TeamPreset,
    val aliveMemberCount: Int
)
