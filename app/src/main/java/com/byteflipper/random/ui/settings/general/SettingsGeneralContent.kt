package com.byteflipper.random.ui.settings.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.AppLanguage
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.ui.components.CustomChip
import com.byteflipper.random.ui.settings.components.PreferenceCategory
import com.byteflipper.random.ui.settings.components.SwitchPreference
import com.byteflipper.random.ui.components.LocalHapticsManager

@Composable
fun SettingsGeneralContent(
    modifier: Modifier = Modifier,
    state: Settings,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetHapticsEnabled: (Boolean) -> Unit,
    onSetHapticsIntensity: (HapticsIntensity) -> Unit,
    onSetShakeToGenerateEnabled: (Boolean) -> Unit
) {
    val hapticsManager = LocalHapticsManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        PreferenceCategory(
            title = stringResource(R.string.language),
            description = stringResource(R.string.language_description)
        )

        val languageItems = AppLanguage.entries.map { language ->
            language to stringResource(language.labelResId())
        }

        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            languageItems.forEach { (language, label) ->
                CustomChip(
                    label = label,
                    selected = state.appLanguage == language,
                    onClick = { onSetLanguage(language) }
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        PreferenceCategory(
            title = stringResource(R.string.vibration),
            description = stringResource(R.string.vibration_description)
        )

        SwitchPreference(
            title = stringResource(R.string.vibration),
            descriptionOn = stringResource(R.string.vibration_switch_description_on),
            descriptionOff = stringResource(R.string.vibration_switch_description_off),
            checked = state.hapticsEnabled,
            icon = painterResource(id = R.drawable.mobile_vibrate_24px),
            onCheckedChange = onSetHapticsEnabled
        )

        if (state.hapticsEnabled) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.vibration_intensity), style = MaterialTheme.typography.titleSmall)
                val sliderValue = when (state.hapticsIntensity) {
                    HapticsIntensity.Low -> 0f
                    HapticsIntensity.Medium -> 1f
                    HapticsIntensity.High -> 2f
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { value ->
                        val level = when (value.coerceIn(0f, 2f).toInt()) {
                            0 -> HapticsIntensity.Low
                            2 -> HapticsIntensity.High
                            else -> HapticsIntensity.Medium
                        }
                        onSetHapticsIntensity(level)
                        hapticsManager?.performPress(level)
                    },
                    valueRange = 0f..2f,
                    steps = 1,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.vibration_low), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.vibration_medium), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.vibration_high), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        PreferenceCategory(
            title = stringResource(R.string.shake_to_generate),
            description = stringResource(R.string.shake_to_generate_description)
        )

        SwitchPreference(
            title = stringResource(R.string.shake_to_generate),
            descriptionOn = stringResource(R.string.shake_to_generate_description_on),
            descriptionOff = stringResource(R.string.shake_to_generate_description_off),
            checked = state.shakeToGenerateEnabled,
            icon = painterResource(id = R.drawable.vibration_24px),
            onCheckedChange = onSetShakeToGenerateEnabled
        )
    }
}

private fun AppLanguage.labelResId(): Int = when (this) {
    AppLanguage.System -> R.string.language_system
    AppLanguage.English -> R.string.language_native_english
    AppLanguage.Russian -> R.string.language_native_russian
    AppLanguage.Ukrainian -> R.string.language_native_ukrainian
    AppLanguage.Belarusian -> R.string.language_native_belarusian
    AppLanguage.Polish -> R.string.language_native_polish
    AppLanguage.Kazakh -> R.string.language_native_kazakh
    AppLanguage.Hindi -> R.string.language_native_hindi
    AppLanguage.Spanish -> R.string.language_native_spanish
    AppLanguage.French -> R.string.language_native_french
}


