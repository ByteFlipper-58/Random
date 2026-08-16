package com.byteflipper.random.ui.ball.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.BallQuality
import com.byteflipper.random.ui.components.ConfigDivider
import com.byteflipper.random.ui.components.ConfigHeader
import com.byteflipper.random.ui.components.ConfigRadioOption

/**
 * The ball's own settings: what the ask is allowed to draw, whether the device's tilt reaches the
 * liquid, and how much work the simulation may do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BallSettingsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    noRepeats: Boolean,
    onNoRepeatsChange: (Boolean) -> Unit,
    tiltEnabled: Boolean,
    onTiltEnabledChange: (Boolean) -> Unit,
    quality: BallQuality,
    onQualityChange: (BallQuality) -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(modifier = Modifier.size(width = 32.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ConfigHeader()
            ConfigDivider()

            BallSectionLayout(
                icon = painterResource(id = R.drawable.repeat_24px),
                title = stringResource(R.string.ball_no_repeats),
                description = stringResource(R.string.ball_no_repeats_desc),
                onRowClick = { onNoRepeatsChange(!noRepeats) },
                action = {
                    Switch(checked = noRepeats, onCheckedChange = onNoRepeatsChange)
                }
            )

            ConfigDivider()

            BallSectionLayout(
                icon = painterResource(id = R.drawable.mobile_vibrate_24px),
                title = stringResource(R.string.ball_tilt),
                description = stringResource(R.string.ball_tilt_desc),
                onRowClick = { onTiltEnabledChange(!tiltEnabled) },
                action = {
                    Switch(checked = tiltEnabled, onCheckedChange = onTiltEnabledChange)
                }
            )

            ConfigDivider()

            BallSectionLayout(
                icon = painterResource(id = R.drawable.bolt_24px),
                title = stringResource(R.string.ball_quality),
                description = stringResource(R.string.ball_quality_desc)
            ) {
                // The same radio list the teams' split modes use: four wordy labels read better
                // stacked than squeezed into a row of segments that would truncate.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QUALITY_OPTIONS.forEach { (option, labelRes) ->
                        ConfigRadioOption(
                            selected = quality == option,
                            title = stringResource(labelRes),
                            onClick = { onQualityChange(option) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** In the order they read as a scale: let the app decide, then most work to least. */
private val QUALITY_OPTIONS = listOf(
    BallQuality.Auto to R.string.ball_quality_auto,
    BallQuality.High to R.string.ball_quality_high,
    BallQuality.Balanced to R.string.ball_quality_balanced,
    BallQuality.Battery to R.string.ball_quality_battery
)

/** Same row shape the other generators' settings sheets use: icon, text, action, optional body. */
@Composable
private fun BallSectionLayout(
    icon: Painter,
    title: String,
    description: String? = null,
    onRowClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onRowClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onRowClick
                        )
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                description?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            action?.let {
                Spacer(modifier = Modifier.width(8.dp))
                it()
            }
        }

        content?.let {
            Spacer(modifier = Modifier.height(16.dp))
            it()
        }
    }
}
