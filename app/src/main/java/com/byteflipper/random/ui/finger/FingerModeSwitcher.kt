package com.byteflipper.random.ui.finger

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.sin

@Composable
fun FingerModeSwitcher(
    selectedMode: FingerMode,
    onSelectMode: (FingerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = FingerMode.entries
    val targetProgress = modes.indexOf(selectedMode).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "finger_mode_progress"
    )

    val colorScheme = MaterialTheme.colorScheme
    val containerColor = lerp(
        colorScheme.surfaceContainer,
        colorScheme.surfaceContainerHigh,
        0.55f
    )
    val indicatorColor = lerp(
        colorScheme.primaryContainer,
        colorScheme.tertiaryContainer,
        0.3f
    )
    val selectedTextColor = lerp(
        colorScheme.onPrimaryContainer,
        colorScheme.onTertiaryContainer,
        0.25f
    )
    val unselectedTextColor = colorScheme.onSurfaceVariant
    val progress = animatedProgress.coerceIn(0f, (modes.size - 1).toFloat())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(24.dp)
            )
            .pointerInput(Unit) {
                // Consume all touch events inside switcher so they don't fall through to canvas
                detectTapGestures { }
            }
            .padding(4.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabCount = modes.size
            val slotWidth = maxWidth / tabCount
            val transitionFraction = (progress - progress.toInt()).absoluteValue
            val jellyProgress = sin((transitionFraction * PI).toFloat()).coerceIn(0f, 1f)
            val directionPull = (transitionFraction - 0.5f) * 2f
            val stretch = 16.dp * jellyProgress
            val indicatorWidth = slotWidth + stretch
            val trailingPull = 4.dp * directionPull * jellyProgress
            val rawOffset = slotWidth * progress - stretch / 2 + trailingPull
            val indicatorOffset = rawOffset.coerceIn(0.dp, maxWidth - indicatorWidth)
            val indicatorScaleX = 1f + 0.03f * jellyProgress
            val indicatorScaleY = 1f - 0.06f * jellyProgress
            val indicatorLift = (-1.dp * jellyProgress)

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset, y = indicatorLift)
                    .size(width = indicatorWidth, height = 38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(indicatorColor)
                    .graphicsLayer {
                        scaleX = indicatorScaleX
                        scaleY = indicatorScaleY
                    }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                modes.forEachIndexed { index, mode ->
                    val emphasis = (1f - (progress - index).absoluteValue).coerceIn(0f, 1f)
                    val softenedEmphasis = emphasis * emphasis * (3f - 2f * emphasis)
                    val textColor = lerp(unselectedTextColor, selectedTextColor, softenedEmphasis)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelectMode(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                FingerMode.WINNER -> stringResource(R.string.finger_mode_winner)
                                FingerMode.TEAMS -> stringResource(R.string.finger_mode_teams)
                                FingerMode.ORDER -> stringResource(R.string.finger_mode_order)
                            },
                            color = textColor,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
