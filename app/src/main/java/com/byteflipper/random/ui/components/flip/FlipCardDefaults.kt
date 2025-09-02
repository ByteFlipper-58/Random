package com.byteflipper.random.ui.components.flip

import androidx.compose.animation.core.Spring
import com.byteflipper.random.ui.theme.Dimens
import com.byteflipper.random.ui.theme.ShapesTokens

object FlipCardDefaults {
    // Geometry
    const val CameraDistanceMultiplier = 24f
    val CardShape = ShapesTokens.CardShape

    // Elevation
    val CardElevation = Dimens.CardElevation

    // Spin behavior
    const val RpsShort = 2.5f
    const val RpsLong = 0.2f
    const val AmpShort = 15f
    const val AmpLong = 5f

    // Reveal timing
    const val RevealFraction = 1f / 3f
    const val RevealDelayMs = 100

    // Exit animation
    const val ExitRotateZ = -360f
    const val ExitAlphaMs = 400
    const val ScrimHideMs = 350
    const val FlipHideTextMs = 100
    val ExitScaleSpringStiffness = Spring.StiffnessLow
    val ExitScaleDamping = 0.7f
    val ExitTransSpringStiffness = Spring.StiffnessLow
    val ExitTransDamping = 0.65f
}


