package com.byteflipper.random.ui.lot

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.ui.components.SizedFab
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.roundToInt
import kotlin.random.Random
import com.byteflipper.random.ui.theme.getRainbowColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

private enum class FabMode { Randomize, RevealAll }

data class LotCard(val id: Int, val isMarked: Boolean, val isRevealed: Boolean, val color: Color)

private fun computeRowSizes(total: Int): List<Int> {
    if (total <= 0) return emptyList()
    val maxPerRow = 5
    if (total <= 3) return listOf(total)
    if (total == 4) return listOf(2, 2)
    if (total == 5) return listOf(3, 2)

    val minRows = (total + maxPerRow - 1) / maxPerRow
    val desiredRows = if (total in 10..15) 3 else sqrt(total.toDouble()).roundToInt().coerceAtLeast(2)
    var rowsCount = maxOf(minRows, desiredRows)

    fun distributeCenter(rowsCount: Int): List<Int> {
        val base = total / rowsCount
        var extra = total % rowsCount
        val rows = MutableList(rowsCount) { base }
        // Порядок распределения лишних элементов — от центра к краям
        val order = buildList {
            if (rowsCount % 2 == 1) {
                val mid = rowsCount / 2
                add(mid)
                for (d in 1..mid) {
                    add(mid - d)
                    add(mid + d)
                }
            } else {
                val leftMid = rowsCount / 2 - 1
                val rightMid = rowsCount / 2
                add(leftMid)
                add(rightMid)
                for (d in 1..leftMid) {
                    add(leftMid - d)
                    add(rightMid + d)
                }
            }
        }
        var guard = 0
        while (extra > 0 && guard < order.size * 2) {
            for (idx in order) {
                if (extra == 0) break
                if (rows[idx] < maxPerRow) {
                    rows[idx] += 1
                    extra -= 1
                }
            }
            guard += 1
        }
        return rows
    }

    // Подбираем количество рядов так, чтобы не было рядов >5 после распределения
    while (true) {
        val rows = distributeCenter(rowsCount)
        if (rows.all { it <= maxPerRow }) return rows
        rowsCount += 1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotScreen(onBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var hapticsEnabled by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository.fromContext(context) }
    val settings: Settings by settingsRepo.settingsFlow.collectAsState(initial = Settings())

    // Получение строк из ресурсов
    val minimum3Fields = stringResource(R.string.minimum_3_fields)
    val minimum1Marked = stringResource(R.string.minimum_1_marked)
    val markedMoreThanTotal = stringResource(R.string.marked_more_than_total)

    // Поля ввода
    var totalText by rememberSaveable { mutableStateOf("10") }
    var markedText by rememberSaveable { mutableStateOf("3") }

    // Состояние сетки карточек
    var cards by remember { mutableStateOf<List<LotCard>>(emptyList()) }

    // Состояния для FAB режима
    var fabMode by rememberSaveable { mutableStateOf(FabMode.Randomize) }

    // Анимация скрима поверх контента
    val scrimAlpha = remember { Animatable(0f) }

    // Получить цвета радуги для текущей темы
    val rainbowColors = getRainbowColors()

    fun parseIntSafe(text: String, minValue: Int, maxValue: Int): Int? {
        val v = text.trim().toIntOrNull() ?: return null
        return v.coerceIn(minValue, maxValue)
    }

    fun generateCards(availableColors: List<androidx.compose.ui.graphics.Color>) {
        val total = parseIntSafe(totalText, 1, 500) ?: 0
        val marked = parseIntSafe(markedText, 0, total) ?: 0
        if (total < 3) {
            scope.launch { snackbarHostState.showSnackbar(minimum3Fields) }
            return
        }
        if (marked < 1) {
            scope.launch { snackbarHostState.showSnackbar(minimum1Marked) }
            return
        }
        if (marked > total) {
            scope.launch { snackbarHostState.showSnackbar(markedMoreThanTotal) }
            return
        }
        // Сформировать список отмеченных индексов
        val indices = (0 until total).toMutableList()
        indices.shuffle()
        val markedSet = indices.take(marked).toSet()

        // Создать список цветов с умным распределением, избегая повторений рядом
        val rows = computeRowSizes(total)
        val colors = distributeColorsSmartly(total, availableColors, rows)

        cards = List(total) { i -> LotCard(id = i, isMarked = i in markedSet, isRevealed = false, color = colors[i]) }
        fabMode = FabMode.RevealAll
        scope.launch { scrimAlpha.animateTo(1f, tween(250)) }
    }

    fun revealCard(id: Int) {
        val pos = cards.indexOfFirst { it.id == id }
        if (pos == -1) return
        if (cards[pos].isRevealed) return
        val wasMarked = cards[pos].isMarked
        cards = cards.toMutableList().also { it[pos] = it[pos].copy(isRevealed = true) }
        if (wasMarked && hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        // Если все отмеченные открыты — автоматически раскрыть остальные и переключить FAB на рандом
        val totalMarked = cards.count { it.isMarked }
        val openedMarked = cards.count { it.isMarked && it.isRevealed }
        if (totalMarked > 0 && openedMarked >= totalMarked) {
            // Раскрыть все неотмеченные
            cards = cards.map { c -> if (!c.isMarked) c.copy(isRevealed = true) else c }
            fabMode = FabMode.Randomize
        }
    }

    fun revealAll() {
        cards = cards.map { it.copy(isRevealed = true) }
        // После полного раскрытия меняем иконку на Randomize
        fabMode = FabMode.Randomize
    }

    fun reshuffleAndHide() {
        // Перетасовать порядок карточек и закрыть их, сохраняя цвета
        val shuffled = cards.shuffled(Random)
        val hasMarked = shuffled.any { it.isMarked }
        cards = shuffled.map { it.copy(isRevealed = false) }
        fabMode = if (hasMarked) FabMode.RevealAll else FabMode.Randomize
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lot_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) } },
                actions = {
                    IconButton(onClick = { hapticsEnabled = !hapticsEnabled }) {
                        val icon = if (hapticsEnabled) painterResource(id = R.drawable.mobile_vibrate_24px) else painterResource(id = R.drawable.mobile_vibrate_off_24px)
                        Icon(icon, contentDescription = if (hapticsEnabled) stringResource(R.string.vibration_on) else stringResource(R.string.vibration_off))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = {
            SizedFab(
                size = settings.fabSize,
                onClick = {
                    if (cards.isEmpty()) {
                        generateCards(rainbowColors)
                    } else {
                        when (fabMode) {
                            FabMode.RevealAll -> revealAll()
                            FabMode.Randomize -> reshuffleAndHide()
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                when (fabMode) {
                    FabMode.RevealAll -> Icon(painterResource(R.drawable.check_24px), contentDescription = stringResource(R.string.show_all))
                    FabMode.Randomize -> Icon(painterResource(R.drawable.autorenew_24px), contentDescription = stringResource(R.string.reshuffle))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .blur((8f * scrimAlpha.value).dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.total_fields_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = totalText,
                            onValueChange = { new -> totalText = new.filter { ch -> ch.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.displayLarge.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 40.sp
                            )
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.marked_fields_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = markedText,
                            onValueChange = { new ->
                                val filtered = new.filter { ch -> ch.isDigit() }
                                val t = filtered.toIntOrNull()
                                val total = totalText.toIntOrNull()
                                markedText = if (t != null && total != null) maxOf(1, min(t, total)).toString() else filtered
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.displayLarge.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 40.sp
                            )
                        )
                    }
                }
            }

            // Оверлей: скрим + сетка карточек поверх
            if (cards.isNotEmpty()) {
                // Back для закрытия
                BackHandler { 
                    scope.launch { scrimAlpha.animateTo(0f, tween(200)) }
                    cards = emptyList()
                    fabMode = FabMode.Randomize
                }

                // Скрим
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f * scrimAlpha.value))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            scope.launch { scrimAlpha.animateTo(0f, tween(200)) }
                            cards = emptyList()
                            fabMode = FabMode.Randomize
                        }
                )

                // Сетка карточек (динамичный размер и центрирование)
                androidx.compose.foundation.layout.BoxWithConstraints(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(16.dp)
                ) {
                    val spacing = 8.dp
                    val rows = computeRowSizes(cards.size)
                    val maxInRow = rows.maxOrNull() ?: 1
                    // Единый размер карточки по ширине/высоте, чтобы всё поместилось и было крупнее при малом количестве
                    val widthCandidate = (maxWidth - spacing * (maxInRow - 1)) / maxInRow
                    val heightCandidate = (maxHeight - spacing * (rows.size - 1)) / rows.size
                    val cardSize = kotlin.math.min(widthCandidate.value, heightCandidate.value).dp
                        .coerceIn(40.dp, 180.dp)

                    Column(
                        modifier = Modifier.matchParentSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var idx = 0
                        rows.forEach { countInRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                                    space = spacing,
                                    alignment = Alignment.CenterHorizontally
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(countInRow) {
                                    val card = cards[idx]
                                    LotGridCard(
                                        modifier = Modifier.size(cardSize),
                                        isRevealed = card.isRevealed,
                                        isMarked = card.isMarked,
                                        cardColor = card.color,
                                        cardSize = cardSize,
                                        onClick = { revealCard(card.id) }
                                    )
                                    idx++
                                }
                            }
                            Spacer(Modifier.height(spacing))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LotGridCard(modifier: Modifier = Modifier, isRevealed: Boolean, isMarked: Boolean, cardColor: Color, cardSize: Dp, onClick: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    val target = if (isRevealed) 180f else 0f
    LaunchedEffect(target) { rotation.animateTo(target, tween(250)) }

    val showFront = ((rotation.value % 360f) + 360f) % 360f <= 90f || ((rotation.value % 360f) + 360f) % 360f >= 270f
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 24f * density
            }
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    !isRevealed -> cardColor
                    isMarked -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.White
                }
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = (cardSize.value * 0.08f).coerceIn(12f, 32f).dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isRevealed) {
            // Адаптируем цвет текста под цвет фона для лучшей читаемости
            val textColor = getContrastColor(cardColor)
            // Размер шрифта адаптируется под размер карточки (увеличен для лучшей видимости)
            val fontSize = (cardSize.value * 0.4f).coerceIn(28f, 72f).sp
            Text("?", style = MaterialTheme.typography.titleLarge.copy(fontSize = fontSize, fontWeight = FontWeight.Bold), color = textColor)
        } else {
            if (isMarked) {
                // Галочка
                Checkmark(cardSize)
            } else {
                // Пусто - адаптивный размер, увеличен для лучшей видимости
                val emptySize = (cardSize.value * 0.08f).coerceIn(20f, 40f).dp
                Box(modifier = Modifier.size(emptySize))
            }
        }
    }
}

@Composable
private fun Checkmark(cardSize: Dp) {
    // Простой нарисованный чек (правильной ориентации: \_/⟋)
    val color = MaterialTheme.colorScheme.primary
    // Адаптивный размер галочки - увеличен для лучшей видимости
    val checkmarkSize = (cardSize.value * 0.12f).coerceIn(28f, 48f).dp
    val strokeWidth = (cardSize.value * 0.025f).coerceIn(5f, 10f)
    Canvas(modifier = Modifier.size(checkmarkSize).alpha(0.96f)) {
        val w = size.width
        val h = size.height
        // Зеркально по горизонтали: нижняя правая -> нижняя середина -> верхняя левая
        val p1 = androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.64f)
        val p2 = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.84f)
        val p3 = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.22f)
        drawLine(color = color, start = p1, end = p2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(color = color, start = p2, end = p3, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

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

// Функция для умного распределения цветов, избегающая повторений рядом
private fun distributeColorsSmartly(
    totalCards: Int,
    availableColors: List<Color>,
    rows: List<Int>
): List<Color> {
    if (availableColors.isEmpty()) return emptyList()

    val colors = mutableListOf<Color>()
    val usedColorsInCurrentRow = mutableSetOf<Color>()
    val usedColorsInPreviousRow = mutableSetOf<Color>()

    var cardIndex = 0
    var rowIndex = 0

    for (row in rows) {
        usedColorsInCurrentRow.clear()

        for (i in 0 until row) {
            if (cardIndex >= totalCards) break

            // Получаем доступные цвета, исключая уже использованные в этом и предыдущем ряду
            val forbiddenColors = usedColorsInCurrentRow + usedColorsInPreviousRow
            val availableForThisCard = availableColors.filter { it !in forbiddenColors }

            val selectedColor = if (availableForThisCard.isNotEmpty()) {
                // Выбираем случайный из доступных цветов
                availableForThisCard.random()
            } else {
                // Если доступных цветов нет, выбираем случайный из всех
                availableColors.random()
            }

            colors.add(selectedColor)
            usedColorsInCurrentRow.add(selectedColor)
            cardIndex++
        }

        // Обновляем цвета предыдущего ряда для следующей итерации
        usedColorsInPreviousRow.clear()
        usedColorsInPreviousRow.addAll(usedColorsInCurrentRow)
        rowIndex++
    }

    return colors
}
