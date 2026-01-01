package com.byteflipper.random.ui.wheel

import android.view.SoundEffectConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.RandomApplication
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.wheel.components.FortuneWheel
import com.byteflipper.random.ui.wheel.components.WheelEditorSheet
import com.byteflipper.random.ui.wheel.components.WheelFabControls
import com.byteflipper.random.ui.wheel.components.WheelSettingsSheet
import com.byteflipper.random.utils.findActivity
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val hapticsManager = LocalHapticsManager.current
    val viewModel: WheelViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val rotationAnim = remember { Animatable(0f) }

    // Calculate current sector based on rotation
    val visibleItems by remember(uiState.items, uiState.excludedIndices) {
        derivedStateOf {
            uiState.items.filterIndexed { index, _ -> index !in uiState.excludedIndices }
        }
    }
    
    val currentSectorText by remember(rotationAnim.value, visibleItems) {
        derivedStateOf {
            if (visibleItems.isEmpty()) return@derivedStateOf ""
            
            val itemCount = visibleItems.size
            val anglePerItem = 360f / itemCount
            // Нормализуем угол вращения в диапазон [0, 360)
            val normalizedRotation = ((rotationAnim.value % 360f) + 360f) % 360f
            // Секторы рисуются начиная от -90° (вверху), указатель тоже сверху
            // При rotation > 0 колесо вращается по часовой стрелке,
            // поэтому секторы "уезжают" вправо от указателя
            // Указатель указывает на сектор, который был справа от текущей позиции
            // Формула: sectorIndex = floor((360 - rotation) / anglePerItem) % itemCount
            val adjustedRotation = ((360f - normalizedRotation) % 360f + 360f) % 360f
            val sectorIndex = (adjustedRotation / anglePerItem).toInt() % itemCount
            visibleItems.getOrNull(sectorIndex) ?: ""
        }
    }

    // Animated scale for final result
    val resultScale by animateFloatAsState(
        targetValue = if (!uiState.isSpinning && uiState.lastResult != null) 1.3f else 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "result_scale"
    )

    // Localized strings for snackbar
    val allOptionsUsedText = stringResource(R.string.wheel_all_options_used)
    val minItemsText = stringResource(R.string.wheel_min_items)

    suspend fun spin() {
        if (uiState.isSpinning) return
        
        val availableCount = uiState.items.size - uiState.excludedIndices.size
        if (availableCount == 0) {
            snackbarHostState.showSnackbar(allOptionsUsedText)
            return
        }
        
        if (uiState.items.size < 2) {
            snackbarHostState.showSnackbar(minItemsText)
            return
        }
        
        val (_, targetRotation) = viewModel.spin() ?: return
        
        view.playSoundEffect(SoundEffectConstants.CLICK)
        if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
        
        rotationAnim.animateTo(
            targetValue = targetRotation,
            animationSpec = tween(durationMillis = uiState.spinDuration, easing = FastOutSlowInEasing)
        )
        
        // Определяем результат по фактическому углу вращения после остановки
        viewModel.onEvent(WheelUiEvent.SetResultByRotation(rotationAnim.value))
        
        if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
        
        val ctx = view.context
        (ctx.applicationContext as? RandomApplication)?.adsController?.let { ctrl ->
            ctx.findActivity()?.let { act -> ctrl.onCoinTossed(act) }
        }
    }

    WheelScaffold(
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            WheelFabControls(
                fabSize = settings.fabSize,
                onSettingsClick = { viewModel.onEvent(WheelUiEvent.ToggleSettingsSheet) },
                onEditClick = { viewModel.onEvent(WheelUiEvent.ToggleEditorSheet) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dynamic text between topbar and wheel (takes equal space with weight)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val displayText = if (uiState.isSpinning) {
                    currentSectorText
                } else {
                    uiState.lastResult ?: ""
                }
                
                if (displayText.isNotEmpty()) {
                    Text(
                        text = if (!uiState.isSpinning && uiState.lastResult != null) "🎉 $displayText" else displayText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = if (!uiState.isSpinning && uiState.lastResult != null) FontWeight.Bold else FontWeight.Medium,
                        color = if (!uiState.isSpinning && uiState.lastResult != null) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .scale(resultScale)
                            .animateContentSize()
                    )
                } else {
                    // Placeholder text when nothing to show
                    Text(
                        text = stringResource(R.string.wheel_tap_to_spin),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Wheel (centered, fixed size)
            FortuneWheel(
                items = uiState.items,
                excludedIndices = uiState.excludedIndices,
                rotation = rotationAnim.value,
                size = 320.dp,
                onClick = {
                    if (!uiState.isSpinning && uiState.items.size >= 2) {
                        scope.launch { spin() }
                    }
                }
            )

            // Bottom area with info (takes equal space with weight)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.wheel_items_count, visibleItems.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.noRepeats && uiState.excludedIndices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.wheel_used_count, uiState.excludedIndices.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Editor Bottom Sheet
    WheelEditorSheet(
        visible = uiState.showEditorSheet,
        onDismiss = { viewModel.onEvent(WheelUiEvent.ToggleEditorSheet) },
        items = uiState.items,
        excludedIndices = uiState.excludedIndices,
        onUpdateItems = { viewModel.onEvent(WheelUiEvent.UpdateItems(it)) },
        presets = presets,
        onLoadPreset = { preset ->
            val itemsToLoad = preset.items.take(16)
            viewModel.onEvent(WheelUiEvent.LoadPreset(preset.copy(items = itemsToLoad)))
        },
        onSaveAsPreset = { name -> viewModel.saveAsPreset(name) }
    )

    // Settings Bottom Sheet
    WheelSettingsSheet(
        visible = uiState.showSettingsSheet,
        onDismiss = { viewModel.onEvent(WheelUiEvent.ToggleSettingsSheet) },
        noRepeats = uiState.noRepeats,
        onNoRepeatsChange = { viewModel.onEvent(WheelUiEvent.SetNoRepeats(it)) },
        spinDuration = uiState.spinDuration,
        onSpinDurationChange = { viewModel.onEvent(WheelUiEvent.SetSpinDuration(it)) },
        excludedCount = uiState.excludedIndices.size,
        totalCount = uiState.items.size,
        onReset = { viewModel.onEvent(WheelUiEvent.Reset) }
    )
}
