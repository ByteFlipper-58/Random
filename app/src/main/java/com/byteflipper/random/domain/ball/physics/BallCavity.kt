package com.byteflipper.random.domain.ball.physics

import com.byteflipper.random.domain.physics.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The spherical glass cavity and the liquid inside it.
 *
 * The liquid surface is treated as a plane perpendicular to gravity — the SPH particles add the
 * splashing on top of it, but the bulk level stays analytic so it can never drift or leak.
 */
class BallCavity(
    private val radius: Float = BallEngineTuning.CAVITY_RADIUS,
    fillFraction: Float = BallEngineTuning.FLUID_FILL
) {

    /**
     * Signed distance from the cavity centre to the liquid surface, measured along "up". Negative
     * when the ball is more than half full, which it normally is.
     */
    val surfaceOffset: Float = solveSurfaceOffset(radius, fillFraction)

    /** Distance from [position] down to the surface: positive under the liquid. */
    fun depthBelowSurface(position: Vec3, up: Vec3): Float = surfaceOffset - (position dot up)

    /** Roughly how much of a sphere at [position] sits in the liquid, from 0 to 1. */
    fun submergedFraction(position: Vec3, bodyRadius: Float, up: Vec3): Float {
        if (bodyRadius <= Vec3.EPSILON) return if (depthBelowSurface(position, up) > 0f) 1f else 0f
        val depth = depthBelowSurface(position, up)
        return ((depth + bodyRadius) / (2f * bodyRadius)).coerceIn(0f, 1f)
    }

    /**
     * Pushes [body] back inside the wall and reflects its velocity.
     *
     * Returns the speed at which it hit, or 0 when it was already inside — the engine turns that
     * into haptics.
     */
    fun constrain(
        body: RigidBody,
        restitution: Float = BallEngineTuning.WALL_RESTITUTION,
        friction: Float = BallEngineTuning.WALL_FRICTION
    ): Float {
        val limit = radius - body.radius
        if (limit <= 0f) {
            // Degenerate tuning: pin the body at the centre rather than fight the wall forever.
            body.position = Vec3.ZERO
            return 0f
        }

        val distance = body.position.length
        if (distance <= limit) return 0f

        val normal = if (distance < Vec3.EPSILON) Vec3.UP else body.position / distance
        body.position = normal * limit

        val normalSpeed = body.velocity dot normal
        if (normalSpeed <= 0f) return 0f

        val normalComponent = normal * normalSpeed
        val tangentComponent = body.velocity - normalComponent
        body.velocity = tangentComponent * (1f - friction) - normalComponent * restitution

        // A glancing blow should also set the die spinning a little.
        body.angularVelocity += (normal cross tangentComponent) * (friction * 2.4f)

        return normalSpeed
    }

    private companion object {
        /**
         * Height of the empty spherical cap that leaves [fillFraction] of the sphere filled, found
         * by bisection on `cap(t) = t^2 * (3R - t) / 3 * PI`.
         */
        fun solveSurfaceOffset(radius: Float, fillFraction: Float): Float {
            // The upper bound leaves a cap the bubbles can still pop into; the solve itself is happy
            // with anything below one, but a surface flush against the glass has nowhere to slosh.
            val fill = fillFraction.coerceIn(0.05f, 0.98f)
            val target = 4f * (1f - fill) * radius * radius * radius
            var low = 0f
            var high = 2f * radius
            repeat(48) {
                val mid = (low + high) * 0.5f
                val volume = mid * mid * (3f * radius - mid)
                if (volume < target) low = mid else high = mid
            }
            val capHeight = (low + high) * 0.5f
            return radius - capHeight
        }
    }
}

/** Clamps a value into a symmetric range around zero. */
internal fun clampAbs(value: Float, limit: Float): Float = max(-limit, min(limit, value))

/** True when both floats are within [tolerance] of each other. */
internal fun approxEquals(a: Float, b: Float, tolerance: Float = 1e-4f): Boolean =
    abs(a - b) <= tolerance
