package com.byteflipper.random.domain.ball.physics

import kotlin.math.exp

/**
 * A body with position and orientation, integrated semi-implicitly.
 *
 * Deliberately mutable: the engine steps two of these 120 times a second and allocating a fresh
 * state each time would only make work for the collector.
 */
class RigidBody(
    val radius: Float,
    val mass: Float,
    var position: Vec3 = Vec3.ZERO,
    var velocity: Vec3 = Vec3.ZERO,
    var orientation: Quat = Quat.IDENTITY,
    var angularVelocity: Vec3 = Vec3.ZERO
) {

    /** Solid-sphere inertia is close enough for a tumbling die and keeps the maths trivial. */
    val inverseInertia: Float get() = 1f / (0.4f * mass * radius * radius)

    val inverseMass: Float get() = 1f / mass

    fun applyForce(force: Vec3, deltaSeconds: Float) {
        velocity += force * (inverseMass * deltaSeconds)
    }

    fun applyImpulse(impulse: Vec3) {
        velocity += impulse * inverseMass
    }

    fun applyTorque(torque: Vec3, deltaSeconds: Float) {
        angularVelocity += torque * (inverseInertia * deltaSeconds)
    }

    fun applyAngularImpulse(impulse: Vec3) {
        angularVelocity += impulse * inverseInertia
    }

    /** Exponential damping, so the slow-down is the same at any step size. */
    fun damp(linearPerSecond: Float, angularPerSecond: Float, deltaSeconds: Float) {
        velocity *= exp(-linearPerSecond * deltaSeconds)
        angularVelocity *= exp(-angularPerSecond * deltaSeconds)
    }

    fun integrate(deltaSeconds: Float) {
        position += velocity * deltaSeconds
        orientation = orientation.integrated(angularVelocity, deltaSeconds)
    }

    /** Resets to the middle of the cavity, at rest. */
    fun reset(position: Vec3 = Vec3.ZERO, orientation: Quat = Quat.IDENTITY) {
        this.position = position
        this.velocity = Vec3.ZERO
        this.orientation = orientation
        this.angularVelocity = Vec3.ZERO
    }

    /** Guards against a NaN escaping into the renderer after an extreme step. */
    fun sanitize() {
        if (!position.isFinite || !velocity.isFinite) {
            reset(orientation = orientation)
        }
        if (!angularVelocity.isFinite) angularVelocity = Vec3.ZERO
    }
}
