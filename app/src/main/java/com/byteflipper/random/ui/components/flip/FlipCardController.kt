package com.byteflipper.random.ui.components.flip

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun FlipCardControls(state: FlipCardState): FlipCardController {
    val scope = rememberCoroutineScope()
    DisposableEffect(state) {
        onDispose {
            state.closeJob?.cancel()
            state.closeJob = null
            state.spinJob?.cancel()
            state.spinJob = null
            state.isSpinning = false
            state.isClosing = false
        }
    }
    return remember(state) {
        FlipCardController(state, scope)
    }
}

class FlipCardController internal constructor(
    private val state: FlipCardState,
    private val scope: CoroutineScope
) {
    fun open() {
        state.isVisible = true
        scope.launch {
            state.scrimProgress.stop()
            state.scrimProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
    }

    fun startClose(anchorInRoot: Offset, onClosed: () -> Unit) {
        startCloseInternal(state, scope, anchorInRoot, onClosed)
    }

    fun spinAndReveal(
        effectiveDelayMs: Int,
        onReveal: (targetIsFront: Boolean) -> Unit,
        onSpinCompleted: (() -> Unit)? = null,
    ) {
        val inputDelayMs = effectiveDelayMs.coerceAtLeast(1)
        state.spinJob?.cancel()
        state.spinJob = scope.launch {
            state.isSpinning = true

            // Hide current text
            state.frontTextAlpha.animateTo(0f, tween(FlipCardDefaults.FlipHideTextMs))
            state.backTextAlpha.animateTo(0f, tween(FlipCardDefaults.FlipHideTextMs))

            // Rotation params based on delay
            val norm = (inputDelayMs - 1000).toFloat() / (60000 - 1000).toFloat()
            val weight = sqrt(1f - norm.coerceIn(0f, 1f))
            val rpsShort = FlipCardDefaults.RpsShort
            val rpsLong = FlipCardDefaults.RpsLong
            val rps = rpsLong + (rpsShort - rpsLong) * weight

            val totalRotations = max(1f, rps * (inputDelayMs / 1000f))
            val wholeRotations = floor(totalRotations)
            val finalRotationDelta = -(wholeRotations * 360f + 180f)

            state.cardRotation.stop()
            state.cardRotation.snapTo(state.lastStopAngle)
            val startAngle = state.lastStopAngle
            val targetAngle = startAngle + finalRotationDelta

            val targetNormalized = normalizeAngle(targetAngle)
            val targetIsFront = targetNormalized < 90f || targetNormalized > 270f

            // Jitter params
            val ampShort = FlipCardDefaults.AmpShort
            val ampLong = FlipCardDefaults.AmpLong
            val amp0 = ampLong + (ampShort - ampLong) * weight

            fun easeOutCubic(p: Float): Float {
                val om = 1f - p
                return 1f - om * om * om
            }

            var revealed = false
            val revealTime = min(400, (inputDelayMs * FlipCardDefaults.RevealFraction).toInt())

            val start = withFrameNanos { it }
            var now = start

            while (true) {
                now = withFrameNanos { it }
                val elapsedMs = ((now - start) / 1_000_000).toInt()
                val progress = (elapsedMs.toFloat() / inputDelayMs).coerceIn(0f, 1f)
                val eased = easeOutCubic(progress)

                val tSec = (now - start) / 1_000_000_000f
                val amp = amp0 * (1f - eased)
                val jitter = amp * (
                    0.5f * sin(2f * PI.toFloat() * 2.7f * tSec) +
                        0.3f * sin(2f * PI.toFloat() * 4.3f * tSec + 0.5f) +
                        0.2f * sin(2f * PI.toFloat() * 7.1f * tSec + 1.2f)
                    )

                val angle = startAngle + finalRotationDelta * eased + jitter
                state.cardRotation.snapTo(angle)

                if (!revealed && inputDelayMs - elapsedMs <= revealTime) {
                    onReveal(targetIsFront)
                    if (targetIsFront) {
                        scope.launch {
                            delay(FlipCardDefaults.RevealDelayMs.toLong())
                            state.frontTextAlpha.animateTo(1f, tween(300))
                        }
                    } else {
                        scope.launch {
                            delay(FlipCardDefaults.RevealDelayMs.toLong())
                            state.backTextAlpha.animateTo(1f, tween(300))
                        }
                    }
                    revealed = true
                }

                if (progress >= 1f) break
            }

            state.cardRotation.snapTo(targetAngle)
            state.lastStopAngle = targetAngle

            if (targetIsFront && state.frontTextAlpha.value < 1f) {
                state.frontTextAlpha.animateTo(1f, tween(200))
            } else if (!targetIsFront && state.backTextAlpha.value < 1f) {
                state.backTextAlpha.animateTo(1f, tween(200))
            }

            state.isSpinning = false
            onSpinCompleted?.invoke()
        }
    }

    fun isVisible(): Boolean = state.isVisible
    val scrimProgress get() = state.scrimProgress
}

internal fun normalizeAngle(angle: Float): Float {
    return ((angle % 360f) + 360f) % 360f
}

internal fun startCloseInternal(
    state: FlipCardState,
    scope: CoroutineScope,
    anchorInRoot: Offset,
    onClosed: () -> Unit
) {
    if (!state.isVisible || state.isClosing) return
    state.isClosing = true
    state.spinJob?.cancel()
    state.spinJob = null
    state.isSpinning = false

    state.closeJob?.cancel()
    state.closeJob = scope.launch {
        val dx = anchorInRoot.x - state.cardCenterInRoot.x
        val dy = anchorInRoot.y - state.cardCenterInRoot.y

        val animations = listOf(
            async { state.scrimProgress.animateTo(0f, tween(FlipCardDefaults.ScrimHideMs, easing = FastOutSlowInEasing)) },
            async { state.exitRotationZ.animateTo(FlipCardDefaults.ExitRotateZ, tween(450, easing = FastOutSlowInEasing)) },
            async { state.exitScale.animateTo(0.5f, spring(dampingRatio = FlipCardDefaults.ExitScaleDamping, stiffness = FlipCardDefaults.ExitScaleSpringStiffness)) },
            async { state.exitTx.animateTo(dx, spring(dampingRatio = FlipCardDefaults.ExitTransDamping, stiffness = FlipCardDefaults.ExitTransSpringStiffness)) },
            async { state.exitTy.animateTo(dy, spring(dampingRatio = FlipCardDefaults.ExitTransDamping, stiffness = FlipCardDefaults.ExitTransSpringStiffness)) },
            async { state.exitAlpha.animateTo(0f, tween(FlipCardDefaults.ExitAlphaMs, easing = FastOutSlowInEasing)) }
        )
        animations.joinAll()

        // Reset state
        state.isVisible = false
        state.isClosing = false
        state.lastStopAngle = 0f
        state.cardRotation.snapTo(0f)
        state.frontTextAlpha.snapTo(1f)
        state.backTextAlpha.snapTo(1f)

        onClosed()
    }
}


