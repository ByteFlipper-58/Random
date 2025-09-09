package com.byteflipper.random.ui.lot

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.lot.components.LotFab
import com.byteflipper.random.ui.lot.components.LotOverlay
import com.byteflipper.random.ui.theme.getRainbowColors
import com.byteflipper.random.ui.lot.components.LotFabMode as FabMode
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotScreen(onBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hapticsManager = LocalHapticsManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: LotViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Получение строк из ресурсов
    val minimum3Fields = stringResource(R.string.minimum_3_fields)
    val minimum1Marked = stringResource(R.string.minimum_1_marked)
    val markedMoreThanTotal = stringResource(R.string.marked_more_than_total)

    // Поля ввода
    val totalText = uiState.totalText
    val markedText = uiState.markedText
    val cards = uiState.cards
    val fabMode = uiState.fabMode

    // Анимация скрима поверх контента
    val scrimAlpha by animateFloatAsState(
        targetValue = if (uiState.isOverlayVisible) 1f else 0f,
        animationSpec = tween(250)
    )

    // Получить цвета радуги для текущей темы
    val rainbowColors = getRainbowColors()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LotUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(message = context.getString(effect.messageRes))
                is LotUiEffect.HapticPress -> hapticsManager?.performPress(effect.intensity)
            }
        }
    }

    LotScaffold(
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            LotFab(
                size = settings.fabSize,
                mode = fabMode,
                onClick = {
                    if (cards.isEmpty()) {
                        viewModel.onEvent(LotUiEvent.GenerateRequested(rainbowColors))
                    } else {
                        when (fabMode) {
                            FabMode.RevealAll -> viewModel.onEvent(LotUiEvent.RevealAll)
                            FabMode.Randomize -> viewModel.onEvent(LotUiEvent.Shuffle)
                        }
                    }
                }
            )
        }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .blur((8f * scrimAlpha).dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LotContent(
                    modifier = Modifier.fillMaxWidth(),
                    totalText = totalText,
                    markedText = markedText,
                    onTotalChange = { new -> viewModel.onEvent(LotUiEvent.TotalChanged(new)) },
                    onMarkedChange = { new -> viewModel.onEvent(LotUiEvent.MarkedChanged(new)) }
                )
            }

            // Оверлей: отображаем только когда isOverlayVisible = true
            if (uiState.isOverlayVisible) {
                LotOverlay(
                    cards = cards,
                    scrimAlpha = scrimAlpha,
                    onDismiss = {
                        viewModel.onEvent(LotUiEvent.OverlayDismissed)
                    },
                    onCardClick = { id -> viewModel.onEvent(LotUiEvent.CardClicked(id)) }
                )
            }
        }
    }
}

