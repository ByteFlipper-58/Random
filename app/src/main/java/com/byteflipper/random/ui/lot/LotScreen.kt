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
import com.byteflipper.random.ui.theme.getRainbowColors
import com.byteflipper.random.ui.lot.LotFabMode as FabMode
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
        viewModel.events.collect { event ->
            when (event) {
                is LotEvent.ShowSnackbar -> snackbarHostState.showSnackbar(message = context.getString(event.messageRes))
                is LotEvent.HapticPress -> hapticsManager?.performPress(event.intensity)
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
                        viewModel.process(LotAction.GenerateRequested(rainbowColors))
                    } else {
                        when (fabMode) {
                            FabMode.RevealAll -> viewModel.process(LotAction.RevealAll)
                            FabMode.Randomize -> viewModel.process(LotAction.Shuffle)
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
                    onTotalChange = { new -> viewModel.updateTotalText(new) },
                    onMarkedChange = { new -> viewModel.updateMarkedText(new) }
                )
            }

            // Оверлей: отображаем только когда isOverlayVisible = true
            if (uiState.isOverlayVisible) {
                LotOverlay(
                    cards = cards,
                    scrimAlpha = scrimAlpha,
                    onDismiss = {
                    viewModel.process(LotAction.OverlayDismissed)
                    },
                    onCardClick = { id -> viewModel.process(LotAction.CardClicked(id)) }
                )
            }
        }
    }
}

