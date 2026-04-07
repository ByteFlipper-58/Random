package com.byteflipper.random.data.preset

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListPresetRepository @Inject constructor(
    private val dao: ListPresetDao
) {
    fun observeAll(): Flow<List<ListPreset>> = dao.observeAll()

    suspend fun upsert(preset: ListPreset): Long = withContext(Dispatchers.IO) {
        dao.upsert(preset)
    }

    suspend fun delete(preset: ListPreset) = withContext(Dispatchers.IO) {
        dao.delete(preset)
    }

    suspend fun restore(preset: ListPreset): Long = withContext(Dispatchers.IO) {
        dao.upsert(preset)
    }

    suspend fun getById(id: Long): ListPreset? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun markUsed(id: Long, usedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        dao.markUsed(id, usedAt)
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        dao.setPinned(id, isPinned)
    }

    suspend fun duplicate(preset: ListPreset, copyName: String): ListPreset = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val duplicatedPreset = ListPreset(
            name = copyName,
            items = preset.items,
            createdAt = now,
            updatedAt = now
        )
        val duplicatedId = dao.upsert(duplicatedPreset)
        duplicatedPreset.copy(id = duplicatedId)
    }
}


