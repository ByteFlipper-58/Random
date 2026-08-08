package com.byteflipper.random.data.team

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "team_preset_members",
    primaryKeys = ["presetId", "personId"],
    foreignKeys = [
        ForeignKey(
            entity = TeamPreset::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["presetId"]),
        Index(value = ["personId"])
    ]
)
data class TeamPresetMember(
    val presetId: Long,
    val personId: Long,
    val position: Int
)
