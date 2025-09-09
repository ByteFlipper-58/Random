package com.byteflipper.random.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settingsFlow: Flow<Settings> = settingsRepository.settingsFlow

    private val _initialSettings: MutableStateFlow<Settings?> = MutableStateFlow(null)
    val initialSettings: StateFlow<Settings?> = _initialSettings.asStateFlow()

    val appLanguageTagFlow: Flow<String> = settingsRepository.settingsFlow
        .map { it.appLanguage.localeTag }
        .distinctUntilChanged()

    init {
        viewModelScope.launch {
            _initialSettings.value = settingsRepository.settingsFlow.first()
        }
    }
}


