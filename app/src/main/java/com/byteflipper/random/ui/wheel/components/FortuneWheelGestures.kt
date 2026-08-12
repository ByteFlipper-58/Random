package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import com.byteflipper.random.ui.wheel.WheelGeometry

/**
 * Spinning the wheel with a finger.
 *
 * While the finger is down the wheel follows it: movement around the center is turned into
 * rotation through [WheelGeometry.angleOf]. On release the angular velocity decides the direction
 * and the number of turns.
 *
 * The callbacks are deliberately not suspending. [onRotate] writes the rotation directly and
 * [onFling] only kicks off the spin, which keeps the whole drag inside a single
 * `awaitEachGesture`; leaving the pointer scope on every event to await something would drop
 * events and make the wheel stutter under the finger.
 *
 * Touch slop comes from the system rather than being expressed in degrees: near the center the
 * same finger movement produces a much larger angle than near the rim, so a degree threshold
 * would trigger unevenly across the wheel.
 */
internal fun Modifier.wheelSpinGesture(
    enabled: Boolean,
    onDragStateChange: (Boolean) -> Unit,
    onRotate: (deltaDegrees: Float) -> Unit,
    onFling: (velocityDegreesPerSecond: Float) -> Unit
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput

    val center = Offset(size.width / 2f, size.height / 2f)

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        var previousAngle = angleAt(down.position, center)
        var travelled = 0f

        // Velocity is tracked over the accumulated angle: the velocity of the touch point itself
        // would depend on how close to the center the finger is.
        val velocityTracker = VelocityTracker1D(isDataDifferential = false)
        velocityTracker.addDataPoint(down.uptimeMillis, 0f)

        fun track(position: Offset, uptimeMillis: Long) {
            val angle = angleAt(position, center)
            val delta = WheelGeometry.angleDelta(previousAngle, angle)
            previousAngle = angle
            travelled += delta
            velocityTracker.addDataPoint(uptimeMillis, travelled)
            onRotate(delta)
        }

        // Nothing is consumed until slop is passed, so a plain tap still reaches the clickable.
        val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ ->
            change.consume()
        } ?: return@awaitEachGesture

        onDragStateChange(true)
        track(dragStart.position, dragStart.uptimeMillis)

        val completed = drag(dragStart.id) { change ->
            track(change.position, change.uptimeMillis)
            change.consume()
        }

        onDragStateChange(false)
        onFling(if (completed) velocityTracker.calculateVelocity() else 0f)
    }
}

private fun angleAt(position: Offset, center: Offset): Float =
    WheelGeometry.angleOf(position.x - center.x, position.y - center.y)
