package com.byteflipper.random.ui.components.flip

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import kotlin.math.sin
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch

@Composable
fun FlipCardOverlay(
    state: FlipCardState,
    anchorInRoot: Offset,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    cardSize: Dp = 280.dp,
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

    BackHandler(enabled = state.isVisible && !state.isClosing) {
        startCloseInternal(state, scope, anchorInRoot, onClosed)
    }

    if (state.isVisible || state.scrimProgress.value > 0.01f) {
        val overlayClickInteraction = androidx.compose.runtime.remember { MutableInteractionSource() }
        val scrimSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)

        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = overlayClickInteraction,
                    indication = null,
                    enabled = state.isVisible && !state.isClosing && !state.isSpinning
                ) {
                    startCloseInternal(state, scope, anchorInRoot, onClosed)
                },
            contentAlignment = Alignment.Center
        ) {
            FlipCardScrim(state = state, anchorInRoot = anchorInRoot, scrimSurfaceColor = scrimSurfaceColor)
            FlipCardContent(
                state = state,
                cardSize = cardSize,
                frontContainerColor = frontContainerColor,
                backContainerColor = backContainerColor,
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
    frontContainerColor: Color,
    backContainerColor: Color,
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
            .size(cardSize)
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInRoot()
                state.cardCenterInRoot = bounds.center
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


