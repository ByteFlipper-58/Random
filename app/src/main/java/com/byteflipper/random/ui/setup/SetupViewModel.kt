package com.byteflipper.random.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun setSetupComplete() {
        viewModelScope.launch {
            settingsRepository.setSetupCompleted(true)
        }
    }
}
