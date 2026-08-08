package com.byteflipper.random.data.db

import androidx.room.TypeConverter
import com.byteflipper.random.data.person.PersonGender
import com.byteflipper.random.data.team.TeamSplitMode

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String>? = when (value) {
        null -> null
        "" -> emptyList()
        else -> value.split("\u0001").map { it }
    }

    @TypeConverter
    fun listToString(list: List<String>?): String? = when (list) {
        null -> null
        emptyList<String>() -> ""
        else -> list.joinToString("\u0001")
    }

    @TypeConverter
    fun fromPersonGender(value: String?): PersonGender = when (value) {
        PersonGender.Male.name -> PersonGender.Male
        PersonGender.Female.name -> PersonGender.Female
        else -> PersonGender.Unspecified
    }

    @TypeConverter
    fun personGenderToString(value: PersonGender?): String = (value ?: PersonGender.Unspecified).name

    @TypeConverter
    fun fromTeamSplitMode(value: String?): TeamSplitMode = when (value) {
        TeamSplitMode.GroupSize.name -> TeamSplitMode.GroupSize
        else -> TeamSplitMode.TeamCount
    }

    @TypeConverter
    fun teamSplitModeToString(value: TeamSplitMode?): String = (value ?: TeamSplitMode.TeamCount).name
}


