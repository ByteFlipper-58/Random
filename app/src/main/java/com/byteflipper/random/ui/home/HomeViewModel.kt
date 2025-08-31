package com.byteflipper.random.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    val listPresetRepository: ListPresetRepository
) : ViewModel() {

    val presets: StateFlow<List<ListPreset>> = listPresetRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deletePreset(preset: ListPreset) {
        viewModelScope.launch {
            listPresetRepository.delete(preset)
        }
    }

    fun createPreset(name: String, items: List<String>) {
        viewModelScope.launch {
            val preset = ListPreset(name = name, items = items)
            listPresetRepository.upsert(preset)
        }
    }

    fun renamePreset(preset: ListPreset, newName: String) {
        viewModelScope.launch {
            val updatedPreset = preset.copy(name = newName)
            listPresetRepository.upsert(updatedPreset)
        }
    }
}
