package com.byteflipper.random.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.byteflipper.random.data.person.Person
import com.byteflipper.random.data.person.PersonDao
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetDao
import com.byteflipper.random.data.team.TeamPreset
import com.byteflipper.random.data.team.TeamPresetDao
import com.byteflipper.random.data.team.TeamPresetMember

@Database(
    entities = [ListPreset::class, Person::class, TeamPreset::class, TeamPresetMember::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listPresetDao(): ListPresetDao
    abstract fun personDao(): PersonDao
    abstract fun teamPresetDao(): TeamPresetDao
}


