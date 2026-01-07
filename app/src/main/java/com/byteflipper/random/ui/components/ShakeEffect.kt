package com.byteflipper.random.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.utils.ShakeDetector

/**
 * Composable helper that creates and manages a ShakeDetector.
 * Automatically starts/stops listening based on the lifecycle.
 * Provides haptic feedback when shake is detected.
 *
 * @param enabled Whether shake detection should be active
 * @param hapticsEnabled Whether haptic feedback is enabled in settings
 * @param hapticsIntensity Intensity level for haptic feedback
 * @param onShake Callback invoked when a shake gesture is detected
 */
@Composable
fun ShakeEffect(
    enabled: Boolean,
    hapticsEnabled: Boolean = true,
    hapticsIntensity: HapticsIntensity = HapticsIntensity.Medium,
    onShake: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticsManager = LocalHapticsManager.current

    val shakeDetector = remember(context, onShake, hapticsEnabled, hapticsIntensity) {
        ShakeDetector(
            context = context,
            onShake = {
                // Haptic feedback when shake is detected
                if (hapticsEnabled) {
                    hapticsManager?.performPress(hapticsIntensity)
                }
                onShake()
            }
        )
    }

    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (enabled) shakeDetector.start()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    shakeDetector.stop()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        // Start immediately if already resumed and enabled
        if (enabled && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            shakeDetector.start()
        }

        onDispose {
            shakeDetector.stop()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
