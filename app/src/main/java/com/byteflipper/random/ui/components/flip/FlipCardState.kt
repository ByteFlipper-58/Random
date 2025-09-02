package com.byteflipper.random.ui.components.flip

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job

class FlipCardState internal constructor() {
    // Visibility/state flags
    var isVisible by mutableStateOf(false)
    var isClosing by mutableStateOf(false)
    var isSpinning by mutableStateOf(false)

    // Rotation state
    val cardRotation = Animatable(0f)
    var lastStopAngle by mutableStateOf(0f)

    // Scrim progress (also usable outside for blur)
    val scrimProgress = Animatable(0f)

    // Exit transforms
    val exitAlpha = Animatable(1f)
    val exitRotationZ = Animatable(0f)
    val exitTx = Animatable(0f)
    val exitTy = Animatable(0f)
    val exitScale = Animatable(1f)

    // Text alphas
    val frontTextAlpha = Animatable(1f)
    val backTextAlpha = Animatable(1f)

    // Layout measurements
    var overlayTopLeftInRoot by mutableStateOf(Offset.Zero)
    var overlaySize by mutableStateOf(IntSize.Zero)
    var cardCenterInRoot by mutableStateOf(Offset.Zero)

    // Background jobs
    var closeJob: Job? = null
    var spinJob: Job? = null
}

@Composable
fun rememberFlipCardState(): FlipCardState {
    return remember { FlipCardState() }
}


