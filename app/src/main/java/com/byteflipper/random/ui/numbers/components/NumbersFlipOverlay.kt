package com.byteflipper.random.ui.numbers.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.ui.components.HapticsManager
import com.byteflipper.random.ui.components.flip.FlipCardOverlay
import com.byteflipper.random.ui.components.flip.FlipCardState
import com.byteflipper.random.ui.numbers.NumbersUiEvent
import com.byteflipper.random.ui.numbers.NumbersUiState
import com.byteflipper.random.ui.theme.CardContentTheme
import com.byteflipper.random.ui.theme.getRainbowColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun NumbersFlipOverlay(
    uiState: NumbersUiState,
    settings: Settings,
    flipCardState: FlipCardState,
    anchorInRoot: Offset,
    context: Context,
    snackbarHostState: SnackbarHostState,
    hapticsManager: HapticsManager?,
    clipboardLabel: String,
    onEvent: (NumbersUiEvent) -> Unit,
    onClosed: () -> Unit,
    scope: CoroutineScope,
    isGenerating: Boolean
) {
    val rainbowColors = getRainbowColors()
    val animatedColor = remember { Animatable(Color.Transparent) }
    val targetColor = remember(uiState.cardColorSeed, uiState.frontValues) {
        pickStableColor(uiState.cardColorSeed, rainbowColors)
    }

    LaunchedEffect(targetColor) {
        if (animatedColor.value == Color.Transparent) {
            animatedColor.snapTo(targetColor)
        } else {
            animatedColor.animateTo(targetColor, tween(400))
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val maxCardWidth = (screenWidthDp - 32.dp).coerceAtLeast(200.dp)
    val maxCardHeight = (screenHeightDp - 160.dp).coerceIn(300.dp, 580.dp)
    val resultsCountForSizing = if (isGenerating) 1 else maxOf(uiState.frontValues.size, uiState.backValues.size)
    val effectiveCount = if (resultsCountForSizing > 0) resultsCountForSizing else 1
    val basePx = computeCardBaseSizeDp(effectiveCount)
    val dynamicMin = 240.coerceAtMost(maxCardWidth.value.toInt())
    val targetCardSize = basePx.coerceIn(dynamicMin, maxCardWidth.value.toInt()).dp
    val heightScale = computeHeightScale(effectiveCount)
    val minHeight = 300.dp.coerceAtMost(maxCardHeight)
    val targetContentHeight = (targetCardSize * heightScale).coerceIn(minHeight, maxCardHeight)

    val animatedCardSize = animateDpAsState(
        targetValue = targetCardSize,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    )
    val animatedCardHeight = animateDpAsState(
        targetValue = targetContentHeight,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    )

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        FlipCardOverlay(
            state = flipCardState,
            anchorInRoot = anchorInRoot,
            onClosed = {
                onClosed()
                onEvent(NumbersUiEvent.ClearResults)
                onEvent(NumbersUiEvent.SetOverlayVisible(false))
            },
            cardSize = animatedCardSize.value,
            cardHeight = animatedCardHeight.value,
            frontContainerColor = animatedColor.value,
            backContainerColor = animatedColor.value,
            onLongPress = {
                val results = if (uiState.frontValues.isNotEmpty()) uiState.frontValues else uiState.backValues
                if (results.isNotEmpty()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(clipboardLabel, results.joinToString(", "))
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(com.byteflipper.random.R.string.copied_to_clipboard))
                    }
                    if (settings.hapticsEnabled) {
                        hapticsManager?.performPress(settings.hapticsIntensity)
                    }
                }
            },
            frontContent = {
                CardContentTheme {
                    NumbersResultsDisplay(
                        results = uiState.frontValues,
                        cardColor = animatedColor.value,
                        cardSize = animatedCardHeight.value
                    )
                }
            },
            backContent = {
                CardContentTheme {
                    NumbersResultsDisplay(
                        results = uiState.backValues,
                        cardColor = animatedColor.value,
                        cardSize = animatedCardHeight.value
                    )
                }
            }
        )
    }
}
