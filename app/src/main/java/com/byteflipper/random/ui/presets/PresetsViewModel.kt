package com.byteflipper.random.ui.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

enum class PresetFilter {
    All,
    Recent,
    MostUsed
}

data class PresetsUiState(
    val filter: PresetFilter = PresetFilter.All,
    val sortAscending: Boolean = true,
    val availablePresets: List<ListPreset> = emptyList(),
    val presets: List<ListPreset> = emptyList(),
    val hasAnyPresets: Boolean = false,
    val lastUsedPresetId: Long? = null,
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0
)

@HiltViewModel
class PresetsViewModel @Inject constructor(
    private val listPresetRepository: ListPresetRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private companion object {
        const val RECENT_PRESETS_LIMIT = 12
    }

    private data class ListViewportState(
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0
    )

    private val filter = MutableStateFlow(PresetFilter.All)
    private val sortAscending = MutableStateFlow(true)
    private val listViewportState = MutableStateFlow(ListViewportState())

    val settings: StateFlow<Settings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    private val contentState = combine(
        listPresetRepository.observeAll(),
        filter,
        sortAscending
    ) { presets, currentFilter, currentSortAscending ->
        val recentPresetIds = presets
            .sortedByDescending(::activityAt)
            .take(RECENT_PRESETS_LIMIT)
            .mapTo(mutableSetOf()) { it.id }

        val availablePresets = presets
            .filter { preset ->
                when (currentFilter) {
                    PresetFilter.All -> true
                    PresetFilter.Recent -> preset.id in recentPresetIds
                    PresetFilter.MostUsed -> preset.useCount > 0
                }
            }
            .sortedWith(comparatorFor(currentFilter, currentSortAscending))

        val lastUsedPresetId = presets
            .filter { it.lastUsedAt != null }
            .maxByOrNull { it.lastUsedAt ?: Long.MIN_VALUE }
            ?.id

        PresetsUiState(
            filter = currentFilter,
            sortAscending = currentSortAscending,
            availablePresets = availablePresets,
            presets = availablePresets,
            hasAnyPresets = presets.isNotEmpty(),
            lastUsedPresetId = lastUsedPresetId
        )
    }

    val uiState: StateFlow<PresetsUiState> = combine(
        contentState,
        listViewportState
    ) { state, viewport ->
        state.copy(
            firstVisibleItemIndex = viewport.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = viewport.firstVisibleItemScrollOffset
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PresetsUiState()
    )

    fun updateFilter(value: PresetFilter) {
        filter.update { value }
    }

    fun toggleSortOrder() {
        sortAscending.update { !it }
    }

    fun updateListViewport(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        listViewportState.update { current ->
            if (
                current.firstVisibleItemIndex == firstVisibleItemIndex &&
                current.firstVisibleItemScrollOffset == firstVisibleItemScrollOffset
            ) {
                current
            } else {
                current.copy(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset
                )
            }
        }
    }

    fun renamePreset(preset: ListPreset, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) return

        viewModelScope.launch {
            listPresetRepository.upsert(
                preset.copy(
                    name = trimmedName,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePreset(preset: ListPreset) {
        viewModelScope.launch {
            listPresetRepository.delete(preset)
        }
    }

    fun restorePreset(preset: ListPreset) {
        viewModelScope.launch {
            listPresetRepository.restore(preset)
        }
    }

    suspend fun duplicatePreset(preset: ListPreset, copyName: String): ListPreset {
        return listPresetRepository.duplicate(preset, copyName)
    }

    fun togglePinned(preset: ListPreset) {
        viewModelScope.launch {
            listPresetRepository.setPinned(preset.id, !preset.isPinned)
        }
    }

    private fun comparatorFor(
        filter: PresetFilter,
        ascending: Boolean
    ): Comparator<ListPreset> {
        val locale = Locale.getDefault()

        val comparator = when (filter) {
            PresetFilter.Recent -> compareBy<ListPreset>(::activityAt)
                .thenBy { it.name.lowercase(locale) }

            PresetFilter.MostUsed -> compareBy<ListPreset> { it.useCount }
                .thenBy(::activityAt)
                .thenBy { it.name.lowercase(locale) }

            PresetFilter.All -> compareBy<ListPreset> { it.name.lowercase(locale) }
                .thenByDescending(::activityAt)
        }

        return if (ascending) comparator else comparator.reversed()
    }

    private fun activityAt(preset: ListPreset): Long {
        return preset.lastUsedAt ?: preset.updatedAt
    }
}
