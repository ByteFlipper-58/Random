package com.byteflipper.random.ui.lists.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.ui.components.HapticsManager
import com.byteflipper.random.ui.components.flip.FlipCardOverlay
import com.byteflipper.random.ui.components.flip.FlipCardState
import com.byteflipper.random.ui.lists.ListUiEvent
import com.byteflipper.random.ui.lists.ListUiState
import com.byteflipper.random.ui.theme.CardContentTheme
import com.byteflipper.random.ui.theme.getRainbowColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ListFlipOverlay(
    uiState: ListUiState,
    settings: Settings,
    flipCardState: FlipCardState,
    anchorInRoot: Offset,
    context: Context,
    snackbarHostState: SnackbarHostState,
    hapticsManager: HapticsManager?,
    clipboardLabel: String,
    onEvent: (ListUiEvent) -> Unit,
    onClosed: () -> Unit,
    scope: CoroutineScope,
    isGenerating: Boolean
) {
    val rainbowColors = getRainbowColors()
    val animatedColor = remember { Animatable(Color.Transparent) }
    val targetColor = remember(uiState.cardColorSeed, uiState.results) {
        val random = uiState.cardColorSeed?.let(::Random) ?: Random
        rainbowColors[random.nextInt(rainbowColors.size)]
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
    val effectiveCount = if (isGenerating || uiState.results.isEmpty()) 1 else uiState.results.size
    val baseScale = when {
        effectiveCount <= 10 -> 1.0
        effectiveCount <= 25 -> 1.15
        effectiveCount <= 50 -> 1.3
        else -> 1.5
    }
    val basePx = (280 * baseScale).toInt()
    val dynamicMin = 240.coerceAtMost(maxCardWidth.value.toInt())
    val targetCardSize = basePx.coerceIn(dynamicMin, maxCardWidth.value.toInt()).dp
    val heightScale = when {
        effectiveCount <= 5 -> 1.0f
        effectiveCount <= 10 -> 1.2f
        effectiveCount <= 20 -> 1.4f
        effectiveCount <= 40 -> 1.6f
        effectiveCount <= 75 -> 1.8f
        else -> 2.2f
    }
    val minHeight = 300.dp.coerceAtMost(maxCardHeight)
    val targetCardHeight = (targetCardSize * heightScale).coerceIn(minHeight, maxCardHeight)

    val animatedCardSize = animateDpAsState(
        targetValue = targetCardSize,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    )
    val animatedCardHeight = animateDpAsState(
        targetValue = targetCardHeight,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    )

    FlipCardOverlay(
        state = flipCardState,
        anchorInRoot = anchorInRoot,
        onClosed = {
            onClosed()
            onEvent(ListUiEvent.ClearResults)
            onEvent(ListUiEvent.SetOverlayVisible(false))
        },
        frontContainerColor = animatedColor.value,
        backContainerColor = animatedColor.value,
        cardSize = animatedCardSize.value,
        cardHeight = animatedCardHeight.value,
        onLongPress = {
            if (uiState.results.isNotEmpty()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText(clipboardLabel, uiState.results.joinToString(", "))
                )
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
                }
                if (settings.hapticsEnabled) {
                    hapticsManager?.performPress(settings.hapticsIntensity)
                }
            }
        },
        frontContent = {
            CardContentTheme {
                ListResultsDisplay(
                    results = uiState.results,
                    cardColor = animatedColor.value,
                    cardSize = animatedCardHeight.value
                )
            }
        },
        backContent = {
            CardContentTheme {
                ListResultsDisplay(
                    results = uiState.results,
                    cardColor = animatedColor.value,
                    cardSize = animatedCardHeight.value
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
