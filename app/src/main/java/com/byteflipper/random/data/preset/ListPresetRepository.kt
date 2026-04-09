package com.byteflipper.random.data.preset

import com.byteflipper.random.data.preset.transfer.ImportedListPreset
import com.byteflipper.random.data.preset.transfer.PresetImportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListPresetRepository @Inject constructor(
    private val dao: ListPresetDao
) {
    fun observeAll(): Flow<List<ListPreset>> = dao.observeAll()

    suspend fun getAll(): List<ListPreset> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun getAllByIds(ids: Collection<Long>): List<ListPreset> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val lookup = ids.toSet()
        dao.getAll().filter { preset -> preset.id in lookup }
    }

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

    suspend fun importPresets(
        importedPresets: List<ImportedListPreset>,
        mode: PresetImportMode = PresetImportMode.Copy
    ): List<ListPreset> = withContext(Dispatchers.IO) {
        val existingPresets = dao.getAll()
        val existingByName = existingPresets.associateBy { preset -> normalizeName(preset.name) }.toMutableMap()
        val knownNames = existingPresets
            .mapTo(mutableSetOf()) { preset -> normalizeName(preset.name) }

        importedPresets.map { importedPreset ->
            val now = System.currentTimeMillis()
            val normalizedImportedName = normalizeName(importedPreset.name)

            when {
                mode == PresetImportMode.ReplaceMatching && normalizedImportedName in existingByName -> {
                    val existing = checkNotNull(existingByName[normalizedImportedName])
                    val updatedPreset = existing.copy(
                        name = importedPreset.name.trim().ifEmpty { existing.name },
                        items = importedPreset.items,
                        updatedAt = now
                    )
                    dao.upsert(updatedPreset)
                    existingByName[normalizeName(updatedPreset.name)] = updatedPreset
                    updatedPreset
                }

                else -> {
                    val resolvedName = resolveImportedName(importedPreset.name, knownNames)
                    val preset = ListPreset(
                        name = resolvedName,
                        items = importedPreset.items,
                        createdAt = now,
                        updatedAt = now
                    )
                    val insertedId = dao.upsert(preset)
                    knownNames += normalizeName(resolvedName)
                    val insertedPreset = preset.copy(id = insertedId)
                    existingByName[normalizeName(resolvedName)] = insertedPreset
                    insertedPreset
                }
            }
        }
    }

    suspend fun mergePresets(
        presets: List<ListPreset>,
        mergedName: String
    ): ListPreset = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val mergedPreset = ListPreset(
            name = mergedName.trim().ifEmpty { "Merged preset" },
            items = presets.flatMap { it.items },
            createdAt = now,
            updatedAt = now
        )
        val insertedId = dao.upsert(mergedPreset)
        mergedPreset.copy(id = insertedId)
    }

    internal fun resolveImportedName(
        baseName: String,
        knownNames: Set<String>
    ): String {
        val trimmedBaseName = baseName.trim().ifEmpty { "Preset" }
        val normalizedBaseName = normalizeName(trimmedBaseName)
        if (normalizedBaseName !in knownNames) {
            return trimmedBaseName
        }

        var copyIndex = 1
        while (true) {
            val suffix = if (copyIndex == 1) "(copy)" else "(copy $copyIndex)"
            val candidate = "$trimmedBaseName $suffix"
            if (normalizeName(candidate) !in knownNames) {
                return candidate
            }
            copyIndex += 1
        }
    }

    private fun normalizeName(name: String): String {
        return name.trim().lowercase(Locale.ROOT)
    }
}


