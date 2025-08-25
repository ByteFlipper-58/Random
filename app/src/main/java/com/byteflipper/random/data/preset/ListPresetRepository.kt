package com.byteflipper.random.data.preset

import com.byteflipper.random.data.di.DatabaseModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ListPresetRepository(private val dao: ListPresetDao) {
    fun observeAll(): Flow<List<ListPreset>> = dao.observeAll()

    suspend fun upsert(preset: ListPreset): Long = withContext(Dispatchers.IO) {
        dao.upsert(preset)
    }

    suspend fun delete(preset: ListPreset) = withContext(Dispatchers.IO) {
        dao.delete(preset)
    }

    companion object {
        fun fromContext(context: android.content.Context): ListPresetRepository {
            val db = DatabaseModule.provideDatabase(context)
            return ListPresetRepository(db.listPresetDao())
        }
    }
}


