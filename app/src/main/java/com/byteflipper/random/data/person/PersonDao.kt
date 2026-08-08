package com.byteflipper.random.data.person

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query(
        """
        SELECT * FROM people
        WHERE isArchived = 0
        ORDER BY displayName COLLATE NOCASE ASC, createdAt ASC
        """
    )
    fun observeAll(): Flow<List<Person>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(person: Person): Long

    @Query("SELECT * FROM people WHERE id = :id AND isArchived = 0")
    suspend fun getById(id: Long): Person?

    @Query("UPDATE people SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archive(id: Long, updatedAt: Long)
}
