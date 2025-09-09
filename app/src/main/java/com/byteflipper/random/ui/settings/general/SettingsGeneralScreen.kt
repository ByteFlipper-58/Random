package com.byteflipper.random.ui.settings.general

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.ui.settings.SettingsUiEvent
import com.byteflipper.random.ui.settings.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsGeneralScaffold(onBack) { inner ->
        SettingsGeneralContent(
            modifier = Modifier.padding(inner),
            state = settings,
            onSetLanguage = { viewModel.onEvent(SettingsUiEvent.SetAppLanguage(it)) },
            onSetHapticsEnabled = { viewModel.onEvent(SettingsUiEvent.SetHapticsEnabled(it)) },
            onSetHapticsIntensity = { viewModel.onEvent(SettingsUiEvent.SetHapticsIntensity(it)) }
        )
    }
}