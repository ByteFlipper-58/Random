package com.byteflipper.random.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.AppLanguage
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.ui.components.CustomChip
import com.byteflipper.random.ui.components.PreferenceCategory
import com.byteflipper.random.ui.components.SwitchPreference
import com.byteflipper.random.ui.components.LocalHapticsManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsGeneralScaffold(onBack) { inner ->
        SettingsGeneralContent(
            modifier = Modifier.padding(inner),
            state = settings,
            onSetLanguage = { viewModel.setAppLanguage(it) },
            onSetHapticsEnabled = { viewModel.setHapticsEnabled(it) },
            onSetHapticsIntensity = { viewModel.setHapticsIntensity(it) }
        )
    }
}