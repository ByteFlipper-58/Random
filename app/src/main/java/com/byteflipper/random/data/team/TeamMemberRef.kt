package com.byteflipper.random.data.team

import androidx.room.ColumnInfo
import com.byteflipper.random.data.person.PersonGender

data class TeamMemberRef(
    @ColumnInfo(name = "personId") val personId: Long,
    @ColumnInfo(name = "displayName") val displayName: String,
    @ColumnInfo(name = "gender") val gender: PersonGender,
    @ColumnInfo(name = "birthYear") val birthYear: Int?,
    @ColumnInfo(name = "birthDateEpochDay") val birthDateEpochDay: Long?,
    @ColumnInfo(name = "position") val position: Int
)
