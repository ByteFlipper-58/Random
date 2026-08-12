package com.byteflipper.random.ui.wheel.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R

@Composable
internal fun WheelResultPanel(
    isSpinning: Boolean,
    lastResult: String?,
    currentSectorText: State<String>,
    modifier: Modifier = Modifier
) {
    // The font size is animated rather than Modifier.scale, which takes no part in layout and let
    // an enlarged winner run past the edges and get clipped on long words.
    val baseFontSize = MaterialTheme.typography.headlineMedium.fontSize
    val fontScale by animateFloatAsState(
        targetValue = if (!isSpinning && lastResult != null) 1.3f else 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "result_font_scale"
    )

    // TalkBack does not announce text changes on its own. The live region is attached to the final
    // result only: while spinning the sector changes dozens of times a second, which would turn
    // the announcements into noise.
    val resultAnnouncement = lastResult
        ?.takeIf { !isSpinning }
        ?.let { stringResource(R.string.wheel_a11y_result, it) }

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // The sector value is read only while spinning, so the panel is not recomposed for nothing.
        val displayText = if (isSpinning) currentSectorText.value else lastResult.orEmpty()

        if (displayText.isNotEmpty()) {
            Text(
                text = if (!isSpinning && lastResult != null) "🎉 $displayText" else displayText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = baseFontSize * fontScale
                ),
                fontWeight = if (!isSpinning && lastResult != null) FontWeight.Bold else FontWeight.Medium,
                color = if (!isSpinning && lastResult != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .animateContentSize()
                    .then(
                        if (resultAnnouncement != null) {
                            Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = resultAnnouncement
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        } else {
            Text(
                text = stringResource(R.string.wheel_tap_to_spin),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun WheelStatusPanel(
    visibleItemsCount: Int,
    noRepeats: Boolean,
    excludedCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.wheel_items_count, visibleItemsCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (noRepeats && excludedCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.wheel_used_count, excludedCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
