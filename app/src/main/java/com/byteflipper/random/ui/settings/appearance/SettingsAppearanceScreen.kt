package com.byteflipper.random.ui.settings.appearance

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
fun SettingsAppearanceScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsAppearanceScaffold(onBack) { inner ->
        SettingsAppearanceContent(
            modifier = Modifier.padding(inner),
            state = settings,
            onSetTheme = { viewModel.onEvent(SettingsUiEvent.SetThemeMode(it)) },
            onSetDynamicColors = { viewModel.onEvent(SettingsUiEvent.SetDynamicColors(it)) },
            onSetFabSize = { viewModel.onEvent(SettingsUiEvent.SetFabSize(it)) }
        )
    }
}


