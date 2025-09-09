package com.byteflipper.random.ui.coin

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.byteflipper.random.ui.components.LocalHapticsManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.domain.coin.CoinSide
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val hapticsManager = LocalHapticsManager.current
    val viewModel: CoinViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val coinSide by viewModel.currentSide.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val rotationXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) } // px
    val bgScaleAnim = remember { Animatable(1.18f) }
    var isAnimating by rememberSaveable { mutableStateOf(false) }
    val scrimAlpha = remember { Animatable(0f) }
    var isOverlayVisible by rememberSaveable { mutableStateOf(false) }

    suspend fun toss() {
        if (isAnimating) return
        isAnimating = true

        val target = viewModel.toss()

        fun isFront(angle: Float): Boolean {
            val a = ((angle % 360f) + 360f) % 360f
            return a <= 90f || a >= 270f
        }
        val wantFront = (target == CoinSide.TAILS)
        val startFront = isFront(rotationXAnim.value)

        var halfTurns = Random.nextInt(8, 16)
        val needOdd = (wantFront != startFront)
        val isOdd = (halfTurns % 2 == 1)
        if (needOdd && !isOdd) halfTurns += 1
        if (!needOdd && isOdd) halfTurns += 1

        val startRotation = rotationXAnim.value
        val endRotation = startRotation + halfTurns * 180f

        val totalMs = 1200
        val upMs = (totalMs * 0.5f).toInt()
        val downMs = totalMs - upMs
        val throwHeightPx = with(density) { 200.dp.toPx() }

        view.playSoundEffect(SoundEffectConstants.CLICK)
        if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)

        if (!uiState.isOverlayVisible) {
            viewModel.onEvent(CoinUiEvent.SetOverlayVisible(true))
            scrimAlpha.snapTo(0f)
            scrimAlpha.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
        }

        val rot = scope.launch {
            rotationXAnim.animateTo(
                targetValue = endRotation,
                animationSpec = tween(durationMillis = totalMs, easing = FastOutSlowInEasing)
            )
        }
        val move = scope.launch {
            offsetYAnim.animateTo(
                targetValue = -throwHeightPx,
                animationSpec = tween(durationMillis = upMs, easing = FastOutSlowInEasing)
            )
            offsetYAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = downMs, easing = FastOutSlowInEasing)
            )
        }

        val bg = scope.launch {
            val near = 1.18f
            val far = 1.04f
            bgScaleAnim.animateTo(
                targetValue = far,
                animationSpec = tween(durationMillis = upMs, easing = FastOutSlowInEasing)
            )
            bgScaleAnim.animateTo(
                targetValue = near,
                animationSpec = tween(durationMillis = downMs, easing = FastOutSlowInEasing)
            )
        }

        rot.join()
        move.join()
        bg.join()
        scrimAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        viewModel.onEvent(CoinUiEvent.SetOverlayVisible(false))
        isAnimating = false
    }

    CoinScaffold(
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .pointerInput(Unit) {
                    var totalDy = 0f
                    var minDy = 0f
                    var triggered = false
                    val threshold = with(density) { 16.dp.toPx() }
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            totalDy += dragAmount
                            if (totalDy < minDy) minDy = totalDy
                            if (!triggered && !isAnimating && (minDy < -threshold || dragAmount < -threshold * 0.6f)) {
                                triggered = true
                                scope.launch { toss() }
                            }
                        },
                        onDragEnd = {
                            totalDy = 0f
                            minDy = 0f
                            triggered = false
                        },
                        onDragCancel = {
                            totalDy = 0f
                            minDy = 0f
                            triggered = false
                        }
                    )
                }
        ) {
            Box(modifier = Modifier.matchParentSize().padding(inner)) {
                CoinContent(
                    modifier = Modifier.matchParentSize(),
                    coinSide = coinSide,
                    rotationX = rotationXAnim.value,
                    offsetY = offsetYAnim.value,
                    bgScale = bgScaleAnim.value,
                    showResult = !isAnimating,
                    onDragThrow = { scope.launch { toss() } }
                )
            }
        }
    }
}


