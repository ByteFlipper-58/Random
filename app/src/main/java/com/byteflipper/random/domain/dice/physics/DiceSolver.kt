package com.byteflipper.random.domain.dice.physics

import com.byteflipper.random.domain.physics.Vec3
import com.byteflipper.random.domain.dice.physics.DiceEngineTuning.Companion.CONTACT_BIAS
import com.byteflipper.random.domain.dice.physics.DiceEngineTuning.Companion.CONTACT_SLOP
import com.byteflipper.random.domain.dice.physics.DiceEngineTuning.Companion.MAX_CORRECTION_SPEED
import com.byteflipper.random.domain.dice.physics.DiceEngineTuning.Companion.MAX_POSITION_CORRECTION
import com.byteflipper.random.domain.dice.physics.DiceEngineTuning.Companion.POSITION_CORRECTION_FRACTION
import com.byteflipper.random.domain.dice.physics.DiceEngineTuning.Companion.RESTITUTION_THRESHOLD
import kotlin.math.max
import kotlin.math.min

/**
 * Turns contacts into velocity changes: one contact at a time, several sweeps over the lot.
 *
 * Sequential impulses, which is the standard trade — it does not solve all the contacts at once, so a
 * tall stack settles over a few steps rather than one, but each sweep is a handful of multiplications
 * and the result is stable at any step size the tiers ask for. Each contact remembers the total normal
 * impulse it has accumulated, both to keep it from ever pulling the dice together and to give the
 * friction something to be a fraction of.
 */
class DiceSolver {

    /** Fastest closing speed in the step just prepared, for haptics to listen to. */
    var strongestImpact: Float = 0f
        private set

    var strongestImpactMaterial: DiceImpactMaterial = DiceImpactMaterial.FELT
        private set

    /**
     * Wakes dice that something is arriving at, before the masses are worked out.
     *
     * A sleeping die is infinitely heavy to the solver, so this has to happen first: a die that woke
     * up afterwards would spend the step being immovable and hand its neighbour a bounce off a wall.
     */
    fun wakeOnImpact(bodies: List<DiceBody>, contacts: ContactBuffer, threshold: Float) {
        for (i in 0 until contacts.size) {
            val contact = contacts[i]
            val a = bodies[contact.a]
            val b = if (contact.b >= 0) bodies[contact.b] else null
            if (a.asleep && (b == null || b.asleep)) continue
            if (closingSpeed(a, b, contact.point, contact.normal) <= threshold) continue
            if (a.asleep) a.wake()
            if (b != null && b.asleep) b.wake()
        }
    }

    /** Works out each contact's effective mass and how much of a bounce it has earned. */
    fun prepare(bodies: List<DiceBody>, contacts: ContactBuffer) {
        strongestImpact = 0f
        strongestImpactMaterial = DiceImpactMaterial.FELT
        for (i in 0 until contacts.size) {
            val contact = contacts[i]
            val a = bodies[contact.a]
            val b = if (contact.b >= 0) bodies[contact.b] else null

            val closing = closingSpeed(a, b, contact.point, contact.normal)
            contact.approachSpeed = closing
            if (closing > strongestImpact) {
                strongestImpact = closing
                strongestImpactMaterial = when {
                    contact.b >= 0 -> DiceImpactMaterial.DICE
                    contact.normal.y > FLOOR_NORMAL_THRESHOLD -> DiceImpactMaterial.FELT
                    else -> DiceImpactMaterial.RIM
                }
            }

            // Slow contacts get no bounce at all, or a die would never stop trembling on the felt.
            contact.bounce =
                if (closing > RESTITUTION_THRESHOLD) closing * contact.restitution else 0f

            val mass = effectiveMass(a, b, contact.point, contact.normal)
            contact.normalMass = if (mass > Vec3.EPSILON) 1f / mass else 0f
            contact.normalImpulse = 0f
            contact.tangentImpulse = Vec3.ZERO
        }
    }

    /** Sweeps every contact [iterations] times, so pushes have a chance to reach along a pile. */
    fun solve(
        bodies: List<DiceBody>,
        contacts: ContactBuffer,
        iterations: Int,
        deltaSeconds: Float
    ) {
        repeat(iterations) {
            for (i in 0 until contacts.size) {
                solveContact(bodies, contacts[i], deltaSeconds)
            }
        }
    }

    /**
     * Removes a conservative share of the remaining overlap directly, after impulses are solved.
     *
     * Velocity bias alone turns deep overlap into separating speed, which looks like an unexplained
     * bounce and keeps stacks humming. This small split correction changes position without injecting
     * kinetic energy; four face contacts together still repair less than half the overlap per step.
     */
    fun correctPositions(bodies: List<DiceBody>, contacts: ContactBuffer) {
        for (i in 0 until contacts.size) {
            val contact = contacts[i]
            val depth = max(0f, contact.penetration - CONTACT_SLOP)
            if (depth <= 0f) continue
            val a = bodies[contact.a]
            val b = if (contact.b >= 0) bodies[contact.b] else null
            val inverseA = a.inverseMass
            val inverseB = b?.inverseMass ?: 0f
            val total = inverseA + inverseB
            if (total <= Vec3.EPSILON) continue
            val magnitude = min(depth * POSITION_CORRECTION_FRACTION, MAX_POSITION_CORRECTION) / total
            val correction = contact.normal * magnitude
            a.position += correction * inverseA
            if (b != null) b.position -= correction * inverseB
        }
    }

    private fun solveContact(bodies: List<DiceBody>, contact: Contact, deltaSeconds: Float) {
        if (contact.normalMass <= 0f) return
        val a = bodies[contact.a]
        val b = if (contact.b >= 0) bodies[contact.b] else null
        val point = contact.point
        val normal = contact.normal

        // Overlap is pushed out as extra separating speed rather than by moving the dice, which keeps
        // the correction inside the solver where the accumulated impulses can keep it honest.
        val bias = min(
            CONTACT_BIAS * max(0f, contact.penetration - CONTACT_SLOP) / deltaSeconds,
            MAX_CORRECTION_SPEED
        )
        val separating = -closingSpeed(a, b, point, normal)
        var impulse = (contact.bounce + bias - separating) * contact.normalMass

        // A contact can only ever push. Clamping the running total rather than each step's share is
        // what lets a sweep take back too much of an earlier one without the pair sticking together.
        val total = max(0f, contact.normalImpulse + impulse)
        impulse = total - contact.normalImpulse
        contact.normalImpulse = total

        a.applyImpulse(normal * impulse, point)
        b?.applyImpulse(normal * -impulse, point)

        applyFriction(a, b, contact, point, normal)
    }

    /**
     * Coulomb friction, solved as one impulse along whichever way the contact is actually sliding
     * rather than as two along fixed axes — a die skidding diagonally is then slowed diagonally, and
     * the cone the impulse is clamped to is round instead of square.
     *
     * The total is what the cone holds, not each sweep's share of it. Solved sweep by sweep with no
     * memory, friction cancels the slide, watches the normal impulse hand some of it back, and cancels
     * it again — seven times a step, which is a felt that grips like glue and a die that stops where it
     * lands. Accumulating instead means the last sweep can take back what the first overdid, and the
     * whole step's friction is what a surface with this much grip could actually have applied.
     */
    private fun applyFriction(
        a: DiceBody,
        b: DiceBody?,
        contact: Contact,
        point: Vec3,
        normal: Vec3
    ) {
        val relative = a.pointVelocity(point) - (b?.pointVelocity(point) ?: Vec3.ZERO)
        val sliding = relative - normal * (relative dot normal)
        val speed = sliding.length
        if (speed < Vec3.EPSILON) return

        val direction = sliding * (1f / speed)
        val mass = effectiveMass(a, b, point, direction)
        if (mass <= Vec3.EPSILON) return

        // What it would take to stop the slide dead, added to what has been applied so far and then
        // held to the cone — so a slide that has changed direction mid-step is answered in the
        // direction it is going now, not the one it started in.
        val wanted = contact.tangentImpulse + direction * (-speed / mass)
        val total = wanted.clampLength(contact.friction * contact.normalImpulse)
        val delta = total - contact.tangentImpulse
        contact.tangentImpulse = total

        a.applyImpulse(delta, point)
        b?.applyImpulse(-delta, point)
    }

    /** How fast the two sides of a contact are coming together; negative when they are parting. */
    private fun closingSpeed(a: DiceBody, b: DiceBody?, point: Vec3, normal: Vec3): Float {
        val relative = a.pointVelocity(point) - (b?.pointVelocity(point) ?: Vec3.ZERO)
        return -(relative dot normal)
    }

    /**
     * Mass the pair presents along [direction] at [point] — the linear part plus what the spin each
     * side would pick up. A cube's isotropic inertia is why this is arithmetic and not a matrix.
     */
    private fun effectiveMass(a: DiceBody, b: DiceBody?, point: Vec3, direction: Vec3): Float {
        var mass = a.inverseMass + ((point - a.position) cross direction).lengthSquared * a.inverseInertia
        if (b != null) {
            mass += b.inverseMass +
                ((point - b.position) cross direction).lengthSquared * b.inverseInertia
        }
        return mass
    }

    private companion object {
        const val FLOOR_NORMAL_THRESHOLD = 0.62f
    }
}
