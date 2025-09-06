package com.byteflipper.random.ui.settings

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.AppLanguage
import com.byteflipper.random.ui.components.PreferenceCategory
import com.byteflipper.random.ui.components.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.general)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            PreferenceCategory(title = stringResource(R.string.language), description = stringResource(R.string.language_description))
            val langKey = when (settings.appLanguage) {
                AppLanguage.System -> "system"
                AppLanguage.English -> "en"
                AppLanguage.Russian -> "ru"
                AppLanguage.Ukrainian -> "uk"
                AppLanguage.Belarusian -> "be"
                AppLanguage.Polish -> "pl"
            }
            val langItems = listOf(
                "system" to stringResource(R.string.language_system),
                "en" to stringResource(R.string.language_english),
                "ru" to stringResource(R.string.language_russian),
                "uk" to stringResource(R.string.language_ukrainian),
                "be" to stringResource(R.string.language_belarusian),
                "pl" to stringResource(R.string.language_polish)
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                langItems.forEach { (key, label) ->
                    FilterChip(
                        selected = langKey == key,
                        onClick = {
                            val language = when (key) {
                                "en" -> AppLanguage.English
                                "ru" -> AppLanguage.Russian
                                "uk" -> AppLanguage.Ukrainian
                                "be" -> AppLanguage.Belarusian
                                "pl" -> AppLanguage.Polish
                                else -> AppLanguage.System
                            }
                            viewModel.setAppLanguage(language)
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            PreferenceCategory(title = stringResource(R.string.vibration), description = stringResource(R.string.vibration_description))
            SwitchPreference(
                title = stringResource(R.string.vibration),
                checked = settings.hapticsEnabled,
                icon = painterResource(id = R.drawable.mobile_vibrate_24px),
                onCheckedChange = { enabled -> viewModel.setHapticsEnabled(enabled) }
            )
        }
    }
}


