package com.byteflipper.random.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.byteflipper.random.data.settings.SettingsRepository
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    val listPresetRepository: ListPresetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val presets: StateFlow<List<ListPreset>> = listPresetRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.byteflipper.random.data.settings.Settings()
    )

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.DeletePreset -> viewModelScope.launch { listPresetRepository.delete(event.preset) }
            is HomeUiEvent.CreatePreset -> viewModelScope.launch {
                val preset = ListPreset(name = event.name, items = event.items)
                listPresetRepository.upsert(preset)
            }
            is HomeUiEvent.RenamePreset -> viewModelScope.launch {
                val updatedPreset = event.preset.copy(name = event.newName)
                listPresetRepository.upsert(updatedPreset)
            }
        }
    }
}

sealed interface HomeUiEvent {
    data class DeletePreset(val preset: ListPreset) : HomeUiEvent
    data class CreatePreset(val name: String, val items: List<String>) : HomeUiEvent
    data class RenamePreset(val preset: ListPreset, val newName: String) : HomeUiEvent
}
