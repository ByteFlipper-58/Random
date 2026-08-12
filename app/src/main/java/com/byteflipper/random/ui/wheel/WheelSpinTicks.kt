package com.byteflipper.random.ui.wheel

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.ui.components.HapticsManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * Minimum gap between ticks. At full speed sectors pass the pointer more than 30 times a second,
 * which the vibrator cannot reproduce and which only floods the effect queue. Dropping the
 * frequent ones makes the ticks emerge naturally as the wheel slows down.
 */
private const val MIN_TICK_INTERVAL_MS = 45L

/**
 * Haptic tick every time a sector divider passes the pointer, both while spinning and while the
 * wheel is being dragged.
 *
 * The rotation is read through [snapshotFlow] rather than in composition, so ticks never trigger
 * recomposition.
 */
@Composable
internal fun WheelSpinTicks(
    isSpinning: Boolean,
    isDragging: Boolean,
    sectorCount: Int,
    hapticsEnabled: Boolean,
    hapticsIntensity: HapticsIntensity,
    hapticsManager: HapticsManager?,
    rotationProvider: () -> Float
) {
    val currentRotationProvider by rememberUpdatedState(rotationProvider)
    val active = isSpinning || isDragging

    LaunchedEffect(active, sectorCount, hapticsEnabled, hapticsIntensity, hapticsManager) {
        if (!active || !hapticsEnabled || hapticsManager == null || sectorCount < 2) {
            return@LaunchedEffect
        }

        var lastTickAt = 0L

        snapshotFlow {
            WheelGeometry.sectorIndexAt(
                rotation = currentRotationProvider(),
                itemCount = sectorCount
            )
        }
            .distinctUntilChanged()
            .drop(1) // The starting sector is not a change of sector.
            .collect {
                val now = SystemClock.uptimeMillis()
                if (now - lastTickAt >= MIN_TICK_INTERVAL_MS) {
                    lastTickAt = now
                    hapticsManager.performTick(hapticsIntensity)
                }
            }
    }
}
