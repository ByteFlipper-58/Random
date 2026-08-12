package com.byteflipper.random.ui.wheel

import android.view.SoundEffectConstants
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.byteflipper.random.utils.findActivity
import kotlinx.coroutines.CancellationException

/**
 * Tap: the wheel is spun up from rest, so it accelerates, runs fast through the middle and coasts
 * out for a long time. The initial speed is zero, otherwise the spin starts with a jerk.
 */
private val WheelTapSpinEasing = CubicBezierEasing(0.42f, 0f, 0.12f, 1f)

/**
 * Fling: the wheel is already moving with the hand, so it must only decay, never accelerate again.
 */
private val WheelFlingSpinEasing = CubicBezierEasing(0.05f, 0.72f, 0.15f, 1f)

/** With system animations off the spin collapses into a short transition to the result. */
private const val REDUCED_MOTION_SPIN_DURATION_MS = 300

/** Slower than this a swipe counts as an accidental nudge rather than a spin. */
const val WHEEL_FLING_THRESHOLD_DEG_PER_SEC = 220f

private val TAP_TURNS = 5..9

private const val MIN_FLING_TURNS = 3
private const val MAX_FLING_TURNS = 14

/**
 * How many turns the fling bought. The velocity is divided by a full turn, so 3600°/s gives ten
 * turns, and the cap keeps the wheel from turning into a windmill.
 */
private fun turnsForFling(velocityDegreesPerSecond: Float): Int =
    (kotlin.math.abs(velocityDegreesPerSecond) / WheelGeometry.FULL_TURN)
        .toInt()
        .coerceIn(MIN_FLING_TURNS, MAX_FLING_TURNS)

internal suspend fun WheelScreenController.spin(
    uiState: WheelUiState,
    settings: com.byteflipper.random.data.settings.Settings,
    viewModel: WheelViewModel,
    snackbarHostState: SnackbarHostState,
    view: android.view.View,
    hapticsManager: com.byteflipper.random.ui.components.HapticsManager?,
    allOptionsUsedText: String,
    minItemsText: String,
    resetActionText: String,
    animationsEnabled: Boolean,
    pendingRemovalIndex: Int,
    flingVelocityDegreesPerSecond: Float? = null
) {
    if (uiState.isSpinning) return

    if (uiState.items.size < WHEEL_MIN_ITEMS) {
        snackbarHostState.showSnackbar(minItemsText)
        return
    }

    // The round is over. The reset lives in the message itself, since the only other way out is
    // buried in the wheel settings.
    if (uiState.needsReset) {
        val result = snackbarHostState.showSnackbar(
            message = allOptionsUsedText,
            actionLabel = resetActionText,
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.onEvent(WheelUiEvent.Reset)
        }
        return
    }

    val velocity = flingVelocityDegreesPerSecond
    // The target is computed from the layout without the used sector, so its removal has to be
    // finished first or the wheel stops off target.
    settleBeforeSpin(pendingRemovalIndex)

    val spin = viewModel.spin(
        currentRotation = rotation,
        fullTurns = if (velocity != null) {
            turnsForFling(velocity)
        } else {
            TAP_TURNS.random()
        },
        // A counter-clockwise fling spins that way; only the path changes, not the winner.
        clockwise = velocity == null || velocity >= 0f
    ) ?: return

    view.playSoundEffect(SoundEffectConstants.CLICK)
    if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)

    try {
        animateSpin(
            targetRotation = spin.targetRotation,
            spec = tween(
                durationMillis = if (animationsEnabled) {
                    uiState.spinDuration
                } else {
                    REDUCED_MOTION_SPIN_DURATION_MS
                },
                easing = if (velocity != null) WheelFlingSpinEasing else WheelTapSpinEasing
            )
        )
    } catch (cancellation: CancellationException) {
        // Leaving the screen or a configuration change, or isSpinning would stay true forever.
        viewModel.onEvent(WheelUiEvent.CancelSpin(rotation))
        throw cancellation
    }

    // Keep the angle within a single turn: over many spins a float loses enough precision for the
    // sector math to start lying.
    normalizeRotation()

    viewModel.onEvent(
        WheelUiEvent.CommitSpin(
            winnerIndex = spin.winnerIndex,
            finalRotation = rotation
        )
    )

    if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)

    // Confetti is another 3.5 seconds of motion across the screen, unwanted with animations off.
    showConfetti = animationsEnabled
    view.context.findActivity()?.let { act -> viewModel.checkAd(act) }
}
