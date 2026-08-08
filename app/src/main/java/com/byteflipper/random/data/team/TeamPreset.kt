package com.byteflipper.random.data.team

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_presets")
data class TeamPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val splitMode: TeamSplitMode = TeamSplitMode.TeamCount,
    val teamCount: Int? = 2,
    val groupSize: Int? = null,
    val equalTeamSizesOnly: Boolean = false,
    val balanceByGender: Boolean = false,
    val balanceByAge: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
    val isPinned: Boolean = false
)
