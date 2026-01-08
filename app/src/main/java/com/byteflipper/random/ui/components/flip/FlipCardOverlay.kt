package com.byteflipper.random.ui.components.flip

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.max
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun FlipCardOverlay(
    state: FlipCardState,
    anchorInRoot: Offset,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    cardSize: Dp = 280.dp,
    cardHeight: Dp? = null,
    frontContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    backContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    onLongPress: (() -> Unit)? = null,
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

    BackHandler(enabled = state.isVisible && !state.isClosing) {
        startCloseInternal(state, scope, anchorInRoot, onClosed)
    }

    if (state.isVisible || state.scrimProgress.value > 0.01f) {
        val overlayClickInteraction = androidx.compose.runtime.remember { MutableInteractionSource() }
        val scrimSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 1. Scrim Visuals
            FlipCardScrim(state = state, anchorInRoot = anchorInRoot, scrimSurfaceColor = scrimSurfaceColor)

            // 2. Scrim Touch Handler (Invisible layer catching taps outside card)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.isVisible, state.isClosing, state.isSpinning) {
                         detectTapOutside(
                             onTapOutside = {
                                 if (state.isVisible && !state.isClosing && !state.isSpinning) {
                                     startCloseInternal(state, scope, anchorInRoot, onClosed)
                                 }
                             },
                             shouldIgnore = { offset ->
                                 // Ignore touches inside card bounds.
                                 // Also ignore if bounds are not yet established (empty).
                                 // If empty, we assume Card is not ready, so we don't consume Down?
                                 // Or if empty, we assume Card is full screen? No.
                                 // Safest: If empty, card is not visible?
                                 // If !isEmpty and contains -> Ignore.
                                 !state.cardBoundsInRoot.isEmpty && state.cardBoundsInRoot.contains(offset)
                             }
                         )
                    }
            )

            // 3. Card Content (Sits on top, blocks touches to Scrim Handler)
            FlipCardContent(
                state = state,
                cardSize = cardSize,
                cardHeight = cardHeight,
                frontContainerColor = frontContainerColor,
                backContainerColor = backContainerColor,
                onLongPress = onLongPress,
                frontContent = frontContent,
                backContent = backContent
            )
        }

        LaunchedEffect(state.isVisible) {
            if (state.isVisible) {
                resetExitTransforms()
            }
        }
    }
}

@Composable
private fun FlipCardScrim(state: FlipCardState, anchorInRoot: Offset, scrimSurfaceColor: Color) {
    if (state.overlaySize.width > 0 && state.overlaySize.height > 0) {
        val maxRadius = hypot(
            state.overlaySize.width.toFloat(),
            state.overlaySize.height.toFloat()
        )
        val fabCenterLocal = anchorInRoot - state.overlayTopLeftInRoot
        val radius = max(1f, state.scrimProgress.value * maxRadius * 1.2f)
        val alpha = 0.85f * state.scrimProgress.value

        Canvas(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
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
}

@Composable
private fun FlipCardContent(
    state: FlipCardState,
    cardSize: Dp,
    cardHeight: Dp?,
    frontContainerColor: Color,
    backContainerColor: Color,
    onLongPress: (() -> Unit)? = null,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit
) {
    val currentRotation = state.cardRotation.value
    val normalizedRotation = normalizeAngle(currentRotation)
    val showFront = normalizedRotation < 90f || normalizedRotation > 270f

    val flipProgress = (normalizedRotation % 180f) / 180f
    val scaleEffect = 1f + 0.08f * sin(flipProgress * PI.toFloat())

    Box(
        modifier = Modifier
            .then(
                if (cardHeight != null) Modifier
                    .width(cardSize)
                    .height(cardHeight) else Modifier.size(cardSize)
            )
            .pointerInput(onLongPress) {
                detectTapGestures(
                    onLongPress = { onLongPress?.invoke() }
                )
            }
            .onGloballyPositioned { coords ->
                state.cardBoundsInRoot = coords.boundsInRoot()
                state.cardCenterInRoot = state.cardBoundsInRoot.center
            }
            .graphicsLayer {
                cameraDistance = FlipCardDefaults.CameraDistanceMultiplier * density

                rotationY = currentRotation
                rotationZ = state.exitRotationZ.value
                val totalScale = scaleEffect * state.exitScale.value
                scaleX = totalScale
                scaleY = totalScale
                translationX = state.exitTx.value
                translationY = state.exitTy.value
                alpha = state.exitAlpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = 180f }
                .alpha(if (showFront) 0f else 1f),
            colors = CardDefaults.cardColors(
                containerColor = backContainerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = FlipCardDefaults.CardElevation),
            shape = FlipCardDefaults.CardShape
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

        Card(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .alpha(if (showFront) 1f else 0f),
            colors = CardDefaults.cardColors(
                containerColor = frontContainerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = FlipCardDefaults.CardElevation),
            shape = FlipCardDefaults.CardShape
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .alpha(state.frontTextAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                frontContent()
            }
        }
    }
}

private suspend fun PointerInputScope.detectTapOutside(
    onTapOutside: () -> Unit,
    shouldIgnore: (androidx.compose.ui.geometry.Offset) -> Boolean
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        if (shouldIgnore(down.position)) {
            // Touch inside ignore area (Card). 
            // Do NOT consume. Allow pass-through.
            return@awaitEachGesture
        }
        
        // Touch outside (Scrim). Consume to handle potential tap.
        down.consume()
        
        val up = waitForUpOrCancellation()
        if (up != null) {
            up.consume()
            onTapOutside()
        }
    }
}


