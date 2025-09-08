package com.byteflipper.random.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.FabSizeSetting
import com.byteflipper.random.data.settings.ThemeMode
import com.byteflipper.random.ui.components.PreferenceCategory
import com.byteflipper.random.ui.components.SwitchPreference
import com.byteflipper.random.ui.components.RadioButtonGroup
import com.byteflipper.random.ui.components.RadioOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsAppearanceScaffold(onBack) { inner ->
        SettingsAppearanceContent(
            modifier = Modifier.padding(inner),
            state = settings,
            onSetTheme = { viewModel.setThemeMode(it) },
            onSetDynamicColors = { viewModel.setDynamicColors(it) },
            onSetFabSize = { viewModel.setFabSize(it) }
        )
    }
}


