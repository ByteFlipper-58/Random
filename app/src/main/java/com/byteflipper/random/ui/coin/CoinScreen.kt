package com.byteflipper.random.ui.coin

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.byteflipper.random.ui.components.LocalHapticsManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import kotlin.math.abs
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
    val settings by viewModel.settings.collectAsState()
    val coinSide by viewModel.currentSide.collectAsState()

    val rotationXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) } // px
    val bgScaleAnim = remember { Animatable(1.18f) }
    var isAnimating by rememberSaveable { mutableStateOf(false) }

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

        var halfTurns = Random.nextInt(10, 18)
        val needOdd = (wantFront != startFront)
        val isOdd = (halfTurns % 2 == 1)
        if (needOdd && !isOdd) halfTurns += 1
        if (!needOdd && isOdd) halfTurns += 1

        val startRotation = rotationXAnim.value
        val endRotation = startRotation + halfTurns * 180f

        val totalMs = 1400
        val upMs = (totalMs * 0.45f).toInt()
        val downMs = totalMs - upMs
        val throwHeightPx = with(density) { 220.dp.toPx() }

        view.playSoundEffect(SoundEffectConstants.CLICK)
        if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)

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
        isAnimating = false
    }

    Scaffold(
        topBar = { CoinTopBar(onBack = onBack) },
        contentWindowInsets = WindowInsets.systemBars
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF4E342E))
        ) {
            Image(
                painter = painterResource(R.drawable.desk),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val parallax = (offsetYAnim.value * 0.06f).coerceIn(-24f, 24f)
                        translationY = parallax
                        scaleX = bgScaleAnim.value
                        scaleY = bgScaleAnim.value
                        alpha = 0.98f
                    }
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val coinSize = 236.dp
                val cameraDistancePx = with(density) { 96.dp.toPx() }
                val maxThrowPx = with(density) { 220.dp.toPx() }
                val fillScale = 1.12f

                Box(
                    modifier = Modifier
                        .size(coinSize)
                        .offset(y = 12.dp)
                        .pointerInput(Unit) {
                            var totalDy = 0f
                            var minDy = 0f
                            var hasLargeUpStep = false
                            val threshold = with(density) { 24.dp.toPx() }
                            detectVerticalDragGestures(
                                onVerticalDrag = { _, dragAmount ->
                                    totalDy += dragAmount
                                    if (totalDy < minDy) minDy = totalDy
                                    if (dragAmount < -threshold * 0.6f) hasLargeUpStep = true
                                },
                                onDragEnd = {
                                    if (minDy < -threshold || hasLargeUpStep) {
                                        scope.launch { toss() }
                                    }
                                    totalDy = 0f
                                    minDy = 0f
                                    hasLargeUpStep = false
                                },
                                onDragCancel = {
                                    totalDy = 0f
                                    minDy = 0f
                                    hasLargeUpStep = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                rotationX = rotationXAnim.value
                                translationY = offsetYAnim.value
                                cameraDistance = cameraDistancePx
                                shape = CircleShape
                                clip = true
                            }
                    ) {
                        val angle = ((rotationXAnim.value % 360f) + 360f) % 360f
                        val showFront = angle <= 90f || angle >= 270f

                        Image(
                            painter = painterResource(R.drawable.coin_front),
                            contentDescription = stringResource(R.string.tails),
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleX = fillScale
                                    scaleY = fillScale
                                    alpha = if (showFront) 1f else 0f
                                }
                        )

                        Image(
                            painter = painterResource(R.drawable.coin_back),
                            contentDescription = stringResource(R.string.heads),
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleX = fillScale
                                    scaleY = fillScale
                                    rotationX = 180f
                                    alpha = if (!showFront) 1f else 0f
                                }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (coinSide) {
                        CoinSide.HEADS -> "${stringResource(R.string.result)}: ${stringResource(R.string.heads)}"
                        CoinSide.TAILS -> "${stringResource(R.string.result)}: ${stringResource(R.string.tails)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.padding(4.dp))
                Text(
                    text = stringResource(R.string.swipe_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            }
        }
    }
}


