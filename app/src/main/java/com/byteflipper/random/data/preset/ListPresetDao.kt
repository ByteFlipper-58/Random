package com.byteflipper.random.data.preset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ListPresetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: ListPreset): Long

    @Update
    suspend fun update(preset: ListPreset)

    @Delete
    suspend fun delete(preset: ListPreset)

    @Query("SELECT * FROM list_presets ORDER BY name ASC")
    fun observeAll(): Flow<List<ListPreset>>

    @Query("SELECT * FROM list_presets WHERE id = :id")
    suspend fun getById(id: Long): ListPreset?
}


