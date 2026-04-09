package com.byteflipper.random.ui.wheel

import android.view.SoundEffectConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.SnackbarHostState
import com.byteflipper.random.utils.findActivity

internal suspend fun WheelScreenController.spin(
    uiState: WheelUiState,
    settings: com.byteflipper.random.data.settings.Settings,
    viewModel: WheelViewModel,
    snackbarHostState: SnackbarHostState,
    view: android.view.View,
    hapticsManager: com.byteflipper.random.ui.components.HapticsManager?,
    allOptionsUsedText: String,
    minItemsText: String
) {
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

    viewModel.onEvent(WheelUiEvent.SetResultByRotation(rotationAnim.value))

    if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)

    showConfetti = true
    view.context.findActivity()?.let { act -> viewModel.checkAd(act) }
}
