package com.byteflipper.random.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetDao

@Database(
    entities = [ListPreset::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listPresetDao(): ListPresetDao
}


