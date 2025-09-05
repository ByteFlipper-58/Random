package com.byteflipper.random.ui.numbers

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
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

import com.byteflipper.random.ui.components.flip.FlipCardOverlay
import com.byteflipper.random.ui.components.flip.rememberFlipCardState
import com.byteflipper.random.ui.components.flip.FlipCardControls
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.numbers.components.NumbersResultsDisplay
import com.byteflipper.random.ui.numbers.components.NumbersFabControls
import com.byteflipper.random.ui.theme.getRainbowColors
import com.byteflipper.random.ui.components.RadioOption
import com.byteflipper.random.ui.numbers.NumberSortingMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    var sortingModeKey by rememberSaveable { mutableStateOf(NumberSortingMode.Random.name) }

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
    val viewModel: NumbersViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
            val usedInRangeCount = usedNumbers.count { it in range }
            val availableCount = rangeSize - usedInRangeCount
            if (availableCount < count) {
                if (availableCount <= 0) {
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
                        snackbarHostState.showSnackbar(context.getString(R.string.only_available_numbers, availableCount))
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
            val selected = mutableSetOf<Int>()
            val lower = range.first
            val upper = range.last
            var attempts = 0
            val maxAttempts = count * 50
            while (selected.size < count && attempts < maxAttempts) {
                val value = (lower..upper).random()
                if (value !in usedNumbers && value !in selected) {
                    selected += value
                }
                attempts++
            }
            if (selected.size < count) {
                var v = lower
                while (selected.size < count && v <= upper) {
                    if (v !in usedNumbers && v !in selected) selected += v
                    v++
                }
            }
            usedNumbers = usedNumbers + selected
            selected.toList()
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
        defaultDelayMs = DEFAULT_DELAY_MS,
        sortingOptions = listOf(
            RadioOption(key = NumberSortingMode.Random.name, title = stringResource(R.string.random_order)),
            RadioOption(key = NumberSortingMode.Ascending.name, title = stringResource(R.string.ascending)),
            RadioOption(key = NumberSortingMode.Descending.name, title = stringResource(R.string.descending))
        ),
        selectedSortingKey = sortingModeKey,
        onSortingChange = { sortingModeKey = it }
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
            NumbersFabControls(
                onConfigClick = { showConfigDialog = true },
                onGenerateClick = {
                    val result = validateInputs() ?: return@NumbersFabControls
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
                            val unsorted = generateNumbers(range, count)
                            val newNumbers = when (NumberSortingMode.valueOf(sortingModeKey)) {
                                NumberSortingMode.Random -> unsorted.shuffled()
                                NumberSortingMode.Ascending -> unsorted.sorted()
                                NumberSortingMode.Descending -> unsorted.sortedDescending()
                            }
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
                onFabPositioned = { center, size ->
                    fabCenterInRoot = center
                    fabSize = size
                }
            )
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
                Spacer(Modifier.height(48.dp))
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
                Spacer(Modifier.height(48.dp))
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
            // Динамическая высота карточки на основе количества результатов
            val heightScale = when {
                resultsCountForSizing <= 10 -> 1.0f
                resultsCountForSizing <= 25 -> 1.2f
                resultsCountForSizing <= 50 -> 1.4f
                resultsCountForSizing <= 100 -> 1.6f
                else -> 1.8f
            }
            val contentTargetHeight = (dynamicCardSize * heightScale).coerceIn(300.dp, maxCardSideDp)

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
                cardHeight = contentTargetHeight,
                // Используем один и тот же цвет для обеих сторон карточки
                frontContainerColor = cardColor,
                backContainerColor = cardColor,
                frontContent = {
                    NumbersResultsDisplay(
                        results = frontValues,
                        cardColor = cardColor,
                        cardSize = contentTargetHeight
                    )
                },
                backContent = {
                    NumbersResultsDisplay(
                        results = backValues,
                        cardColor = cardColor,
                        cardSize = contentTargetHeight
                    )
                }
            )
        }
    }
}