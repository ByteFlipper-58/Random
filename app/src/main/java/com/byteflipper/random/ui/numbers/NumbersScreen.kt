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
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.data.settings.HapticsIntensity
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
import com.byteflipper.random.domain.numbers.SortingMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.byteflipper.random.utils.Constants.DEFAULT_DELAY_MS
import com.byteflipper.random.utils.Constants.MIN_DELAY_MS
import com.byteflipper.random.utils.Constants.MAX_DELAY_MS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbersScreen(onBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val hapticsManager = LocalHapticsManager.current
    val view = LocalView.current
    val context = LocalContext.current

    // Все пользовательские параметры и результаты берём из VM
    val viewModel: NumbersViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Позиции FAB
    var fabCenterInRoot by remember { mutableStateOf(Offset.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }

    // Пульс FAB
    val fabPulseProgress = remember { Animatable(0f) }
    val fabScale = remember { Animatable(1f) }

    // Состояние и контроллер для переиспользуемой карточки
    val flipCardState = rememberFlipCardState()
    val flipCardController = FlipCardControls(flipCardState)

    fun resetUsedNumbers() {
        viewModel.resetUsedNumbers()
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.history_cleared))
        }
    }

    fun validateInputs(): Pair<IntRange, Int>? {
        val validation = viewModel.validateInputs()
        if (validation == null) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.enter_valid_numbers))
            }
        }
        return validation
    }

    // Сброс usedNumbers на изменения диапазона/режима
    LaunchedEffect(uiState.fromText, uiState.toText, uiState.allowRepetitions) {
        if (uiState.allowRepetitions) {
            viewModel.resetUsedNumbers()
        } else {
            val from = uiState.fromText.trim().toIntOrNull()
            val to = uiState.toText.trim().toIntOrNull()
            if (from != null && to != null) {
                val range = if (from <= to) from..to else to..from
                viewModel.pruneUsedNumbersToRange(range)
            }
        }
    }

    fun triggerFabPulse() = scope.launch {
        if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
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

    // Диалог настроек
    GeneratorConfigDialog(
        visible = uiState.showConfigDialog,
        onDismissRequest = { viewModel.setConfigDialogVisible(false) },
        countText = uiState.countText,
        onCountChange = { viewModel.updateCountText(it) },
        allowRepetitions = uiState.allowRepetitions,
        onAllowRepetitionsChange = { viewModel.updateAllowRepetitions(it) },
        usedNumbers = uiState.usedNumbers,
        availableRange = run {
            val from = uiState.fromText.trim().toIntOrNull()
            val to = uiState.toText.trim().toIntOrNull()
            if (from != null && to != null) {
                if (from <= to) from..to else to..from
            } else null
        },
        onResetUsedNumbers = { resetUsedNumbers() },
        useDelay = uiState.useDelay,
        onUseDelayChange = { viewModel.updateUseDelay(it) },
        delayText = uiState.delayText,
        onDelayChange = { viewModel.updateDelayText(it) },
        minDelayMs = MIN_DELAY_MS,
        maxDelayMs = MAX_DELAY_MS,
        defaultDelayMs = DEFAULT_DELAY_MS,
        sortingOptions = listOf(
            RadioOption(key = SortingMode.Random.name, title = stringResource(R.string.random_order)),
            RadioOption(key = SortingMode.Ascending.name, title = stringResource(R.string.ascending)),
            RadioOption(key = SortingMode.Descending.name, title = stringResource(R.string.descending))
        ),
        selectedSortingKey = uiState.sortingMode.name,
        onSortingChange = { key -> viewModel.updateSortingMode(SortingMode.valueOf(key)) }
    )

    NumbersScaffold(
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            NumbersFabControls(
                onConfigClick = { viewModel.setConfigDialogVisible(true) },
                onGenerateClick = {
                    val result = validateInputs() ?: return@NumbersFabControls
                    val delayMs = viewModel.getEffectiveDelayMs()
                    if (!flipCardController.isVisible()) {
                        flipCardController.open()
                    }
                    flipCardController.spinAndReveal(
                        effectiveDelayMs = delayMs,
                        onReveal = { targetIsFront ->
                            val newNumbers = viewModel.generate()
                            if (targetIsFront) viewModel.setFrontValues(newNumbers) else viewModel.setBackValues(newNumbers)
                        },
                        onSpinCompleted = {
                            if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
                        }
                    )
                },
                onFabPositioned = { center, size ->
                    fabCenterInRoot = center
                    fabSize = size
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val blurRadius = (8f * flipCardController.scrimProgress.value).dp

            NumbersContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .blur(blurRadius),
                fromText = uiState.fromText,
                toText = uiState.toText,
                onFromChange = { viewModel.updateFromText(it) },
                onToChange = { viewModel.updateToText(it) }
            )

            val resultsCountForSizing = max(uiState.frontValues.size, uiState.backValues.size)
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

            val dynamicCardSize = computeCardSize(resultsCountForSizing)
            val heightScale = when {
                resultsCountForSizing <= 10 -> 1.0f
                resultsCountForSizing <= 25 -> 1.2f
                resultsCountForSizing <= 50 -> 1.4f
                resultsCountForSizing <= 100 -> 1.6f
                else -> 1.8f
            }
            val contentTargetHeight = (dynamicCardSize * heightScale).coerceIn(300.dp, maxCardSideDp)

            val rainbowColors = getRainbowColors()
            val cardColor = remember(uiState.frontValues) { rainbowColors.random() }

            FlipCardOverlay(
                state = flipCardState,
                anchorInRoot = fabCenterInRoot,
                onClosed = {
                    triggerFabPulse()
                    viewModel.clearResults()
                },
                cardSize = dynamicCardSize,
                cardHeight = contentTargetHeight,
                frontContainerColor = cardColor,
                backContainerColor = cardColor,
                frontContent = {
                    NumbersResultsDisplay(
                        results = uiState.frontValues,
                        cardColor = cardColor,
                        cardSize = contentTargetHeight
                    )
                },
                backContent = {
                    NumbersResultsDisplay(
                        results = uiState.backValues,
                        cardColor = cardColor,
                        cardSize = contentTargetHeight
                    )
                }
            )
        }
    }
}