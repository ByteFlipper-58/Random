package com.byteflipper.random.data.team

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamPresetRepository @Inject constructor(
    private val dao: TeamPresetDao
) {
    fun observeAll(): Flow<List<TeamPreset>> = dao.observeAll()

    fun observeAllWithCounts(): Flow<List<TeamPresetWithCount>> = dao.observeAllWithCounts()

    suspend fun getById(id: Long): TeamPresetWithMembers? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun upsertPreset(preset: TeamPreset, memberIdsInOrder: List<Long>): Long = withContext(Dispatchers.IO) {
        dao.upsertPreset(
            preset = preset,
            members = memberIdsInOrder.mapIndexed { index, personId ->
                TeamPresetMember(
                    presetId = preset.id,
                    personId = personId,
                    position = index
                )
            }
        )
    }

    suspend fun deletePreset(preset: TeamPreset) = withContext(Dispatchers.IO) {
        dao.deletePreset(preset)
    }

    suspend fun markUsed(id: Long, usedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        dao.markUsed(id, usedAt)
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        dao.setPinned(id, isPinned)
    }
}
