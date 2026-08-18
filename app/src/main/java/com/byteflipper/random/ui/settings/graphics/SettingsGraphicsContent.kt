package com.byteflipper.random.ui.settings.graphics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SimulationQuality
import com.byteflipper.random.ui.settings.components.PreferenceCategory
import com.byteflipper.random.ui.settings.components.RadioButtonGroup
import com.byteflipper.random.ui.settings.components.RadioOption

/**
 * How much work the app's 3D scenes may do — the ball of fate's liquid and the dice tray both run at
 * the tier chosen here.
 *
 * This is the device's business rather than any one generator's — what a phone can afford to draw
 * does not change from one throw to the next — so it is answered once here instead of again in every
 * generator's own sheet.
 */
@Composable
fun SettingsGraphicsContent(
    modifier: Modifier = Modifier,
    state: Settings,
    onSetGraphicsQuality: (SimulationQuality) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        PreferenceCategory(
            title = stringResource(R.string.quality),
            description = stringResource(R.string.quality_desc)
        )
        val qualityKey = when (state.graphicsQuality) {
            SimulationQuality.Auto -> "auto"
            SimulationQuality.High -> "high"
            SimulationQuality.Balanced -> "balanced"
            SimulationQuality.Battery -> "battery"
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            RadioButtonGroup(
                options = listOf(
                    RadioOption(key = "auto", title = stringResource(R.string.quality_auto)),
                    RadioOption(key = "high", title = stringResource(R.string.quality_high)),
                    RadioOption(key = "balanced", title = stringResource(R.string.quality_balanced)),
                    RadioOption(key = "battery", title = stringResource(R.string.quality_battery))
                ),
                selectedKey = qualityKey,
                onOptionSelected = { key ->
                    val quality = when (key) {
                        "high" -> SimulationQuality.High
                        "balanced" -> SimulationQuality.Balanced
                        "battery" -> SimulationQuality.Battery
                        else -> SimulationQuality.Auto
                    }
                    onSetGraphicsQuality(quality)
                }
            )
        }
    }
}
