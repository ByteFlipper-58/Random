package com.byteflipper.random.domain.physics

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Unit quaternion shared by the physical generators for rigid-body orientation.
 *
 * Stored as (w, x, y, z) and kept normalised — [times] and [integrated] renormalise, so drift never
 * accumulates over a long shake.
 */
data class Quat(val w: Float, val x: Float, val y: Float, val z: Float) {

    /** Hamilton product: `this` applied *after* [other]. */
    operator fun times(other: Quat): Quat = Quat(
        w * other.w - x * other.x - y * other.y - z * other.z,
        w * other.x + x * other.w + y * other.z - z * other.y,
        w * other.y - x * other.z + y * other.w + z * other.x,
        w * other.z + x * other.y - y * other.x + z * other.w
    ).normalized()

    operator fun times(scalar: Float): Quat = Quat(w * scalar, x * scalar, y * scalar, z * scalar)

    operator fun plus(other: Quat): Quat = Quat(w + other.w, x + other.x, y + other.y, z + other.z)

    val conjugate: Quat get() = Quat(w, -x, -y, -z)

    fun normalized(): Quat {
        val length = sqrt(w * w + x * x + y * y + z * z)
        return if (length < Vec3.EPSILON) IDENTITY else Quat(w / length, x / length, y / length, z / length)
    }

    /** Rotates [vector] into the frame this quaternion describes. */
    fun rotate(vector: Vec3): Vec3 {
        val u = Vec3(x, y, z)
        val uv = u cross vector
        val uuv = u cross uv
        return vector + (uv * w + uuv) * 2f
    }

    /** Inverse rotation; cheaper than building the conjugate first. */
    fun inverseRotate(vector: Vec3): Vec3 = conjugate.rotate(vector)

    /**
     * One step of `dq/dt = 0.5 * omega * q` with [angularVelocity] in radians per second. Good
     * enough at the engine's 1/120 s step and far cheaper than an exponential map.
     */
    fun integrated(angularVelocity: Vec3, deltaSeconds: Float): Quat {
        val half = 0.5f * deltaSeconds
        val delta = Quat(
            -half * (angularVelocity.x * x + angularVelocity.y * y + angularVelocity.z * z),
            half * (angularVelocity.x * w + angularVelocity.y * z - angularVelocity.z * y),
            half * (angularVelocity.y * w + angularVelocity.z * x - angularVelocity.x * z),
            half * (angularVelocity.z * w + angularVelocity.x * y - angularVelocity.y * x)
        )
        return (this + delta).normalized()
    }

    /** Angle of the shortest rotation from this orientation to [target], in radians. */
    fun angleTo(target: Quat): Float {
        val dot = abs(w * target.w + x * target.x + y * target.y + z * target.z).coerceIn(0f, 1f)
        return 2f * acos(dot)
    }

    /**
     * Rotation error as an axis-times-angle vector pointing from this orientation to [target].
     * Feeding it straight into a PD controller is what steers the chosen face to the window.
     */
    fun errorTo(target: Quat): Vec3 {
        // Same rotation, opposite sign: pick the hemisphere that gives the shorter path.
        val aligned = if (w * target.w + x * target.x + y * target.y + z * target.z < 0f) {
            Quat(-target.w, -target.x, -target.y, -target.z)
        } else {
            target
        }
        val delta = aligned * conjugate
        val axis = Vec3(delta.x, delta.y, delta.z)
        val sinHalf = axis.length
        if (sinHalf < Vec3.EPSILON) return Vec3.ZERO
        val angle = 2f * acos(delta.w.coerceIn(-1f, 1f))
        return axis * (angle / sinHalf)
    }

    /** Column-major 4x4 rotation matrix, ready for `glUniformMatrix4fv`. */
    fun writeMatrix(target: FloatArray, offset: Int = 0) {
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z

        target[offset] = 1f - 2f * (yy + zz)
        target[offset + 1] = 2f * (xy + wz)
        target[offset + 2] = 2f * (xz - wy)
        target[offset + 3] = 0f

        target[offset + 4] = 2f * (xy - wz)
        target[offset + 5] = 1f - 2f * (xx + zz)
        target[offset + 6] = 2f * (yz + wx)
        target[offset + 7] = 0f

        target[offset + 8] = 2f * (xz + wy)
        target[offset + 9] = 2f * (yz - wx)
        target[offset + 10] = 1f - 2f * (xx + yy)
        target[offset + 11] = 0f

        target[offset + 12] = 0f
        target[offset + 13] = 0f
        target[offset + 14] = 0f
        target[offset + 15] = 1f
    }

    companion object {
        val IDENTITY = Quat(1f, 0f, 0f, 0f)

        fun fromAxisAngle(axis: Vec3, radians: Float): Quat {
            val unit = axis.normalized()
            val half = radians * 0.5f
            val s = sin(half)
            return Quat(cos(half), unit.x * s, unit.y * s, unit.z * s).normalized()
        }

        /** Shortest rotation taking [from] onto [to]; both are normalised here. */
        fun rotationBetween(from: Vec3, to: Vec3): Quat {
            val a = from.normalized()
            val b = to.normalized()
            val dot = (a dot b).coerceIn(-1f, 1f)
            if (dot > 1f - 1e-6f) return IDENTITY
            if (dot < -1f + 1e-6f) {
                // Opposite vectors: any perpendicular axis gives a valid half turn.
                val axis = (a cross Vec3.UP).let { if (it.lengthSquared < 1e-8f) a cross Vec3.FORWARD else it }
                return fromAxisAngle(axis, Math.PI.toFloat())
            }
            val axis = a cross b
            return Quat(1f + dot, axis.x, axis.y, axis.z).normalized()
        }
    }
}
