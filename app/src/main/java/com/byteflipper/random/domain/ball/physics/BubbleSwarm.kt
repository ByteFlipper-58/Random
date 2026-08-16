package com.byteflipper.random.domain.ball.physics

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The handful of air bubbles drifting up through the liquid.
 *
 * They are not part of the SPH solve — a bubble small enough to see is smaller than a particle, so
 * simulating one properly would cost more than it is worth. Instead each is an analytic sphere that
 * climbs along "up", wobbles, gets carried around by the turning glass, and pops when it reaches the
 * surface. The interior shader intersects the same spheres, so what the player sees is exactly what
 * is stored here.
 */
class BubbleSwarm(
    private val cavityRadius: Float = BallEngineTuning.CAVITY_RADIUS,
    private val capacity: Int = BallEngineTuning.MAX_BUBBLES,
    private val random: Random = Random.Default
) {

    private val px = FloatArray(capacity)
    private val py = FloatArray(capacity)
    private val pz = FloatArray(capacity)
    private val radius = FloatArray(capacity)
    private val phase = FloatArray(capacity)
    private val riseSpeed = FloatArray(capacity)

    var count: Int = 0
        private set

    /** Refills the swarm; called on a tier change or a reset, never per frame. */
    fun seed(bubbleCount: Int, cavity: BallCavity, up: Vec3) {
        count = bubbleCount.coerceIn(0, capacity)
        if (count == 0) return

        val tangent = perpendicular(up)
        val bitangent = (up cross tangent).normalized()
        for (index in 0 until count) {
            respawn(index, cavity, up, tangent, bitangent, spread = true)
        }
    }

    fun step(
        deltaSeconds: Float,
        elapsedSeconds: Float,
        cavity: BallCavity,
        up: Vec3,
        shellAngularVelocity: Vec3
    ) {
        if (count == 0 || deltaSeconds <= 0f) return

        val tangent = perpendicular(up)
        val bitangent = (up cross tangent).normalized()
        val limit = cavityRadius - BallEngineTuning.PARTICLE_RADIUS

        for (index in 0 until count) {
            val wobblePhase = phase[index] + elapsedSeconds * WOBBLE_RATE
            val wobble = WOBBLE_AMPLITUDE * radius[index] / REFERENCE_RADIUS

            // Rise, wobble across the rise, and get dragged along by the glass.
            var velocityX = up.x * riseSpeed[index] +
                tangent.x * sin(wobblePhase) * wobble +
                bitangent.x * cos(wobblePhase * 0.77f) * wobble
            var velocityY = up.y * riseSpeed[index] +
                tangent.y * sin(wobblePhase) * wobble +
                bitangent.y * cos(wobblePhase * 0.77f) * wobble
            var velocityZ = up.z * riseSpeed[index] +
                tangent.z * sin(wobblePhase) * wobble +
                bitangent.z * cos(wobblePhase * 0.77f) * wobble

            val x = px[index]
            val y = py[index]
            val z = pz[index]
            velocityX += (shellAngularVelocity.y * z - shellAngularVelocity.z * y) * SWIRL
            velocityY += (shellAngularVelocity.z * x - shellAngularVelocity.x * z) * SWIRL
            velocityZ += (shellAngularVelocity.x * y - shellAngularVelocity.y * x) * SWIRL

            px[index] = x + velocityX * deltaSeconds
            py[index] = y + velocityY * deltaSeconds
            pz[index] = z + velocityZ * deltaSeconds

            val position = Vec3(px[index], py[index], pz[index])
            val breached = cavity.depthBelowSurface(position, up) <= radius[index] * 0.5f
            if (breached || !position.isFinite) {
                respawn(index, cavity, up, tangent, bitangent, spread = false)
                continue
            }

            // Bubbles hug the glass on their way up rather than passing through it.
            val distance = position.length
            val ceiling = limit - radius[index]
            if (distance > ceiling && distance > Vec3.EPSILON) {
                val scale = ceiling / distance
                px[index] *= scale
                py[index] *= scale
                pz[index] *= scale
            }
        }
    }

    /**
     * Packs centre and radius per bubble into [target] as `x, y, z, r`, and returns how many were
     * written — the shader's uniform array is read with exactly that count.
     */
    fun writeTo(target: FloatArray): Int {
        val written = minOf(count, target.size / STRIDE)
        for (index in 0 until written) {
            val base = index * STRIDE
            target[base] = px[index]
            target[base + 1] = py[index]
            target[base + 2] = pz[index]
            target[base + 3] = radius[index]
        }
        return written
    }

    fun positionAt(index: Int): Vec3 = Vec3(px[index], py[index], pz[index])

    fun radiusAt(index: Int): Float = radius[index]

    /**
     * Drops a bubble back into the liquid.
     *
     * [spread] fills the whole depth, which is what a fresh swarm wants; without it the bubble starts
     * near the bottom, the way one that just popped should.
     */
    private fun respawn(
        index: Int,
        cavity: BallCavity,
        up: Vec3,
        tangent: Vec3,
        bitangent: Vec3,
        spread: Boolean
    ) {
        val limit = cavityRadius - BallEngineTuning.PARTICLE_RADIUS
        val bottom = -limit * 0.9f
        val depth = if (spread) random.nextFloat() else random.nextFloat() * 0.35f
        val axial = bottom + (cavity.surfaceOffset - bottom) * depth

        val lateralLimit = sqrt(max(limit * limit - axial * axial, 0f)) * 0.85f
        val angle = random.nextFloat() * TWO_PI
        val lateral = sqrt(random.nextFloat()) * lateralLimit

        val position = tangent * (cos(angle) * lateral) +
            bitangent * (sin(angle) * lateral) +
            up * axial
        px[index] = position.x
        py[index] = position.y
        pz[index] = position.z

        radius[index] = MIN_RADIUS + random.nextFloat() * (MAX_RADIUS - MIN_RADIUS)
        phase[index] = random.nextFloat() * TWO_PI
        // Bigger bubbles climb faster, which keeps the swarm from moving as one sheet.
        riseSpeed[index] = BallEngineTuning.BUBBLE_RISE_SPEED *
            (0.55f + 0.9f * radius[index] / MAX_RADIUS)
    }

    /** Any unit vector perpendicular to [axis]. */
    private fun perpendicular(axis: Vec3): Vec3 {
        val reference = if (kotlin.math.abs(axis.z) < 0.9f) Vec3.FORWARD else Vec3.UP
        return (reference cross axis).normalized(Vec3(1f, 0f, 0f))
    }

    private companion object {
        const val STRIDE = 4
        const val TWO_PI = 6.2831855f

        const val MIN_RADIUS = 0.018f
        const val MAX_RADIUS = 0.052f
        const val REFERENCE_RADIUS = 0.035f

        /** Radians per second of the sideways wobble. */
        const val WOBBLE_RATE = 2.6f
        const val WOBBLE_AMPLITUDE = 0.075f

        /** How much of the shell's spin the bubbles pick up. */
        const val SWIRL = 0.35f
    }
}
