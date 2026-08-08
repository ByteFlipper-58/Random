package com.byteflipper.random.data.team

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TeamPresetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertPresetEntity(preset: TeamPreset): Long

    @Delete
    protected abstract suspend fun deletePresetEntity(preset: TeamPreset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMembers(members: List<TeamPresetMember>)

    @Query(
        """
        SELECT * FROM team_presets
        ORDER BY isPinned DESC,
                 CASE WHEN lastUsedAt IS NULL THEN 1 ELSE 0 END ASC,
                 lastUsedAt DESC,
                 updatedAt DESC,
                 name COLLATE NOCASE ASC
        """
    )
    abstract fun observeAll(): Flow<List<TeamPreset>>

    @Query(
        """
        SELECT tp.*,
               COUNT(CASE WHEN p.isArchived = 0 THEN 1 END) AS aliveMemberCount
        FROM team_presets tp
        LEFT JOIN team_preset_members tpm ON tpm.presetId = tp.id
        LEFT JOIN people p ON p.id = tpm.personId
        GROUP BY tp.id
        ORDER BY tp.isPinned DESC,
                 CASE WHEN tp.lastUsedAt IS NULL THEN 1 ELSE 0 END ASC,
                 tp.lastUsedAt DESC,
                 tp.updatedAt DESC,
                 tp.name COLLATE NOCASE ASC
        """
    )
    abstract fun observeAllWithCounts(): Flow<List<TeamPresetWithCount>>

    @Query("SELECT * FROM team_presets WHERE id = :id")
    protected abstract suspend fun getPresetEntityById(id: Long): TeamPreset?

    @Query(
        """
        SELECT
            people.id AS personId,
            people.displayName AS displayName,
            people.gender AS gender,
            people.birthYear AS birthYear,
            people.birthDateEpochDay AS birthDateEpochDay,
            team_preset_members.position AS position
        FROM team_preset_members
        INNER JOIN people ON people.id = team_preset_members.personId
        WHERE team_preset_members.presetId = :presetId
          AND people.isArchived = 0
        ORDER BY team_preset_members.position ASC, people.displayName COLLATE NOCASE ASC
        """
    )
    protected abstract suspend fun getMembersForPreset(presetId: Long): List<TeamMemberRef>

    @Query("DELETE FROM team_preset_members WHERE presetId = :presetId")
    protected abstract suspend fun deleteMembersForPreset(presetId: Long)

    @Query("UPDATE team_presets SET lastUsedAt = :usedAt, useCount = useCount + 1 WHERE id = :id")
    abstract suspend fun markUsed(id: Long, usedAt: Long)

    @Query("UPDATE team_presets SET isPinned = :isPinned WHERE id = :id")
    abstract suspend fun setPinned(id: Long, isPinned: Boolean)

    @Transaction
    open suspend fun upsertPreset(preset: TeamPreset, members: List<TeamPresetMember>): Long {
        val presetId = upsertPresetEntity(preset)
        deleteMembersForPreset(presetId)
        if (members.isNotEmpty()) {
            insertMembers(
                members.mapIndexed { index, member ->
                    member.copy(presetId = presetId, position = index)
                }
            )
        }
        return presetId
    }

    @Transaction
    open suspend fun getById(id: Long): TeamPresetWithMembers? {
        val preset = getPresetEntityById(id) ?: return null
        val members = getMembersForPreset(id)
        return TeamPresetWithMembers(preset = preset, members = members)
    }

    @Transaction
    open suspend fun deletePreset(preset: TeamPreset) {
        deleteMembersForPreset(preset.id)
        deletePresetEntity(preset)
    }
}
