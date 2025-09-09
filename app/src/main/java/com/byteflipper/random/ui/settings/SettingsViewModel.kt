package com.byteflipper.random.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.settings.AppLanguage
import com.byteflipper.random.data.settings.FabSizeSetting
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.settings.ThemeMode
import com.byteflipper.random.data.settings.HapticsIntensity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.byteflipper.random.data.settings.Settings()
    )

    private val _effects = MutableSharedFlow<SettingsUiEffect>()
    val effects: SharedFlow<SettingsUiEffect> = _effects

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SetThemeMode -> viewModelScope.launch { settingsRepository.setThemeMode(event.mode) }
            is SettingsUiEvent.SetDynamicColors -> viewModelScope.launch { settingsRepository.setDynamicColors(event.enabled) }
            is SettingsUiEvent.SetFabSize -> viewModelScope.launch { settingsRepository.setFabSize(event.size) }
            is SettingsUiEvent.SetAppLanguage -> viewModelScope.launch { settingsRepository.setAppLanguage(event.language) }
            is SettingsUiEvent.SetHapticsEnabled -> viewModelScope.launch { settingsRepository.setHapticsEnabled(event.enabled) }
            is SettingsUiEvent.SetHapticsIntensity -> viewModelScope.launch { settingsRepository.setHapticsIntensity(event.intensity) }
        }
    }
}

sealed interface SettingsUiEvent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsUiEvent
    data class SetDynamicColors(val enabled: Boolean) : SettingsUiEvent
    data class SetFabSize(val size: FabSizeSetting) : SettingsUiEvent
    data class SetAppLanguage(val language: AppLanguage) : SettingsUiEvent
    data class SetHapticsEnabled(val enabled: Boolean) : SettingsUiEvent
    data class SetHapticsIntensity(val intensity: HapticsIntensity) : SettingsUiEvent
}

sealed interface SettingsUiEffect
