package com.byteflipper.random.ui.wheel

import kotlin.math.atan2

/**
 * Single source of truth for the wheel geometry.
 *
 * Sectors are drawn starting at `position * sweep - 90°` clockwise, and the pointer sits at the
 * top (-90°), so the sector under the pointer is `floor(normalize(360 - rotation) / sweep)`.
 * Both the target angle and the current sector must be derived from here, otherwise the visual
 * result drifts away from the logical one.
 */
internal object WheelGeometry {

    const val FULL_TURN = 360f

    fun sweepAngle(itemCount: Int): Float = FULL_TURN / itemCount

    fun normalizeRotation(rotation: Float): Float =
        ((rotation % FULL_TURN) + FULL_TURN) % FULL_TURN

    /** Position of the sector that is under the pointer at the given rotation. */
    fun sectorIndexAt(rotation: Float, itemCount: Int): Int {
        if (itemCount <= 0) return -1
        val pointerAngle = normalizeRotation(FULL_TURN - normalizeRotation(rotation))
        return (pointerAngle / sweepAngle(itemCount)).toInt().coerceIn(0, itemCount - 1)
    }

    /**
     * Rotation, within a single turn, that puts [sectorIndex] under the pointer.
     * [sectorFraction] picks where inside the sector to stop, 0f..1f.
     */
    fun rotationForSector(sectorIndex: Int, itemCount: Int, sectorFraction: Float): Float {
        if (itemCount <= 0) return 0f
        val sweep = sweepAngle(itemCount)
        val angle = sectorIndex * sweep + sweep * sectorFraction.coerceIn(0f, 1f)
        return normalizeRotation(FULL_TURN - angle)
    }

    /**
     * Animation target: [fullTurns] complete turns away from [from], ending exactly at
     * [targetRotationInTurn] modulo 360°.
     */
    fun animationTarget(
        from: Float,
        fullTurns: Int,
        targetRotationInTurn: Float,
        clockwise: Boolean = true
    ): Float = if (clockwise) {
        from + fullTurns * FULL_TURN + normalizeRotation(targetRotationInTurn - from)
    } else {
        from - fullTurns * FULL_TURN - normalizeRotation(from - targetRotationInTurn)
    }

    /**
     * Angle of a point around the center, in Canvas degrees (0° points right, clockwise).
     * Turns finger movement around the wheel into rotation.
     */
    fun angleOf(dx: Float, dy: Float): Float =
        Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

    /** Difference between two angles, wrapped to -180°..180° so it never jumps across the seam. */
    fun angleDelta(from: Float, to: Float): Float {
        val raw = normalizeRotation(to - from)
        return if (raw > 180f) raw - FULL_TURN else raw
    }
}
