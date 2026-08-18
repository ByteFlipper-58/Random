package com.byteflipper.random.domain.dice.physics

import com.byteflipper.random.domain.physics.Quat
import com.byteflipper.random.domain.physics.Vec3
import kotlin.math.abs
import kotlin.math.exp

/**
 * One die: a cube with mass, spin, contact-point impulses and its own sleep state.
 *
 * Deliberately not the ball's `RigidBody`. That one carries a solid-sphere inertia, which is a fine
 * approximation for something tumbling in syrup but wrong for a cube being solved against its own
 * corners — and a die also needs contact-point impulses and its own sleep state, neither of which a
 * body floating in a fluid ever asked for.
 *
 * A cube's inertia tensor happens to be isotropic, so all of that fits in one scalar and none of the
 * usual rotate-the-tensor-into-world-space work is needed.
 */
class DiceBody {

    var halfExtent: Float = 0.5f

    var position: Vec3 = Vec3.ZERO
    var velocity: Vec3 = Vec3.ZERO
    var orientation: Quat = Quat.IDENTITY
    var angularVelocity: Vec3 = Vec3.ZERO

    /** True while there is something underneath — the floor, a wall, or another die. */
    var supported: Boolean = false

    /** Dice at rest are immovable, which is what lets a pile of them hold still. */
    var asleep: Boolean = false
        private set

    private var calmSeconds: Float = 0f

    /** Uniform dice, so the absolute figure never matters — only the ratio to the inertia does. */
    private val mass: Float = 1f

    /** Zero for a die the solver must treat as part of the scenery. */
    val inverseMass: Float get() = if (asleep) 0f else 1f / mass

    /** A cube's inertia is isotropic: `I = (2/3) m h²`, the same about every axis through its centre. */
    val inverseInertia: Float
        get() = if (asleep) 0f else 1f / (INERTIA_FACTOR * mass * halfExtent * halfExtent)

    /** World direction of body axis [index] — 0 is x, 1 is y, 2 is z. */
    fun axis(index: Int): Vec3 = orientation.rotate(BODY_AXES[index])

    /**
     * World position of corner [index], 0..7, its bits reading as the sign along x, y and z.
     *
     * Eight corners against six walls is the whole of the die-versus-tray test: a face lying flat on
     * the floor produces four contacts that way, which is exactly what holds it flat.
     */
    fun corner(index: Int): Vec3 {
        val sx = if (index and 1 == 0) -halfExtent else halfExtent
        val sy = if (index and 2 == 0) -halfExtent else halfExtent
        val sz = if (index and 4 == 0) -halfExtent else halfExtent
        return position + orientation.rotate(Vec3(sx, sy, sz))
    }

    /** Velocity of the material point at [point], spin included. */
    fun pointVelocity(point: Vec3): Vec3 = velocity + (angularVelocity cross (point - position))

    fun applyImpulse(impulse: Vec3, at: Vec3) {
        velocity += impulse * inverseMass
        angularVelocity += ((at - position) cross impulse) * inverseInertia
    }

    fun applyImpulse(impulse: Vec3) {
        velocity += impulse * inverseMass
    }

    fun applyAngularImpulse(impulse: Vec3) {
        angularVelocity += impulse * inverseInertia
    }

    fun damp(linearPerSecond: Float, angularPerSecond: Float, deltaSeconds: Float) {
        velocity *= exp(-linearPerSecond * deltaSeconds)
        angularVelocity *= exp(-angularPerSecond * deltaSeconds)
    }

    fun integrate(deltaSeconds: Float) {
        position += velocity * deltaSeconds
        orientation = orientation.integrated(angularVelocity, deltaSeconds)
    }

    fun clampSpeeds(maxLinear: Float, maxAngular: Float) {
        velocity = velocity.clampLength(maxLinear)
        angularVelocity = angularVelocity.clampLength(maxAngular)
    }

    /** Puts the die back somewhere sane if the arithmetic ever escapes; one NaN would freeze it all. */
    fun sanitize() {
        if (!position.isFinite) position = Vec3(0f, halfExtent, 0f)
        if (!velocity.isFinite) velocity = Vec3.ZERO
        if (!angularVelocity.isFinite) angularVelocity = Vec3.ZERO
        if (!orientation.w.isFinite() || !orientation.x.isFinite() ||
            !orientation.y.isFinite() || !orientation.z.isFinite()
        ) {
            orientation = Quat.IDENTITY
        }
    }

    /** The number currently facing up, which is what a player would read off the tray. */
    fun upValue(): Int {
        var best = 0
        var bestDot = -Float.MAX_VALUE
        for (face in 0 until DieFaces.FACE_COUNT) {
            val dot = orientation.rotate(DieFaces.normals[face]) dot Vec3.UP
            if (dot > bestDot) {
                bestDot = dot
                best = face
            }
        }
        return DieFaces.values[best]
    }

    /**
     * Ends a pathological roll without rotating the die or changing its visible result.
     *
     * An airborne body is put on the felt inside the walls. Its orientation is preserved, and the
     * centre uses the rotated cube's vertical extent so no corner is buried in the floor.
     */
    fun forceSleep(trayNearHalfX: Float, trayFarHalfX: Float, trayHalfZ: Float) {
        if (!supported && position.y > halfExtent * AIRBORNE_HEIGHT) {
            val limitZ = (trayHalfZ - halfExtent).coerceAtLeast(0f)
            val z = position.z.coerceIn(-limitZ, limitZ)
            val t = ((z + trayHalfZ) / (2f * trayHalfZ)).coerceIn(0f, 1f)
            val width = trayFarHalfX + (trayNearHalfX - trayFarHalfX) * t
            val limitX = (width - halfExtent).coerceAtLeast(0f)
            val floorHeight = halfExtent * (
                abs(axis(0).y) + abs(axis(1).y) + abs(axis(2).y)
            )
            position = Vec3(
                position.x.coerceIn(-limitX, limitX),
                floorHeight,
                z
            )
        }
        velocity = Vec3.ZERO
        angularVelocity = Vec3.ZERO
        supported = true
        asleep = true
        calmSeconds = 0f
    }

    /** Throws the die in, with everything about the last roll forgotten. */
    fun launch(
        halfExtent: Float,
        position: Vec3,
        velocity: Vec3,
        orientation: Quat,
        angularVelocity: Vec3
    ) {
        this.halfExtent = halfExtent
        this.position = position
        this.velocity = velocity
        this.orientation = orientation
        this.angularVelocity = angularVelocity
        supported = false
        asleep = false
        calmSeconds = 0f
    }

    /**
     * Places the die flat on the floor already showing [targetValue], turned [yawRadians] about the
     * vertical so a tray of them does not look stamped out.
     */
    fun rest(halfExtent: Float, targetValue: Int, x: Float, z: Float, yawRadians: Float) {
        this.halfExtent = halfExtent
        position = Vec3(x, halfExtent, z)
        velocity = Vec3.ZERO
        angularVelocity = Vec3.ZERO
        orientation = Quat.fromAxisAngle(Vec3.UP, yawRadians) *
            Quat.rotationBetween(DieFaces.normalOf(targetValue), Vec3.UP)
        supported = true
        asleep = true
        calmSeconds = 0f
    }

    /** Drops the die off once it is calm and has something underneath it. */
    fun updateSleep(
        deltaSeconds: Float,
        linearLimit: Float,
        angularLimit: Float,
        delaySeconds: Float
    ) {
        if (asleep) return
        val calm = supported &&
            velocity.length < linearLimit &&
            angularVelocity.length < angularLimit
        calmSeconds = if (calm) calmSeconds + deltaSeconds else 0f
        if (calmSeconds < delaySeconds) return
        asleep = true
        velocity = Vec3.ZERO
        angularVelocity = Vec3.ZERO
    }

    fun wake() {
        asleep = false
        calmSeconds = 0f
    }

    private companion object {
        /** `I = (2/3) m h²` for a cube of half-extent `h`. */
        const val INERTIA_FACTOR = 2f / 3f

        /**
         * Above this many half-extents a die with nothing under it counts as off the felt.
         *
         * A cube balanced on one corner has its centre at `√3` half-extents, so anything higher than
         * this is either resting on another die — which is support, and support is checked first — or
         * in the air.
         */
        const val AIRBORNE_HEIGHT = 1.9f

        val BODY_AXES = listOf(Vec3(1f, 0f, 0f), Vec3(0f, 1f, 0f), Vec3(0f, 0f, 1f))

    }
}
