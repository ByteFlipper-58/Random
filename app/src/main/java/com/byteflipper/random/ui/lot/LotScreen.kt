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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class FabMode { Randomize, RevealAll }

data class LotCard(val id: Int, val isMarked: Boolean, val isRevealed: Boolean)

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

    // Поля ввода
    var totalText by rememberSaveable { mutableStateOf("10") }
    var markedText by rememberSaveable { mutableStateOf("3") }

    // Состояние сетки карточек
    var cards by remember { mutableStateOf<List<LotCard>>(emptyList()) }

    // Состояния для FAB режима
    var fabMode by rememberSaveable { mutableStateOf(FabMode.Randomize) }

    // Анимация скрима поверх контента
    val scrimAlpha = remember { Animatable(0f) }

    fun parseIntSafe(text: String, minValue: Int, maxValue: Int): Int? {
        val v = text.trim().toIntOrNull() ?: return null
        return v.coerceIn(minValue, maxValue)
    }

    fun generateCards() {
        val total = parseIntSafe(totalText, 1, 500) ?: 0
        val marked = parseIntSafe(markedText, 0, total) ?: 0
        if (total < 3) {
            scope.launch { snackbarHostState.showSnackbar("Минимум 3 поля") }
            return
        }
        if (marked < 1) {
            scope.launch { snackbarHostState.showSnackbar("Минимум 1 отмеченное") }
            return
        }
        if (marked > total) {
            scope.launch { snackbarHostState.showSnackbar("Отмеченных больше, чем всего") }
            return
        }
        // Сформировать список отмеченных индексов
        val indices = (0 until total).toMutableList()
        indices.shuffle()
        val markedSet = indices.take(marked).toSet()
        cards = List(total) { i -> LotCard(id = i, isMarked = i in markedSet, isRevealed = false) }
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
        // Перетасовать порядок карточек и закрыть их
        val shuffled = cards.shuffled(Random)
        val hasMarked = shuffled.any { it.isMarked }
        cards = shuffled.map { it.copy(isRevealed = false) }
        fabMode = if (hasMarked) FabMode.RevealAll else FabMode.Randomize
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Жребий") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад") } },
                actions = {
                    IconButton(onClick = { hapticsEnabled = !hapticsEnabled }) {
                        val icon = if (hapticsEnabled) Icons.Outlined.Vibration else Icons.Outlined.Close //TODO Vibration off icon
                        Icon(icon, contentDescription = if (hapticsEnabled) "Вибрация: вкл" else "Вибрация: выкл")
                    }
                }
            )
        },
        floatingActionButton = {
            SizedFab(
                size = settings.fabSize,
                onClick = {
                    if (cards.isEmpty()) {
                        generateCards()
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
                    FabMode.RevealAll -> Icon(Icons.Outlined.Check, contentDescription = "Показать все")
                    FabMode.Randomize -> Icon(Icons.Outlined.Autorenew, contentDescription = "Перетасовать")
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
                        Text("Общее количество полей", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Количество отмеченных", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun LotGridCard(modifier: Modifier = Modifier, isRevealed: Boolean, isMarked: Boolean, onClick: () -> Unit) {
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
                    !isRevealed -> MaterialTheme.colorScheme.primaryContainer
                    isMarked -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.White
                }
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isRevealed) {
            Text("?", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            if (isMarked) {
                // Галочка
                Checkmark()
            } else {
                // Пусто
                Box(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun Checkmark() {
    // Простой нарисованный чек (правильной ориентации: \_/⟋)
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(28.dp).alpha(0.96f)) {
        val w = size.width
        val h = size.height
        // Зеркально по горизонтали: нижняя правая -> нижняя середина -> верхняя левая
        val p1 = androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.64f)
        val p2 = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.84f)
        val p3 = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.22f)
        drawLine(color = color, start = p1, end = p2, strokeWidth = 6f, cap = StrokeCap.Round)
        drawLine(color = color, start = p2, end = p3, strokeWidth = 6f, cap = StrokeCap.Round)
    }
}


