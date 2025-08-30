package com.byteflipper.random.ui.numbers

import android.view.SoundEffectConstants
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ceil
import kotlin.math.sqrt

import com.byteflipper.random.ui.components.FlipCardOverlay
import com.byteflipper.random.ui.components.rememberFlipCardState
import com.byteflipper.random.ui.components.FlipCardControls
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.components.SizedFab
import com.byteflipper.random.ui.theme.getRainbowColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository

private const val MIN_DELAY_MS = 1_000
private const val MAX_DELAY_MS = 60_000

// Функция для получения контрастного цвета текста на основе цвета фона
private fun getContrastColor(backgroundColor: Color): Color {
    // Вычисляем яркость цвета фона (формула luminance)
    val luminance = backgroundColor.luminance()

    // Если фон светлый (luminance > 0.5), используем черный текст
    // Если фон темный (luminance <= 0.5), используем белый текст
    return if (luminance > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}
private const val DEFAULT_DELAY_MS = 3_000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbersScreen(onBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current

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

    // Диалог настроек
    var showConfigDialog by rememberSaveable { mutableStateOf(false) }

    // Значения на сторонах карточки - теперь списки
    var frontValues by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var backValues by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }

    // Позиции FAB
    var fabCenterInRoot by remember { mutableStateOf(Offset.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }

    // Пульс FAB
    val fabPulseProgress = remember { Animatable(0f) }
    val fabScale = remember { Animatable(1f) }

    // Цвета
    val primaryColor = MaterialTheme.colorScheme.primary
    // Настройки приложения (для размера FAB)
    val settingsRepo = remember { SettingsRepository.fromContext(context) }
    val settings: Settings by settingsRepo.settingsFlow.collectAsState(initial = Settings())

    // Состояние и контроллер для переиспользуемой карточки
    val flipCardState = rememberFlipCardState()
    val flipCardController = FlipCardControls(flipCardState)

    fun parseIntOrNull(text: String): Int? = text.trim().toIntOrNull()

    fun resetUsedNumbers() {
        usedNumbers = emptySet()
        showResetDialog = false
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.history_cleared))
        }
    }

    fun validateInputs(): Pair<IntRange, Int>? {
        val from = parseIntOrNull(fromText)
        val to = parseIntOrNull(toText)
        val count = parseIntOrNull(countText) ?: 1

        if (from == null || to == null) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.enter_valid_numbers))
            }
            return null
        }

        if (count < 1) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.count_must_be_positive))
            }
            return null
        }

        val range = if (from <= to) from..to else to..from
        val rangeSize = range.last - range.first + 1

        if (!allowRepetitions) {
            val availableNumbers = range.toSet() - usedNumbers
            if (availableNumbers.size < count) {
                if (availableNumbers.isEmpty()) {
                    scope.launch {
                        val res = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.all_numbers_used),
                            actionLabel = context.getString(R.string.reset)
                        )
                        if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                            resetUsedNumbers()
                        }
                    }
                    return null
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.only_available_numbers, availableNumbers.size))
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

    

    // Диалог сброса заменён на snackbar с действием (см. validateInputs)

    // Диалог настроек
    GeneratorConfigDialog(
        visible = showConfigDialog,
        onDismissRequest = { showConfigDialog = false },
        countText = countText,
        onCountChange = { countText = it },
        allowRepetitions = allowRepetitions,
        onAllowRepetitionsChange = { allowRepetitions = it },
        usedNumbers = usedNumbers,
        availableRange = run {
                                val from = parseIntOrNull(fromText)
                                val to = parseIntOrNull(toText)
                                if (from != null && to != null) {
                if (from <= to) from..to else to..from
            } else null
        },
        onResetUsedNumbers = { resetUsedNumbers() },
        useDelay = useDelay,
        onUseDelayChange = { useDelay = it },
        delayText = delayText,
        onDelayChange = { delayText = it },
        minDelayMs = MIN_DELAY_MS,
        maxDelayMs = MAX_DELAY_MS,
        defaultDelayMs = DEFAULT_DELAY_MS
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.number)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // FAB для настроек
                SmallFloatingActionButton(
                    onClick = { showConfigDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(painter = painterResource(R.drawable.settings_24px), contentDescription = stringResource(R.string.settings))
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

                    SizedFab(
                        size = settings.fabSize,
                        onClick = {
                            val result = validateInputs() ?: return@SizedFab
                            val (range, count) = result
                            val delayParsed = if (useDelay) {
                                parseIntOrNull(delayText) ?: DEFAULT_DELAY_MS
                            } else {
                                1000
                            }
                            val delayMs = delayParsed.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)

                            if (!flipCardController.isVisible()) {
                                flipCardController.open()
                            }
                            flipCardController.spinAndReveal(
                                effectiveDelayMs = delayMs,
                                onReveal = { targetIsFront ->
                                    val newNumbers = generateNumbers(range, count)
                                    if (targetIsFront) {
                                        frontValues = newNumbers
                                    } else {
                                        backValues = newNumbers
                                    }
                                },
                                onSpinCompleted = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.graphicsLayer {
                            scaleX = fabScale.value
                            scaleY = fabScale.value
                        }
                    ) {
                        Icon(painterResource(R.drawable.autorenew_24px), contentDescription = stringResource(R.string.generate))
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
            // Основной контент с blur (зависит от прогресса скрима карточки)
            val blurRadius = (8f * flipCardController.scrimProgress.value).dp
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
                    stringResource(R.string.from),
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
                    stringResource(R.string.to),
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

            // Переиспользуемый оверлей с карточкой
            val resultsCountForSizing = max(frontValues.size, backValues.size)
            val configuration = LocalConfiguration.current
            val maxCardSideDp = (min(configuration.screenWidthDp, configuration.screenHeightDp) - 64).coerceAtLeast(200).dp

            fun computeCardSize(count: Int): Dp {
                val base = 280
                val scale = when {
                    count <= 10 -> 1.0
                    count <= 25 -> 1.15
                    count <= 50 -> 1.3
                    else -> 1.5
                }
                val target = (base * scale).toInt()
                val clamped = target.coerceIn(240, maxCardSideDp.value.toInt())
                return clamped.dp
            }

            fun columnsFor(count: Int): Int {
                if (count <= 1) return 1
                val approx = ceil(sqrt(count.toDouble())).toInt()
                return approx.coerceIn(3, 10)
            }

            val dynamicCardSize = computeCardSize(resultsCountForSizing)

            fun numberFontSizeFor(count: Int, cardSize: Dp): TextUnit {
                // Адаптивный размер текста в зависимости от размера карточки и количества чисел
                val baseSize = when {
                    count <= 5 -> cardSize.value * 0.08f  // Для малого количества - крупный текст
                    count <= 10 -> cardSize.value * 0.06f
                    count <= 25 -> cardSize.value * 0.05f
                    count <= 50 -> cardSize.value * 0.04f
                    else -> cardSize.value * 0.035f
                }
                return baseSize.coerceIn(18f, 36f).sp
            }

            // Получить цвета радуги и выбрать случайный для карточки
            val rainbowColors = getRainbowColors()
            val cardColor = remember(frontValues) { rainbowColors.random() }

            FlipCardOverlay(
                state = flipCardState,
                anchorInRoot = fabCenterInRoot,
                onClosed = {
                    // Пульс по закрытию + очистка локального результата
                    triggerFabPulse()
                    frontValues = emptyList()
                    backValues = emptyList()
                },
                cardSize = dynamicCardSize,
                // Используем один и тот же цвет для обеих сторон карточки
                frontContainerColor = cardColor,
                backContainerColor = cardColor,
                frontContent = {
                    if (frontValues.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding((dynamicCardSize.value * 0.03f).coerceIn(8f, 24f).dp)
                        ) {
                            if (frontValues.size == 1) {
                                // Адаптивный размер шрифта для одиночного числа - увеличен для лучшей видимости
                                val singleNumberFontSize = (dynamicCardSize.value * 0.4f).coerceIn(56f, 140f).sp
                                // Адаптируем цвет текста под цвет фона карточки
                                val textColor = getContrastColor(cardColor)
                                Text(
                                    text = frontValues[0].toString(),
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = singleNumberFontSize),
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            } else {
                                val cols = columnsFor(frontValues.size)
                                val numberSize = numberFontSizeFor(frontValues.size, dynamicCardSize)
                                // Адаптивный размер заголовка
                                val headerFontSize = (dynamicCardSize.value * 0.04f).coerceIn(16f, 28f).sp
                                // Адаптируем цвет текста под цвет фона карточки
                                val headerTextColor = getContrastColor(cardColor).copy(alpha = 0.8f)
                                Text(
                                    text = stringResource(R.string.results),
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = headerFontSize),
                                    color = headerTextColor
                                )
                                Spacer(Modifier.height(6.dp))
                                frontValues.chunked(cols).forEach { rowNumbers ->
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowNumbers.forEach { number ->
                                            // Адаптируем цвет текста под цвет фона карточки
                                            val numberTextColor = getContrastColor(cardColor)
                                            Text(
                                                text = number.toString(),
                                                fontSize = numberSize,
                                                fontWeight = FontWeight.Bold,
                                                color = numberTextColor,
                                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                backContent = {
                    if (backValues.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding((dynamicCardSize.value * 0.03f).coerceIn(8f, 24f).dp)
                        ) {
                            if (backValues.size == 1) {
                                // Адаптивный размер шрифта для одиночного числа - увеличен для лучшей видимости
                                val singleNumberFontSize = (dynamicCardSize.value * 0.4f).coerceIn(56f, 140f).sp
                                // Адаптируем цвет текста под цвет фона карточки
                                val textColor = getContrastColor(cardColor)
                                Text(
                                    text = backValues[0].toString(),
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = singleNumberFontSize),
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            } else {
                                val cols = columnsFor(backValues.size)
                                val numberSize = numberFontSizeFor(backValues.size, dynamicCardSize)
                                // Адаптивный размер заголовка
                                val headerFontSize = (dynamicCardSize.value * 0.04f).coerceIn(16f, 28f).sp
                                // Адаптируем цвет текста под цвет фона карточки
                                val headerTextColor = getContrastColor(cardColor).copy(alpha = 0.8f)
                                Text(
                                    text = stringResource(R.string.results),
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = headerFontSize),
                                    color = headerTextColor
                                )
                                Spacer(Modifier.height(6.dp))
                                backValues.chunked(cols).forEach { rowNumbers ->
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowNumbers.forEach { number ->
                                            // Адаптируем цвет текста под цвет фона карточки
                                            val numberTextColor = getContrastColor(cardColor)
                                            Text(
                                                text = number.toString(),
                                                fontSize = numberSize,
                                                fontWeight = FontWeight.Bold,
                                                color = numberTextColor,
                                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}