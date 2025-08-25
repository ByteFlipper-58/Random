package com.byteflipper.random.ui.numbers

import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val MIN_DELAY_MS = 1_000
private const val MAX_DELAY_MS = 60_000
private const val DEFAULT_DELAY_MS = 3_000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbersScreen(onBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    var fromText by rememberSaveable { mutableStateOf("1") }
    var toText by rememberSaveable { mutableStateOf("10") }
    var delayText by rememberSaveable { mutableStateOf("") }
    var countText by rememberSaveable { mutableStateOf("1") }

    // Новые параметры
    var allowRepetitions by rememberSaveable { mutableStateOf(true) }
    var useDelay by rememberSaveable { mutableStateOf(true) }

    // Хранение использованных чисел
    var usedNumbers by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    // BottomSheet состояние
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    // Значения на сторонах карточки - теперь списки
    var frontValues by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var backValues by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }

    // Прозрачность текста для плавного появления
    val frontTextAlpha = remember { Animatable(1f) }
    val backTextAlpha = remember { Animatable(1f) }

    // Видимость/состояния
    var isCardVisible by rememberSaveable { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }
    var isSpinning by remember { mutableStateOf(false) }

    // Поворот карточки (по Y)
    val cardRotation = remember { Animatable(0f) }
    var lastStopAngle by rememberSaveable { mutableStateOf(0f) }

    // Позиции FAB и карточки
    var fabCenterInRoot by remember { mutableStateOf(Offset.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }
    var overlayTopLeftInRoot by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var cardCenterInRoot by remember { mutableStateOf(Offset.Zero) }

    // Scrim/blur прогресс
    val scrimProgress = remember { Animatable(0f) }

    // Параметры закрытия
    val exitAlpha = remember { Animatable(1f) }
    val exitRotationZ = remember { Animatable(0f) }
    val exitTx = remember { Animatable(0f) }
    val exitTy = remember { Animatable(0f) }
    val exitScale = remember { Animatable(1f) }

    // Пульс FAB
    val fabPulseProgress = remember { Animatable(0f) }
    val fabScale = remember { Animatable(1f) }

    var closeJob: Job? by remember { mutableStateOf(null) }
    var spinJob: Job? by remember { mutableStateOf(null) }

    // Цвета
    val primaryColor = MaterialTheme.colorScheme.primary
    val scrimSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)

    fun parseIntOrNull(text: String): Int? = text.trim().toIntOrNull()

    fun validateInputs(): Pair<IntRange, Int>? {
        val from = parseIntOrNull(fromText)
        val to = parseIntOrNull(toText)
        val count = parseIntOrNull(countText) ?: 1

        if (from == null || to == null) {
            scope.launch {
                snackbarHostState.showSnackbar("Введите корректные числа в поля 'ОТ' и 'ДО'")
            }
            return null
        }

        if (count < 1) {
            scope.launch {
                snackbarHostState.showSnackbar("Количество должно быть больше 0")
            }
            return null
        }

        val range = if (from <= to) from..to else to..from
        val rangeSize = range.last - range.first + 1

        if (!allowRepetitions) {
            // Проверяем доступные числа
            val availableNumbers = range.toSet() - usedNumbers
            if (availableNumbers.size < count) {
                if (availableNumbers.isEmpty()) {
                    // Все числа использованы
                    showResetDialog = true
                    return null
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Доступно только ${availableNumbers.size} неиспользованных чисел")
                    }
                    return null
                }
            }
        }

        return Pair(range, count)
    }

    fun generateNumbers(range: IntRange, count: Int): List<Int> {
        return if (allowRepetitions) {
            List(count) { range.random() }
        } else {
            // Исключаем использованные числа
            val availableNumbers = range.toSet() - usedNumbers
            if (availableNumbers.size >= count) {
                val selected = availableNumbers.shuffled().take(count)
                // Добавляем выбранные числа к использованным
                usedNumbers = usedNumbers + selected
                selected
            } else {
                // Не должно происходить, так как мы проверяем в validateInputs
                emptyList()
            }
        }
    }

    fun resetUsedNumbers() {
        usedNumbers = emptySet()
        showResetDialog = false
        scope.launch {
            snackbarHostState.showSnackbar("История использованных чисел очищена")
        }
    }

    // Сброс использованных чисел при изменении диапазона или режима повторений
    LaunchedEffect(fromText, toText, allowRepetitions) {
        if (allowRepetitions) {
            usedNumbers = emptySet()
        } else {
            // При изменении диапазона очищаем только числа вне нового диапазона
            val from = parseIntOrNull(fromText)
            val to = parseIntOrNull(toText)
            if (from != null && to != null) {
                val range = if (from <= to) from..to else to..from
                usedNumbers = usedNumbers.filter { it in range }.toSet()
            }
        }
    }

    fun resetExitTransforms() = scope.launch {
        exitAlpha.snapTo(1f)
        exitRotationZ.snapTo(0f)
        exitTx.snapTo(0f)
        exitTy.snapTo(0f)
        exitScale.snapTo(1f)
    }

    fun triggerFabPulse() = scope.launch {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        view.playSoundEffect(SoundEffectConstants.CLICK)

        fabPulseProgress.snapTo(0f)
        val ring = launch {
            fabPulseProgress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            fabPulseProgress.snapTo(0f)
        }
        val scale = launch {
            fabScale.animateTo(1.12f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
            fabScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
        }
        ring.join()
        scale.join()
    }

    fun openOverlay() = scope.launch {
        isCardVisible = true
        scrimProgress.stop()
        scrimProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
    }

    // Улучшенная функция вращения
    fun spinAndReveal(range: IntRange, count: Int, inputDelayMs: Int) {
        val actualDelayMs = if (useDelay) {
            inputDelayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
        } else {
            1000 // 1 секунда если задержка выключена
        }

        spinJob?.cancel()
        spinJob = scope.launch {
            isSpinning = true

            // Скрываем текущий текст
            frontTextAlpha.animateTo(0f, tween(100))
            backTextAlpha.animateTo(0f, tween(100))

            // Расчет параметров вращения
            val norm = (actualDelayMs - MIN_DELAY_MS).toFloat() / (MAX_DELAY_MS - MIN_DELAY_MS).toFloat()
            val weight = sqrt(1f - norm)
            val rpsShort = 2.5f
            val rpsLong = 0.2f
            val rps = rpsLong + (rpsShort - rpsLong) * weight

            val totalRotations = max(1f, rps * (actualDelayMs / 1000f))
            val wholeRotations = floor(totalRotations)
            val finalRotationDelta = -(wholeRotations * 360f + 180f) // против часовой

            cardRotation.stop()
            cardRotation.snapTo(lastStopAngle)
            val startAngle = lastStopAngle
            val targetAngle = startAngle + finalRotationDelta

            // Определяем какая сторона будет видна в конце
            fun normalizeAngle(angle: Float): Float {
                val normalized = ((angle % 360f) + 360f) % 360f
                return normalized
            }

            val targetNormalized = normalizeAngle(targetAngle)
            val targetIsFront = targetNormalized < 90f || targetNormalized > 270f

            // Генерируем новые числа
            val newNumbers = generateNumbers(range, count)

            // Параметры джиттера
            val ampShort = 15f
            val ampLong = 5f
            val amp0 = ampLong + (ampShort - ampLong) * weight

            // Функция плавного замедления
            fun easeOutCubic(p: Float): Float {
                val om = 1f - p
                return 1f - om * om * om
            }

            var revealed = false
            val revealTime = min(400, actualDelayMs / 3) // Когда показывать результат

            val start = withFrameNanos { it }
            var now = start

            while (true) {
                now = withFrameNanos { it }
                val elapsedMs = ((now - start) / 1_000_000).toInt()
                val progress = (elapsedMs.toFloat() / actualDelayMs).coerceIn(0f, 1f)
                val eased = easeOutCubic(progress)

                // Джиттер с затуханием
                val tSec = (now - start) / 1_000_000_000f
                val amp = amp0 * (1f - eased)
                val jitter = amp * (
                        0.5f * sin(2f * PI.toFloat() * 2.7f * tSec) +
                                0.3f * sin(2f * PI.toFloat() * 4.3f * tSec + 0.5f) +
                                0.2f * sin(2f * PI.toFloat() * 7.1f * tSec + 1.2f)
                        )

                val angle = startAngle + finalRotationDelta * eased + jitter
                cardRotation.snapTo(angle)

                // Показываем результат ближе к концу
                if (!revealed && actualDelayMs - elapsedMs <= revealTime) {
                    if (targetIsFront) {
                        frontValues = newNumbers
                        scope.launch {
                            delay(100) // Небольшая задержка для естественности
                            frontTextAlpha.animateTo(1f, tween(300))
                        }
                    } else {
                        backValues = newNumbers
                        scope.launch {
                            delay(100)
                            backTextAlpha.animateTo(1f, tween(300))
                        }
                    }
                    revealed = true
                }

                if (progress >= 1f) break
            }

            // Финальная позиция
            cardRotation.snapTo(targetAngle)
            lastStopAngle = targetAngle

            // Убеждаемся что текст видим
            if (targetIsFront && frontTextAlpha.value < 1f) {
                frontTextAlpha.animateTo(1f, tween(200))
            } else if (!targetIsFront && backTextAlpha.value < 1f) {
                backTextAlpha.animateTo(1f, tween(200))
            }

            isSpinning = false

            // Тактильная обратная связь
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun startClose() {
        if (!isCardVisible || isClosing) return
        isClosing = true
        spinJob?.cancel()
        spinJob = null
        isSpinning = false

        closeJob?.cancel()
        closeJob = scope.launch {
            val dx = fabCenterInRoot.x - cardCenterInRoot.x
            val dy = fabCenterInRoot.y - cardCenterInRoot.y

            val animations = listOf(
                async { scrimProgress.animateTo(0f, tween(350, easing = FastOutSlowInEasing)) },
                async { exitRotationZ.animateTo(-360f, tween(450, easing = FastOutSlowInEasing)) },
                async { exitScale.animateTo(0.5f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) },
                async { exitTx.animateTo(dx, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)) },
                async { exitTy.animateTo(dy, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)) },
                async { exitAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
            )

            animations.joinAll()
            triggerFabPulse()

            // Сброс состояния
            isCardVisible = false
            isClosing = false
            lastStopAngle = 0f
            cardRotation.snapTo(0f)
            frontValues = emptyList()
            backValues = emptyList()
            frontTextAlpha.snapTo(1f)
            backTextAlpha.snapTo(1f)
            resetExitTransforms()
        }
    }

    BackHandler(enabled = isCardVisible && !isClosing) { startClose() }

    // Диалог сброса использованных чисел
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Все числа использованы") },
            text = {
                Text("Все числа из диапазона ${fromText}–${toText} были использованы. Хотите сбросить историю и начать заново?")
            },
            confirmButton = {
                TextButton(
                    onClick = { resetUsedNumbers() }
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // BottomSheet с настройками
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Настройки генерации",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Количество результатов
                Column {
                    Text(
                        "Количество результатов",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = countText,
                        onValueChange = { newValue ->
                            countText = newValue.filter { ch -> ch.isDigit() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (countText.isEmpty()) {
                                        Text(
                                            text = "1",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )
                }

                // Повторения
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Разрешить повторения",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!allowRepetitions && usedNumbers.isNotEmpty()) {
                                val from = parseIntOrNull(fromText)
                                val to = parseIntOrNull(toText)
                                if (from != null && to != null) {
                                    val range = if (from <= to) from..to else to..from
                                    val totalCount = range.count()
                                    val usedCount = usedNumbers.count { it in range }
                                    Text(
                                        "Использовано: $usedCount из $totalCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                        Switch(
                            checked = allowRepetitions,
                            onCheckedChange = { allowRepetitions = it }
                        )
                    }
                }

                // Кнопка сброса истории
                if (!allowRepetitions && usedNumbers.isNotEmpty()) {
                    TextButton(
                        onClick = { resetUsedNumbers() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сбросить историю использованных чисел")
                    }
                }

                // Задержка
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Использовать задержку",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = useDelay,
                                onCheckedChange = { useDelay = it }
                            )
                        }

                        if (useDelay) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Задержка (мс)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            BasicTextField(
                                value = delayText,
                                onValueChange = { newValue ->
                                    delayText = newValue.filter { ch -> ch.isDigit() }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (delayText.isEmpty()) {
                                            Text(
                                                text = DEFAULT_DELAY_MS.toString(),
                                                style = MaterialTheme.typography.headlineSmall.copy(
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            Text(
                                "1000–60000 мс",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                "Фиксированная задержка: 1 секунда",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Число") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // FAB для настроек
                SmallFloatingActionButton(
                    onClick = { showBottomSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Настройки")
                }

                // Основная FAB для генерации
                Box(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        fabSize = coords.size
                        val pos = coords.positionInRoot()
                        fabCenterInRoot = Offset(pos.x + fabSize.width / 2f, pos.y + fabSize.height / 2f)
                    }
                ) {
                    // Пульс-эффект
                    if (fabPulseProgress.value > 0f && fabSize.width > 0) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(fabPulseProgress.value)
                        ) {
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val baseR = min(size.width, size.height) / 2f
                            val t = fabPulseProgress.value
                            val r = baseR + baseR * 1.5f * t

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.4f * (1f - t)),
                                        primaryColor.copy(alpha = 0.2f * (1f - t)),
                                        primaryColor.copy(alpha = 0f)
                                    ),
                                    center = c,
                                    radius = max(1f, r)
                                ),
                                center = c,
                                radius = r
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            if (isClosing) return@FloatingActionButton
                            val result = validateInputs() ?: return@FloatingActionButton
                            val (range, count) = result
                            val delayParsed = if (useDelay) {
                                parseIntOrNull(delayText) ?: DEFAULT_DELAY_MS
                            } else {
                                1000
                            }
                            val delayMs = delayParsed.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)

                            if (!isCardVisible) {
                                openOverlay()
                            }
                            spinAndReveal(range, count, delayMs)
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.graphicsLayer {
                            scaleX = fabScale.value
                            scaleY = fabScale.value
                        }
                    ) {
                        Icon(Icons.Outlined.Autorenew, contentDescription = "Сгенерировать")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Основной контент с blur
            val blurRadius = (8f * scrimProgress.value).dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .blur(blurRadius)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ОТ",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = fromText,
                    onValueChange = { newValue ->
                        fromText = newValue.filter { ch -> ch.isDigit() || ch == '-' }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 64.sp
                    )
                )

                Spacer(Modifier.height(48.dp))

                Text(
                    "ДО",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = toText,
                    onValueChange = { newValue ->
                        toText = newValue.filter { ch -> ch.isDigit() || ch == '-' }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 64.sp
                    )
                )
            }

            // Оверлей с карточкой (остальной код без изменений)
            if (isCardVisible || scrimProgress.value > 0.01f) {
                val overlayClickInteraction = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            overlayTopLeftInRoot = coords.positionInRoot()
                            overlaySize = coords.size
                        }
                        .clickable(
                            interactionSource = overlayClickInteraction,
                            indication = null,
                            enabled = isCardVisible && !isClosing && !isSpinning
                        ) { startClose() }
                ) {
                    // Scrim
                    if (overlaySize.width > 0 && overlaySize.height > 0) {
                        val maxRadius = hypot(overlaySize.width.toFloat(), overlaySize.height.toFloat())
                        val fabCenterLocal = fabCenterInRoot - overlayTopLeftInRoot
                        val radius = max(1f, scrimProgress.value * maxRadius * 1.2f)
                        val alpha = 0.85f * scrimProgress.value

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
                }

                // Определяем видимую сторону
                val currentRotation = cardRotation.value
                val normalizedRotation = ((currentRotation % 360f) + 360f) % 360f
                val showFront = normalizedRotation < 90f || normalizedRotation > 270f

                // Эффект масштабирования при переворотах
                val flipProgress = (normalizedRotation % 180f) / 180f
                val scaleEffect = 1f + 0.08f * sin(flipProgress * PI.toFloat())

                // Карточка
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .align(Alignment.Center)
                        .onGloballyPositioned { coords ->
                            val bounds = coords.boundsInRoot()
                            cardCenterInRoot = bounds.center
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = false
                        ) { /* блокируем клики */ }
                        .graphicsLayer {
                            rotationY = currentRotation
                            rotationZ = exitRotationZ.value
                            val totalScale = scaleEffect * exitScale.value
                            scaleX = totalScale
                            scaleY = totalScale
                            translationX = exitTx.value
                            translationY = exitTy.value
                            cameraDistance = 12f * density
                            alpha = exitAlpha.value
                        }
                ) {
                    if (showFront) {
                        // Лицевая сторона
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 8.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (frontValues.isNotEmpty()) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .alpha(frontTextAlpha.value)
                                    ) {
                                        if (frontValues.size == 1) {
                                            Text(
                                                text = frontValues[0].toString(),
                                                style = MaterialTheme.typography.displayLarge.copy(
                                                    fontSize = 56.sp
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        } else {
                                            Text(
                                                text = "Результаты:",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            frontValues.chunked(5).forEach { rowNumbers ->
                                                Row(
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    rowNumbers.forEach { number ->
                                                        Text(
                                                            text = number.toString(),
                                                            style = MaterialTheme.typography.headlineMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Обратная сторона (перевёрнута на 180°)
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationY = 180f
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 8.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (backValues.isNotEmpty()) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .alpha(backTextAlpha.value)
                                    ) {
                                        if (backValues.size == 1) {
                                            Text(
                                                text = backValues[0].toString(),
                                                style = MaterialTheme.typography.displayLarge.copy(
                                                    fontSize = 56.sp
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        } else {
                                            Text(
                                                text = "Результаты:",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            backValues.chunked(5).forEach { rowNumbers ->
                                                Row(
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    rowNumbers.forEach { number ->
                                                        Text(
                                                            text = number.toString(),
                                                            style = MaterialTheme.typography.headlineMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Сброс трансформаций при открытии
    LaunchedEffect(isCardVisible) {
        if (isCardVisible) {
            resetExitTransforms()
        }
    }
}