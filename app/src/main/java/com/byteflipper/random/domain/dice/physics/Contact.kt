package com.byteflipper.random.domain.dice.physics

import com.byteflipper.random.domain.physics.Vec3

/**
 * One point where two things touch.
 *
 * [normal] is the direction die [a] has to travel along to come free, so the solver pushes [a] along
 * it and [b] against it. For a contact with the tray that is simply the wall's inward normal.
 */
class Contact {

    var a: Int = 0

    /** The other die, or -1 when the other side of the contact is the tray itself. */
    var b: Int = -1

    var point: Vec3 = Vec3.ZERO
    var normal: Vec3 = Vec3.UP
    var penetration: Float = 0f
    var restitution: Float = 0f
    var friction: Float = 0f

    /** Scratch the solver fills in once per step, so its sweeps stay arithmetic. */
    var normalMass: Float = 0f
    var bounce: Float = 0f
    var normalImpulse: Float = 0f

    /**
     * Friction's running total for the step, as a vector in the contact plane.
     *
     * Kept, rather than worked out afresh each sweep, because Coulomb's limit is a limit on the whole
     * step's friction and not on each sweep's share of it. A sweep that cancelled the slide outright
     * and then had six more chances to do it again would grip a skidding die several times harder than
     * the felt it is sliding on ever could.
     */
    var tangentImpulse: Vec3 = Vec3.ZERO

    /** How fast the two sides were closing when the step began; what haptics listen to. */
    var approachSpeed: Float = 0f
}

/**
 * The step's contacts, in an array that is reused rather than rebuilt.
 *
 * Ten dice come to forty-five pairs and a few hundred contacts, every step, at a hundred and twenty
 * steps a second. Allocating those would hand the collector more garbage than the rest of the app
 * produces in a session.
 */
class ContactBuffer(capacity: Int = 512) {

    private var contacts = Array(capacity) { Contact() }

    var size: Int = 0
        private set

    operator fun get(index: Int): Contact = contacts[index]

    fun clear() {
        size = 0
    }

    /** The next contact to fill in, growing the array if a busy pile ever outruns it. */
    fun next(): Contact {
        if (size == contacts.size) {
            val previous = contacts
            contacts = Array(previous.size * 2) { index ->
                previous.getOrElse(index) { Contact() }
            }
        }
        return contacts[size++]
    }
}
