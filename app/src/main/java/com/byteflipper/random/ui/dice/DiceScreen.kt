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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random
import androidx.compose.ui.platform.LocalContext
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.ui.components.SizedFab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current

    val maxDice = 10
    var diceCount by rememberSaveable { mutableStateOf(2) } // 1..10
    var diceValues by rememberSaveable { mutableStateOf(listOf(1, 2)) }

    // Анимации для 4 кубиков (используем первые diceCount)
    val rotations = remember { List(maxDice) { Animatable(0f) } }
    val scales = remember { List(maxDice) { Animatable(1f) } }

    // Оверлей
    val scrimAlpha = remember { Animatable(0f) }
    var overlayVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(diceCount) {
        // Подгоняем список значений под новое количество кубиков
        if (diceValues.size != diceCount) {
            val base = if (diceValues.isEmpty()) emptyList() else diceValues.take(diceCount)
            diceValues = buildList(diceCount) { addAll(base); repeat(diceCount - base.size) { add(Random.nextInt(1, 7)) } }
        }
    }

    suspend fun openOverlayIfNeeded() {
        if (!overlayVisible) {
            overlayVisible = true
            scrimAlpha.snapTo(0f)
            scrimAlpha.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
        }
    }

    fun closeOverlay() {
        scope.launch {
            scrimAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            overlayVisible = false
        }
    }

    fun rollAll() {
        scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            view.playSoundEffect(SoundEffectConstants.CLICK)

            openOverlayIfNeeded()

            val newValues = List(diceCount) { Random.nextInt(1, 7) }
            // Запускаем анимации параллельно
            val jobs = mutableListOf<kotlinx.coroutines.Job>()
            repeat(diceCount) { i ->
                diceValues = diceValues.toMutableList().also { list -> list[i] = newValues[i] }
                jobs += launch {
                    rotations[i].animateTo(
                        targetValue = rotations[i].value + 360f * Random.nextInt(2, 4),
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                    )
                }
                jobs += launch {
                    scales[i].animateTo(1.12f, tween(150, easing = FastOutSlowInEasing))
                    scales[i].animateTo(1f, tween(250, easing = FastOutSlowInEasing))
                }
            }
            jobs.forEach { it.join() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Игральные кости") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад") }
                }
            )
        },
        floatingActionButton = {
            val context = LocalContext.current
            val settingsRepo = remember { SettingsRepository.fromContext(context) }
            val settings: Settings by settingsRepo.settingsFlow.collectAsState(initial = Settings())
            SizedFab(
                size = settings.fabSize,
                onClick = { rollAll() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Outlined.Autorenew, contentDescription = "Бросить")
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .blur((8f * scrimAlpha.value).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Количество кубиков",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val firstRow = 1..min(5, maxDice)
                val secondRow: IntRange? = if (maxDice > 5) 6..maxDice else null
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    firstRow.forEach { n ->
                        val selected = n == diceCount
                        SmallFloatingActionButton(
                            onClick = { diceCount = n },
                            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(text = n.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (secondRow != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        secondRow.forEach { n ->
                            val selected = n == diceCount
                            SmallFloatingActionButton(
                                onClick = { diceCount = n },
                                containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(text = n.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Оверлей поверх экрана с кубиками
        if (overlayVisible) {
            BackHandler { closeOverlay() }

            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * scrimAlpha.value))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    closeOverlay()
                }
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val columns = when {
                        diceCount <= 1 -> 1
                        else -> kotlin.math.ceil(kotlin.math.sqrt(diceCount.toDouble())).toInt().coerceIn(2, 4)
                    }
                    val rows = kotlin.math.ceil(diceCount / columns.toFloat()).toInt()
                    val spacing = 16.dp
                    val widthCandidate = (maxWidth - spacing * (columns - 1)) / columns
                    val heightCandidate = (maxHeight - spacing * (rows - 1)) / rows
                    val dieSize = min(widthCandidate.value, heightCandidate.value).dp.coerceIn(84.dp, 200.dp)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var index = 0
                        repeat(rows) { rowIdx ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(spacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(columns) {
                                    if (index < diceCount) {
                                        val i = index
                                        Box(
                                            modifier = Modifier
                                                .size(dieSize)
                                                .graphicsLayer {
                                                    rotationZ = rotations[i].value
                                                    scaleX = scales[i].value
                                                    scaleY = scales[i].value
                                                }
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    scope.launch {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val newV = Random.nextInt(1, 7)
                                                        diceValues = diceValues.toMutableList().also { it[i] = newV }
                                                        rotations[i].animateTo(
                                                            targetValue = rotations[i].value + 360f * Random.nextInt(1, 3),
                                                            animationSpec = tween(420, easing = FastOutSlowInEasing)
                                                        )
                                                        scales[i].animateTo(1.1f, tween(120))
                                                        scales[i].animateTo(1f, tween(180))
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            DieFace(value = diceValues[i])
                                        }
                                        index++
                                    }
                                }
                            }
                            if (rowIdx < rows - 1) Spacer(Modifier.height(spacing))
                        }
                        Spacer(Modifier.height(24.dp))
                        val total = diceValues.take(diceCount).sum()
                        Text(
                            text = "Сумма: $total",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DieFace(value: Int) {
    // Плоский корпус без градиента; объём за счёт фасок, внутренней тени и объёмных пипсов
    val faceColor = Color.White
    val edge = Color(0xFFBDBDBD)
    val pipColor = Color(0xFF1C1C1C)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val s = min(w, h)
        val corner = s * 0.14f

        // Плоский корпус
        drawRoundRect(
            color = faceColor,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = Fill
        )
        // Внешняя обводка
        drawRoundRect(
            color = edge.copy(alpha = 0.7f),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Внутренний микро-скос (две полуплоские линии вместо градиента)
        val inset = s * 0.045f
        val strokeW = s * 0.018f
        val leftX = inset
        val rightX = w - inset
        val topY = inset
        val bottomY = h - inset
        // Блики сверху и слева
        drawLine(color = Color.White.copy(alpha = 0.35f), start = androidx.compose.ui.geometry.Offset(leftX + corner * 0.4f, topY), end = androidx.compose.ui.geometry.Offset(rightX - corner * 0.4f, topY), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = Color.White.copy(alpha = 0.28f), start = androidx.compose.ui.geometry.Offset(leftX, topY + corner * 0.4f), end = androidx.compose.ui.geometry.Offset(leftX, bottomY - corner * 0.4f), strokeWidth = strokeW, cap = StrokeCap.Round)
        // Тени снизу и справа
        drawLine(color = Color.Black.copy(alpha = 0.12f), start = androidx.compose.ui.geometry.Offset(leftX + corner * 0.4f, bottomY), end = androidx.compose.ui.geometry.Offset(rightX - corner * 0.4f, bottomY), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = Color.Black.copy(alpha = 0.10f), start = androidx.compose.ui.geometry.Offset(rightX, topY + corner * 0.4f), end = androidx.compose.ui.geometry.Offset(rightX, bottomY - corner * 0.4f), strokeWidth = strokeW, cap = StrokeCap.Round)

        // Мягкая внутренняя тень (винигрет) без градиента корпуса
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.06f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(w * 0.46f, h * 0.36f),
                radius = s
            ),
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(w - inset * 2, h - inset * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner * 0.9f, corner * 0.9f)
        )

        // Раскладка точек
        val margin = s * 0.18f
        val cx = w / 2f
        val cy = h / 2f
        val left = margin
        val right = w - margin
        val top = margin
        val bottom = h - margin
        val midX = cx
        val midY = cy
        val pipR = s * 0.065f
        val shadowOffset = pipR * 0.14f

        fun dot(x: Float, y: Float) {
            // Мягкая падающая тень пипсы
            drawCircle(
                color = Color.Black.copy(alpha = 0.12f),
                radius = pipR,
                center = androidx.compose.ui.geometry.Offset(x + shadowOffset, y + shadowOffset)
            )
            // Объёмная пипса с лёгким смещённым центром освещения
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        pipColor,
                        pipColor.copy(alpha = 0.92f),
                        Color(0xFF2B2B2B)
                    ),
                    center = androidx.compose.ui.geometry.Offset(x - pipR * 0.28f, y - pipR * 0.28f),
                    radius = pipR * 1.2f
                ),
                radius = pipR,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }

        when (value.coerceIn(1, 6)) {
            1 -> dot(midX, midY)
            2 -> { dot(left, top); dot(right, bottom) }
            3 -> { dot(left, top); dot(midX, midY); dot(right, bottom) }
            4 -> { dot(left, top); dot(right, top); dot(left, bottom); dot(right, bottom) }
            5 -> { dot(left, top); dot(right, top); dot(midX, midY); dot(left, bottom); dot(right, bottom) }
            6 -> { dot(left, top); dot(left, midY); dot(left, bottom); dot(right, top); dot(right, midY); dot(right, bottom) }
        }
    }
}


