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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appearance)) },
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
            PreferenceCategory(title = stringResource(R.string.theme), description = stringResource(R.string.theme_description))
            val themeKey = when (settings.themeMode) {
                ThemeMode.System -> "system"
                ThemeMode.Light -> "light"
                ThemeMode.Dark -> "dark"
                else -> "system"
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                val items = listOf(
                    "system" to stringResource(R.string.system_theme),
                    "light" to stringResource(R.string.light_theme),
                    "dark" to stringResource(R.string.dark_theme)
                )
                items.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = themeKey == key,
                        onClick = {
                            val mode = when (key) {
                                "light" -> ThemeMode.Light
                                "dark" -> ThemeMode.Dark
                                else -> ThemeMode.System
                            }
                            viewModel.setThemeMode(mode)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, items.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            PreferenceCategory(
                title = stringResource(R.string.dynamic_colors),
                description = if (dynamicSupported) stringResource(R.string.dynamic_colors_description) else stringResource(R.string.android_12_required)
            )
            SwitchPreference(
                title = stringResource(R.string.dynamic_colors),
                descriptionOn = stringResource(R.string.use_wallpaper_colors),
                descriptionOff = stringResource(R.string.disabled),
                checked = settings.dynamicColors && dynamicSupported,
                onCheckedChange = { enabled -> if (dynamicSupported) viewModel.setDynamicColors(enabled) }
            )

            PreferenceCategory(title = stringResource(R.string.fab_size), description = stringResource(R.string.fab_size_description))
            val fabKey = when (settings.fabSize) {
                FabSizeSetting.Small -> "s"
                FabSizeSetting.Medium -> "m"
                FabSizeSetting.Large -> "l"
                else -> "m"
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                val fabItems = listOf(
                    "s" to stringResource(R.string.fab_size_small),
                    "m" to stringResource(R.string.fab_size_medium),
                    "l" to stringResource(R.string.fab_size_large)
                )
                fabItems.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = fabKey == key,
                        onClick = {
                            val size = when (key) {
                                "s" -> FabSizeSetting.Small
                                "l" -> FabSizeSetting.Large
                                else -> FabSizeSetting.Medium
                            }
                            viewModel.setFabSize(size)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, fabItems.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}


