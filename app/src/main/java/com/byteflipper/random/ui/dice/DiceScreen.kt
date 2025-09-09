package com.byteflipper.random.ui.dice

import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.byteflipper.random.ui.components.LocalHapticsManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.ui.components.SizedFab
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import kotlinx.coroutines.Job
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.material3.FloatingActionButton
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val hapticsManager = LocalHapticsManager.current
    val view = LocalView.current
    val viewModel: DiceViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val maxDice = 10
    var diceCount by rememberSaveable { mutableStateOf(uiState.diceCount) }

    val rotations = remember { List(maxDice) { Animatable(0f) } }
    val scales = remember { List(maxDice) { Animatable(1f) } }
    val isAnimating = remember { mutableStateOf(List(maxDice) { false }) }

    val diceColorPalette = remember {
        listOf(
            Color(0xFFE74C3C), Color(0xFF3498DB), Color(0xFF2ECC71), Color(0xFFF39C12),
            Color(0xFF9B59B6), Color(0xFF1ABC9C), Color(0xFFE67E22), Color(0xFF34495E),
            Color(0xFF16A085), Color(0xFF27AE60), Color(0xFF2980B9), Color(0xFF8E44AD),
            Color(0xFFC0392B), Color(0xFFD35400), Color(0xFF7F8C8D), Color(0xFF2C3E50)
        )
    }

    var diceColors by remember { mutableStateOf(List(maxDice) { diceColorPalette.random() }) }

    val animatedColors = diceColors.mapIndexed { index, color ->
        animateColorAsState(
            targetValue = color,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "dice_color_${index}"
        )
    }

    var isRolling by remember { mutableStateOf(false) }
    val scrimAlpha = remember { Animatable(0f) }
    var currentRollJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(uiState.diceCount) { diceCount = uiState.diceCount }
    LaunchedEffect(diceCount) { viewModel.setDiceCount(diceCount) }

    suspend fun openOverlayIfNeeded() {
        if (!uiState.isOverlayVisible) {
            viewModel.setOverlayVisible(true)
            scrimAlpha.snapTo(0f)
            scrimAlpha.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
        }
    }

    fun closeOverlay() {
        scope.launch {
            scrimAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            viewModel.setOverlayVisible(false)
        }
    }

    fun rollAll(hapticsAllowed: Boolean) {
        currentRollJob?.cancel()
        currentRollJob = scope.launch {
            isRolling = true
            if (hapticsAllowed) hapticsManager?.performPress(settings.hapticsIntensity)
            view.playSoundEffect(SoundEffectConstants.CLICK)
            openOverlayIfNeeded()

            val newValues = viewModel.rollAll()

            diceColors = List(maxDice) { index ->
                val currentColor = diceColors[index]
                var newColor = diceColorPalette.random()
                while (newColor == currentColor && diceColorPalette.size > 1) {
                    newColor = diceColorPalette.random()
                }
                newColor
            }

            val jobs = mutableListOf<Job>()
            repeat(diceCount) { i ->

                val currentRotation = rotations[i].value
                val normalizedRotation = ((currentRotation % 360) / 90).toInt() * 90f
                rotations[i].snapTo(normalizedRotation)

                jobs += launch {
                    val fullRotations = Random.nextInt(3, 6) * 360f
                    val finalRotation = fullRotations + 90f * Random.nextInt(0, 4)
                    rotations[i].animateTo(
                        targetValue = normalizedRotation + finalRotation,
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                    )
                }
                jobs += launch {
                    scales[i].animateTo(1.15f, tween(150, easing = FastOutSlowInEasing))
                    scales[i].animateTo(1f, tween(250, easing = FastOutSlowInEasing))
                }
            }
            jobs.forEach { it.join() }
            isRolling = false
        }
    }

    DiceScaffold(
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            DiceFabControls(
                size = settings.fabSize,
                isRolling = isRolling,
                onClick = { rollAll(settings.hapticsEnabled) }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .blur((8f * scrimAlpha.value).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DiceContent(
                modifier = Modifier.fillMaxWidth(),
                diceCount = diceCount,
                onDiceCountChange = { diceCount = it }
            )
        }

        if (uiState.isOverlayVisible) {
            DiceOverlay(
                scrimAlpha = scrimAlpha.value,
                diceCount = diceCount,
                rotations = rotations,
                scales = scales,
                isAnimating = isAnimating.value,
                animatedColors = animatedColors,
                values = uiState.values,
                onDismiss = { closeOverlay() },
                onDieClick = { i ->
                    if (!isAnimating.value[i]) {
                        scope.launch {
                            isAnimating.value = isAnimating.value.toMutableList().also { it[i] = true }
                            if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
                            val newV = viewModel.rollOne(i)
                            val currentColor = diceColors[i]
                            var newColor = diceColorPalette.random()
                            while (newColor == currentColor && diceColorPalette.size > 1) {
                                newColor = diceColorPalette.random()
                            }
                            diceColors = diceColors.toMutableList().also { it[i] = newColor }

                            val currentRotation = rotations[i].value
                            val normalizedRotation = ((currentRotation % 360) / 90).toInt() * 90f
                            rotations[i].snapTo(normalizedRotation)

                            rotations[i].animateTo(
                                targetValue = normalizedRotation + 360f * Random.nextInt(2, 4),
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            )
                            scales[i].animateTo(1.12f, tween(120))
                            scales[i].animateTo(1f, tween(180))
                            isAnimating.value = isAnimating.value.toMutableList().also { it[i] = false }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DieFace(value: Int, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val s = min(w, h)
        val corner = s * 0.15f

        // Создаем более темный и светлый оттенки
        val darkColor = Color(
            red = (color.red * 0.8f).coerceIn(0f, 1f),
            green = (color.green * 0.8f).coerceIn(0f, 1f),
            blue = (color.blue * 0.8f).coerceIn(0f, 1f)
        )

        val lightColor = Color(
            red = (color.red * 1.2f).coerceIn(0f, 1f),
            green = (color.green * 1.2f).coerceIn(0f, 1f),
            blue = (color.blue * 1.2f).coerceIn(0f, 1f)
        )

        // Внешняя тень
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.3f),
                    Color.Black.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(w/2 + 6f, h/2 + 6f),
                radius = s * 0.7f
            ),
            topLeft = androidx.compose.ui.geometry.Offset(2f, 2f),
            size = androidx.compose.ui.geometry.Size(w + 4f, h + 4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner + 2f, corner + 2f),
            style = Fill
        )

        // Основа кубика с градиентом
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(lightColor, color, darkColor),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(w, h)
            ),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = Fill
        )

        // Внутренняя рамка для глубины
        drawRoundRect(
            color = darkColor.copy(alpha = 0.3f),
            topLeft = androidx.compose.ui.geometry.Offset(2f, 2f),
            size = androidx.compose.ui.geometry.Size(w - 4f, h - 4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner - 2f, corner - 2f),
            style = Stroke(width = 1.5f)
        )

        // Точки на кубике
        drawDots(value, s, w, h, lightColor)
    }
}

private fun DrawScope.drawDots(value: Int, s: Float, w: Float, h: Float, baseColor: Color) {
    val margin = s * 0.24f
    val cx = w / 2f
    val cy = h / 2f
    val left = margin
    val right = w - margin
    val top = margin
    val bottom = h - margin
    val pipR = s * 0.08f

    fun drawDot(x: Float, y: Float) {
        // Внешняя тень точки
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(x + 2f, y + 2f),
                radius = pipR * 1.2f
            ),
            radius = pipR * 1.2f,
            center = androidx.compose.ui.geometry.Offset(x + 2f, y + 2f)
        )

        // Впадина вокруг точки
        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = pipR * 1.1f,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )

        // Основная точка
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFAFAFA),
                    Color(0xFFE0E0E0),
                    Color(0xFFBDBDBD)
                ),
                center = androidx.compose.ui.geometry.Offset(x, y),
                radius = pipR
            ),
            radius = pipR,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )

        // Блик на точке
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(x - pipR * 0.3f, y - pipR * 0.3f),
                radius = pipR * 0.5f
            ),
            radius = pipR * 0.4f,
            center = androidx.compose.ui.geometry.Offset(x - pipR * 0.3f, y - pipR * 0.3f)
        )
    }

    when (value.coerceIn(1, 6)) {
        1 -> drawDot(cx, cy)
        2 -> {
            drawDot(left, top)
            drawDot(right, bottom)
        }
        3 -> {
            drawDot(left, top)
            drawDot(cx, cy)
            drawDot(right, bottom)
        }
        4 -> {
            drawDot(left, top)
            drawDot(right, top)
            drawDot(left, bottom)
            drawDot(right, bottom)
        }
        5 -> {
            drawDot(left, top)
            drawDot(right, top)
            drawDot(cx, cy)
            drawDot(left, bottom)
            drawDot(right, bottom)
        }
        6 -> {
            drawDot(left, top)
            drawDot(left, cy)
            drawDot(left, bottom)
            drawDot(right, top)
            drawDot(right, cy)
            drawDot(right, bottom)
        }
    }
}