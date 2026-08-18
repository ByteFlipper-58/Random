package com.byteflipper.random.ui.settings.graphics

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.ui.settings.SettingsUiEvent
import com.byteflipper.random.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGraphicsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsGraphicsScaffold(onBack) { inner ->
        SettingsGraphicsContent(
            modifier = Modifier.padding(inner),
            state = settings,
            onSetGraphicsQuality = { viewModel.onEvent(SettingsUiEvent.SetGraphicsQuality(it)) }
        )
    }
}
