package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.byteflipper.random.ui.wheel.WheelSector

@Composable
fun FortuneWheel(
    sectors: List<WheelSector>,
    rotationProvider: () -> Float,
    wheelDescription: String,
    spinActionLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    /** Sector that is greying out, or -1. */
    fadingIndexProvider: () -> Int = { -1 },
    fadeProgressProvider: () -> Float = { 1f },
    /** Sector that is collapsing before removal, or -1. */
    collapsingIndexProvider: () -> Int = { -1 },
    collapseWeightProvider: () -> Float = { 0f },
    /** Winning sector to highlight, or -1. */
    highlightIndexProvider: () -> Int = { -1 },
    highlightStrengthProvider: () -> Float = { 0f },
    gesturesEnabled: Boolean = true,
    onDragStateChange: (Boolean) -> Unit = {},
    onRotate: (deltaDegrees: Float) -> Unit = {},
    /**
     * Deliberately not suspending: a spin lasts seconds and must not live in the gesture coroutine,
     * which is recreated when [gesturesEnabled] changes and would cancel its own animation.
     */
    onFling: (velocityDegreesPerSecond: Float) -> Unit = {},
    onClick: () -> Unit = {}
) {


    Box(
        modifier = modifier
            .size(size)
            .clickable(
                onClick = onClick,
                onClickLabel = spinActionLabel,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            // The wheel is drawn entirely on a Canvas, so to TalkBack it is an empty spot. Without a
            // description a blind user cannot tell what it is or how many options it holds.
            .wheelSpinGesture(
                enabled = gesturesEnabled,
                onDragStateChange = onDragStateChange,
                onRotate = onRotate,
                onFling = onFling
            )
            .semantics(mergeDescendants = true) {
                contentDescription = wheelDescription
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        FortuneWheelCanvas(
            sectors = sectors,
            rotationProvider = rotationProvider,
            fadingIndexProvider = fadingIndexProvider,
            fadeProgressProvider = fadeProgressProvider,
            collapsingIndexProvider = collapsingIndexProvider,
            collapseWeightProvider = collapseWeightProvider,
            highlightIndexProvider = highlightIndexProvider,
            highlightStrengthProvider = highlightStrengthProvider
        )

        FortuneWheelPointer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 8.dp)
                .size(32.dp, 44.dp)
        )
    }
}
