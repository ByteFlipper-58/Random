package com.byteflipper.random.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.FabSizeSetting
import com.byteflipper.random.data.settings.ThemeMode
import com.byteflipper.random.ui.components.PreferenceCategory
import com.byteflipper.random.ui.components.SwitchPreference
import com.byteflipper.random.ui.components.RadioButtonGroup
import com.byteflipper.random.ui.components.RadioOption

@Composable
fun SettingsAppearanceContent(
    modifier: Modifier = Modifier,
    state: com.byteflipper.random.data.settings.Settings,
    onSetTheme: (ThemeMode) -> Unit,
    onSetDynamicColors: (Boolean) -> Unit,
    onSetFabSize: (FabSizeSetting) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        PreferenceCategory(title = stringResource(R.string.theme), description = stringResource(R.string.theme_description))
        val themeKey = when (state.themeMode) {
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
                        onSetTheme(mode)
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

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        PreferenceCategory(
            title = stringResource(R.string.dynamic_colors),
        )
        SwitchPreference(
            title = stringResource(R.string.dynamic_colors),
            descriptionOn = stringResource(R.string.dynamic_colors_description),
            descriptionOff = stringResource(R.string.dynamic_colors_description),
            checked = state.dynamicColors && dynamicSupported,
            onCheckedChange = { enabled -> if (dynamicSupported) onSetDynamicColors(enabled) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        PreferenceCategory(title = stringResource(R.string.fab_size), description = stringResource(R.string.fab_size_description))
        val fabKey = when (state.fabSize) {
            FabSizeSetting.Small -> "s"
            FabSizeSetting.Medium -> "m"
            FabSizeSetting.Large -> "l"
            else -> "m"
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            RadioButtonGroup(
                options = listOf(
                    RadioOption(
                        key = "s",
                        title = stringResource(R.string.fab_size_small),
                        description = stringResource(R.string.fab_size_small_desc)
                    ),
                    RadioOption(
                        key = "m",
                        title = stringResource(R.string.fab_size_medium),
                        description = stringResource(R.string.fab_size_medium_desc)
                    ),
                    RadioOption(
                        key = "l",
                        title = stringResource(R.string.fab_size_large),
                        description = stringResource(R.string.fab_size_large_desc)
                    )
                ),
                selectedKey = fabKey,
                onOptionSelected = { key ->
                    val size = when (key) {
                        "s" -> FabSizeSetting.Small
                        "l" -> FabSizeSetting.Large
                        else -> FabSizeSetting.Medium
                    }
                    onSetFabSize(size)
                }
            )
        }
    }
}


