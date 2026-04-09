package com.byteflipper.random.ui.dice

import android.view.SoundEffectConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.ui.components.HapticsManager
import com.byteflipper.random.utils.findActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random

internal object DiceAnimations {
    val ScrimOpen = tween<Float>(
        durationMillis = 250,
        easing = FastOutSlowInEasing
    )
    val ScrimClose = tween<Float>(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )
    val RollRotation = tween<Float>(
        durationMillis = 700,
        easing = FastOutSlowInEasing
    )
    val RollScaleUp = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing
    )
    val RollScaleDown = tween<Float>(
        durationMillis = 250,
        easing = FastOutSlowInEasing
    )
    val SingleRotation = tween<Float>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )
    val SingleScaleUp = tween<Float>(120)
    val SingleScaleDown = tween<Float>(180)
    val ColorChange = tween<androidx.compose.ui.graphics.Color>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )
}

internal fun DiceScreenController.closeOverlay(
    scope: CoroutineScope,
    onVisibilityChange: (Boolean) -> Unit
) {
    scope.launch {
        scrimAlpha.animateTo(0f, DiceAnimations.ScrimClose)
        onVisibilityChange(false)
    }
}

internal fun DiceScreenController.rollAll(
    scope: CoroutineScope,
    uiState: DiceUiState,
    settings: Settings,
    viewModel: DiceViewModel,
    view: android.view.View,
    hapticsManager: HapticsManager?
): Job {
    currentRollJob?.cancel()
    return scope.launch {
        currentRollJob = coroutineContext[Job]
        isRolling = true
        if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
        view.playSoundEffect(SoundEffectConstants.CLICK)
        openOverlayIfNeeded(
            isOverlayVisible = uiState.isOverlayVisible,
            onVisibilityChange = { visible ->
                viewModel.onEvent(DiceUiEvent.SetOverlayVisible(visible))
            }
        )

        viewModel.rollAll()
        randomizeDiceColors()

        val jobs = mutableListOf<Job>()
        repeat(diceCount) { index ->
            val normalizedRotation = normalizedRotation(index)
            rotations[index].snapTo(normalizedRotation)

            jobs += launch {
                val fullRotations = Random.nextInt(3, 6) * 360f
                val finalRotation = fullRotations + 90f * Random.nextInt(0, 4)
                rotations[index].animateTo(
                    targetValue = normalizedRotation + finalRotation,
                    animationSpec = DiceAnimations.RollRotation
                )
            }
            jobs += launch {
                scales[index].animateTo(1.15f, DiceAnimations.RollScaleUp)
                scales[index].animateTo(1f, DiceAnimations.RollScaleDown)
            }
        }
        jobs.forEach { it.join() }
        isRolling = false
        view.context.findActivity()?.let { activity ->
            viewModel.checkAd(activity)
        }
    }
}

internal fun DiceScreenController.rollSingleDie(
    scope: CoroutineScope,
    index: Int,
    settings: Settings,
    viewModel: DiceViewModel,
    hapticsManager: HapticsManager?
): Job = scope.launch {
    if (isAnimating[index]) return@launch

    isAnimating = isAnimating.toMutableList().also { it[index] = true }
    if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
    viewModel.rollOne(index)
    randomizeColorFor(index)

    val normalizedRotation = normalizedRotation(index)
    rotations[index].snapTo(normalizedRotation)
    rotations[index].animateTo(
        targetValue = normalizedRotation + 360f * Random.nextInt(2, 4),
        animationSpec = DiceAnimations.SingleRotation
    )
    scales[index].animateTo(1.12f, DiceAnimations.SingleScaleUp)
    scales[index].animateTo(1f, DiceAnimations.SingleScaleDown)
    isAnimating = isAnimating.toMutableList().also { it[index] = false }
}
