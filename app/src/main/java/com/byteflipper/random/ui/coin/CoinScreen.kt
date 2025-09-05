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
import kotlin.random.Random

private enum class CoinSide { HEADS, TAILS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val viewModel: CoinViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsState()

    val rotationXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) } // px
    val bgScaleAnim = remember { Animatable(1.18f) }
    var isAnimating by rememberSaveable { mutableStateOf(false) }
    var currentSide by rememberSaveable { mutableStateOf(CoinSide.HEADS) }
    

    suspend fun toss() {
        if (isAnimating) return
        isAnimating = true

        // Случайный итог (орёл/решка)
        val target = if (Random.nextBoolean()) CoinSide.HEADS else CoinSide.TAILS

        // Нужная видимая сторона: front = Решка, back = Орёл
        fun isFront(angle: Float): Boolean {
            val a = ((angle % 360f) + 360f) % 360f
            return a <= 90f || a >= 270f
        }
        val wantFront = (target == CoinSide.TAILS)
        val startFront = isFront(rotationXAnim.value)

        // Кол-во полупереворотов (180°) для реалистичного вращения, корректируем чётность под нужную сторону
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
        if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)

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

        // Масштаб фона: отдалить при взлёте и вернуть при падении
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
        currentSide = target
        isAnimating = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.coin)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF4E342E))
        ) {
            // Фон с текстурой стола (parallax лёгкий)
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

                // Обёртка монеты: внешняя обводка, тень и сама монета
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
                    // Динамичная тень на «столе»
                    /*
                    val lift = (-offsetYAnim.value).coerceAtLeast(0f)
                    val t = (lift / maxThrowPx).coerceIn(0f, 1f)
                    val shadowScaleX = 1.2f - 0.3f * t
                    val shadowScaleY = 0.40f - 0.18f * t
                    val shadowAlpha = 0.40f * (1f - t) + 0.12f
                    Box(
                        modifier = Modifier
                            .size(coinSize)
                            .graphicsLayer {
                                scaleX = shadowScaleX
                                scaleY = shadowScaleY
                                alpha = shadowAlpha
                            }
                            .clip(CircleShape)
                            .background(Color.Black)
                            .blur(22.dp)
                    )
                    */

                    // Монета с 3D-вращением (без статического наклона)
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
                        // Определяем видимую сторону
                        val angle = ((rotationXAnim.value % 360f) + 360f) % 360f
                        val showFront = angle <= 90f || angle >= 270f

                        // FRONT: решка (coin_front)
                        Image(
                            painter = painterResource(R.drawable.coin_front),
                            contentDescription = stringResource(R.string.tails),
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    // равномерное заполнение, чтобы не было внутренней обводки
                                    scaleX = fillScale
                                    scaleY = fillScale
                                    alpha = if (showFront) 1f else 0f
                                }
                        )

                        // BACK: орёл (coin_back)
                        Image(
                            painter = painterResource(R.drawable.coin_back),
                            contentDescription = stringResource(R.string.heads),
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    // равномерное заполнение + переворот обратной стороны
                                    scaleX = fillScale
                                    scaleY = fillScale
                                    rotationX = 180f
                                    alpha = if (!showFront) 1f else 0f
                                }
                                // клип на слое контейнера, тут не нужен
                        )
                    }
                }
            }

            // Нижний блок: результат и подсказка у низа экрана
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (currentSide) {
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


