package com.byteflipper.random.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.AppLanguage
import com.byteflipper.random.ui.components.PreferenceCategory
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.ui.components.SwitchPreference

@Composable
fun CustomChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "chip_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        animationSpec = spring(stiffness = 400f),
        label = "background_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = spring(stiffness = 400f),
        label = "border_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = 400f),
        label = "text_color"
    )

    val shadowElevation by animateFloatAsState(
        targetValue = if (selected) 8f else 2f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "shadow_elevation"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = shadowElevation.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (selected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else
                    Color.Black.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = if (selected) 0.3.sp else 0.sp
        )
    }
}

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
            PreferenceCategory(
                title = stringResource(R.string.language),
                description = stringResource(R.string.language_description)
            )

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                langItems.forEach { (key, label) ->
                    CustomChip(
                        label = label,
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
                        }
                    )
                }
            }

            Divider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

            PreferenceCategory(
                title = stringResource(R.string.vibration),
                description = stringResource(R.string.vibration_description)
            )

            SwitchPreference(
                title = stringResource(R.string.vibration),
                checked = settings.hapticsEnabled,
                icon = painterResource(id = R.drawable.mobile_vibrate_24px),
                onCheckedChange = { enabled -> viewModel.setHapticsEnabled(enabled) }
            )

            if (settings.hapticsEnabled) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(R.string.vibration_intensity), style = MaterialTheme.typography.titleSmall)
                    val sliderValue = when (settings.hapticsIntensity) {
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
                            viewModel.setHapticsIntensity(level)
                        },
                        valueRange = 0f..2f,
                        steps = 1,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.vibration_low), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(R.string.vibration_medium), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(R.string.vibration_high), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}