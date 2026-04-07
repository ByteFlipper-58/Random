package com.byteflipper.random.data.preset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListPresetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: ListPreset): Long

    @Delete
    suspend fun delete(preset: ListPreset)

    @Query(
        """
        SELECT * FROM list_presets
        ORDER BY isPinned DESC,
                 CASE WHEN lastUsedAt IS NULL THEN 1 ELSE 0 END ASC,
                 lastUsedAt DESC,
                 updatedAt DESC,
                 name COLLATE NOCASE ASC
        """
    )
    fun observeAll(): Flow<List<ListPreset>>

    @Query("SELECT * FROM list_presets WHERE id = :id")
    suspend fun getById(id: Long): ListPreset?

    @Query("UPDATE list_presets SET lastUsedAt = :usedAt, useCount = useCount + 1 WHERE id = :id")
    suspend fun markUsed(id: Long, usedAt: Long)

    @Query("UPDATE list_presets SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)
}


