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

    suspend fun getById(id: Long): ListPreset? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }
}


