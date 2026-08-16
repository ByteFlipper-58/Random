package com.byteflipper.random.domain.ball.physics

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Immutable 3D vector.
 *
 * The rigid bodies use these directly; the fluid keeps its particles in flat `FloatArray`s instead,
 * because there are hundreds of them per step and the allocations would show up.
 */
data class Vec3(val x: Float, val y: Float, val z: Float) {

    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)

    operator fun div(scalar: Float): Vec3 = Vec3(x / scalar, y / scalar, z / scalar)

    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    infix fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    infix fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    val lengthSquared: Float get() = x * x + y * y + z * z

    val length: Float get() = sqrt(lengthSquared)

    /** Unit vector, or [fallback] when this one is too short to have a direction. */
    fun normalized(fallback: Vec3 = UP): Vec3 {
        val length = length
        return if (length < EPSILON) fallback else Vec3(x / length, y / length, z / length)
    }

    /** Length capped at [maxLength], direction untouched. */
    fun clampLength(maxLength: Float): Vec3 {
        val length = length
        return if (length <= maxLength || length < EPSILON) this else this * (maxLength / length)
    }

    fun lerp(target: Vec3, t: Float): Vec3 = Vec3(
        x + (target.x - x) * t,
        y + (target.y - y) * t,
        z + (target.z - z) * t
    )

    /** True when every component is finite; a NaN here would freeze the whole simulation. */
    val isFinite: Boolean get() = x.isFinite() && y.isFinite() && z.isFinite()

    fun approxEquals(other: Vec3, tolerance: Float = 1e-4f): Boolean =
        abs(x - other.x) <= tolerance &&
            abs(y - other.y) <= tolerance &&
            abs(z - other.z) <= tolerance

    /** Writes x, y, z into [target] starting at [offset], for uniform uploads. */
    fun writeTo(target: FloatArray, offset: Int = 0) {
        target[offset] = x
        target[offset + 1] = y
        target[offset + 2] = z
    }

    companion object {
        const val EPSILON = 1e-6f

        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, 1f, 0f)
        val DOWN = Vec3(0f, -1f, 0f)
        val FORWARD = Vec3(0f, 0f, 1f)
    }
}

operator fun Float.times(vector: Vec3): Vec3 = vector * this
