package com.byteflipper.random.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

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
    // All Animatables live in the state; no external scope is stored here
    return remember { FlipCardState() }
}

private fun normalizeAngle(angle: Float): Float {
    return ((angle % 360f) + 360f) % 360f
}

@Composable
fun FlipCardOverlay(
    state: FlipCardState,
    anchorInRoot: Offset,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    cardSize: Dp = 280.dp,
    // Colors are overridable to support reuse on other screens
    frontContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    backContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    fun resetExitTransforms() = scope.launch {
        state.exitAlpha.snapTo(1f)
        state.exitRotationZ.snapTo(0f)
        state.exitTx.snapTo(0f)
        state.exitTy.snapTo(0f)
        state.exitScale.snapTo(1f)
    }

    // Public helpers on state via lambdas to avoid leaking scope outside
    if (state is FlipCardState) {
        // Expose helper functions on the instance via extension-like closures
        // Open overlay
        if (false) { /* no-op placeholder to keep structured */ }
    }

    // Back press closes overlay
    BackHandler(enabled = state.isVisible && !state.isClosing) {
        startCloseInternal(state, scope, anchorInRoot, onClosed)
    }

    if (state.isVisible || state.scrimProgress.value > 0.01f) {
        val overlayClickInteraction = remember { MutableInteractionSource() }
        val scrimSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)

        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    state.overlayTopLeftInRoot = coords.positionInRoot()
                    state.overlaySize = coords.size
                }
                .clickable(
                    interactionSource = overlayClickInteraction,
                    indication = null,
                    enabled = state.isVisible && !state.isClosing && !state.isSpinning
                ) {
                    startCloseInternal(state, scope, anchorInRoot, onClosed)
                }
        ) {
            // Scrim
            if (state.overlaySize.width > 0 && state.overlaySize.height > 0) {
                val maxRadius = hypot(
                    state.overlaySize.width.toFloat(),
                    state.overlaySize.height.toFloat()
                )
                val fabCenterLocal = anchorInRoot - state.overlayTopLeftInRoot
                val radius = max(1f, state.scrimProgress.value * maxRadius * 1.2f)
                val alpha = 0.85f * state.scrimProgress.value

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                scrimSurfaceColor.copy(alpha = 0f),
                                scrimSurfaceColor.copy(alpha = alpha * 0.3f),
                                scrimSurfaceColor.copy(alpha = alpha * 0.6f),
                                scrimSurfaceColor.copy(alpha = alpha)
                            ),
                            center = fabCenterLocal,
                            radius = radius
                        ),
                        center = fabCenterLocal,
                        radius = radius
                    )
                }
            }

            // Determine side visibility from rotation
            val currentRotation = state.cardRotation.value
            val normalizedRotation = normalizeAngle(currentRotation)
            val showFront = normalizedRotation < 90f || normalizedRotation > 270f

            // Scale bump during flip
            val flipProgress = (normalizedRotation % 180f) / 180f
            val scaleEffect = 1f + 0.08f * sin(flipProgress * PI.toFloat())

            // Card
            Box(
                modifier = Modifier
                    .size(cardSize)
                    .align(Alignment.Center)
                    .onGloballyPositioned { coords ->
                        val bounds = coords.boundsInRoot()
                        state.cardCenterInRoot = bounds.center
                    }
                    .graphicsLayer {
                        // Ensure stable camera distance and high precision perspective
                        cameraDistance = 24f * density

                        rotationY = currentRotation
                        rotationZ = state.exitRotationZ.value
                        val totalScale = scaleEffect * state.exitScale.value
                        scaleX = totalScale
                        scaleY = totalScale
                        translationX = state.exitTx.value
                        translationY = state.exitTy.value
                        alpha = state.exitAlpha.value
                    }
            ) {
                // Back side (always composed)
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .alpha(if (showFront) 0f else 1f),
                    colors = CardDefaults.cardColors(
                        containerColor = backContainerColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(state.backTextAlpha.value),
                        contentAlignment = Alignment.Center
                    ) {
                        backContent()
                    }
                }

                // Front side (always composed)
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (showFront) 1f else 0f),
                    colors = CardDefaults.cardColors(
                        containerColor = frontContainerColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(state.frontTextAlpha.value),
                        contentAlignment = Alignment.Center
                    ) {
                        frontContent()
                    }
                }
            }
        }

        // Reset transforms when overlay becomes visible
        LaunchedEffect(state.isVisible) {
            if (state.isVisible) {
                resetExitTransforms()
            }
        }
    }
}

// Public API functions to control the state
@Composable
fun FlipCardControls(state: FlipCardState): FlipCardController {
    val scope = rememberCoroutineScope()
    return remember(state) {
        FlipCardController(state, scope)
    }
}

class FlipCardController internal constructor(
    private val state: FlipCardState,
    private val scope: kotlinx.coroutines.CoroutineScope
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
            state.frontTextAlpha.animateTo(0f, tween(100))
            state.backTextAlpha.animateTo(0f, tween(100))

            // Rotation params based on delay
            val norm = (inputDelayMs - 1000).toFloat() / (60000 - 1000).toFloat()
            val weight = kotlin.math.sqrt(1f - norm.coerceIn(0f, 1f))
            val rpsShort = 2.5f
            val rpsLong = 0.2f
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
            val ampShort = 15f
            val ampLong = 5f
            val amp0 = ampLong + (ampShort - ampLong) * weight

            fun easeOutCubic(p: Float): Float {
                val om = 1f - p
                return 1f - om * om * om
            }

            var revealed = false
            val revealTime = min(400, inputDelayMs / 3)

            val start = androidx.compose.runtime.withFrameNanos { it }
            var now = start

            while (true) {
                now = androidx.compose.runtime.withFrameNanos { it }
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
                            kotlinx.coroutines.delay(100)
                            state.frontTextAlpha.animateTo(1f, tween(300))
                        }
                    } else {
                        scope.launch {
                            kotlinx.coroutines.delay(100)
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

private fun startCloseInternal(
    state: FlipCardState,
    scope: kotlinx.coroutines.CoroutineScope,
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
            async { state.scrimProgress.animateTo(0f, tween(350, easing = FastOutSlowInEasing)) },
            async { state.exitRotationZ.animateTo(-360f, tween(450, easing = FastOutSlowInEasing)) },
            async { state.exitScale.animateTo(0.5f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) },
            async { state.exitTx.animateTo(dx, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)) },
            async { state.exitTy.animateTo(dy, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)) },
            async { state.exitAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
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


