package com.byteflipper.random.domain.dice.physics

import com.byteflipper.random.domain.physics.SimulationQualityTier
import kotlin.math.sqrt

/**
 * Every number the dice simulation leans on, in one place.
 *
 * Lengths are in tray units: the floor runs from [TRAY_NEAR_HALF_X] to [TRAY_FAR_HALF_X] across and
 * [TRAY_HALF_Z] to the far and near edges. A die is around a unit on a side, so the tray remains a
 * handful of dice wide however many are in play.
 *
 * The motion is art-directed rather than scaled from a real 16 mm die. Scaled properly, gravity here
 * would be some six hundred units per second squared and a throw would be over in half a second —
 * which on a phone reads as a glitch rather than a roll. These numbers land it at about a second and
 * a half, which is long enough to watch and short enough not to wait for.
 */
data class DiceEngineTuning(
    /** Which tier these numbers came from, so the frame meter knows what it is measuring. */
    val tier: SimulationQualityTier,
    /**
     * Sweeps the contact solver makes per substep. More means firmer piles and less sink where dice
     * rest on each other; this is the simulation's most expensive knob by a distance.
     */
    val solverIterations: Int,
    val maxSubstepsPerFrame: Int,
    /** Whether the floor's contact shadows are worth their cost at this tier. */
    val softShadows: Boolean
) {

    companion object {
        /** Physics step; the engine accumulates real time and consumes it in chunks this size. */
        const val FIXED_STEP_SECONDS = 1f / 120f

        /** Most dice the tray will ever hold, and the size every array here is cut for. */
        const val MAX_DICE = 10

        /**
         * Half-extents of the tray floor. The far edge is deliberately wider than the near edge, so
         * the tray opens towards the top of the screen instead of looking like a perspective funnel.
         *
         * Taller than it is wide, because the screen is: a square tray framed on a phone leaves a band
         * of empty floor down both long sides, and every unit of tray that has to fit across the narrow
         * axis is a unit the camera has to back away to see. A portrait tray fills the viewport, which
         * is the same as saying the dice come out as large as they can.
         */
        const val TRAY_NEAR_HALF_X = 1.7f
        const val TRAY_FAR_HALF_X = 2.55f
        const val TRAY_HALF_Z = 4.45f

        /** Half-width of the trapezoid at world-space [z] (far is negative, near is positive). */
        fun trayHalfWidth(z: Float): Float {
            val t = ((z + TRAY_HALF_Z) / (2f * TRAY_HALF_Z)).coerceIn(0f, 1f)
            return TRAY_FAR_HALF_X + (TRAY_NEAR_HALF_X - TRAY_FAR_HALF_X) * t
        }

        /**
         * How high the invisible walls reach, so no shake can throw a die out of the tray.
         *
         * Low enough to stay in shot. A lid above the top of the screen sounds harmless until
         * something pins a die to it, and a die held where the camera cannot see it is indistinguishable
         * from a die that has vanished.
         */
        const val TRAY_CEILING = 8f

        /**
         * Half-extent of a die when [count] of them are in play.
         *
         * Fewer dice means bigger dice, the way a photograph of two dice is framed closer than one of
         * ten. Measured against the narrow axis, since that is the one that runs out first. The lower
         * bound keeps ten of them readable and the upper bound keeps one of them from filling the tray.
         *
         * The density formula is the old readable baseline. The count multiplier preserves the
         * requested relative sizing between small and large sets, while [DIE_SIZE_REDUCTION] scales
         * every count down uniformly. The engine chooses grid columns so bodies begin without overlap.
         */
        fun dieHalfExtent(count: Int): Float {
            val base = (TRAY_NEAR_HALF_X /
                (2.5f * sqrt(count.coerceAtLeast(1).toFloat())))
                .coerceIn(0.15f, 0.43f)
            val scale = when (count) {
                in 7..10 -> 2f
                6 -> 1.8f
                else -> 1.6f
            }
            return base * scale / DIE_SIZE_REDUCTION
        }

        /** Uniform reduction of the rendered body, collider, shadow and touch target for every count. */
        const val DIE_SIZE_REDUCTION = 1.5f

        /** Downward pull in tray units per second squared. */
        const val GRAVITY = 30f

        /**
         * How much of the phone's own acceleration the dice feel, as a fraction of gravity, and the
         * ceiling on the reading itself, in g.
         *
         * Modest on purpose. A jolt reads as several g — a haptic tick alone can — and handed over at
         * face value it would cancel gravity for as long as it lasted. What the dice should get from
         * being moved is a nudge, not a change of which way is down.
         */
        const val MOTION_GRAVITY_SCALE = 0.4f
        const val MOTION_MAX_G = 1.6f

        /**
         * The least of gravity that always survives whatever the phone is doing, as a fraction of it.
         *
         * The floor is the one thing the dice may never be argued out of. Without this a shake — or
         * the haptic tick a landing fires, which the accelerometer duly reports — can add up to more
         * than gravity and turn the tray over, and a die that falls upwards ends the roll pinned under
         * the lid where the camera cannot see it.
         */
        const val MIN_GRAVITY_FRACTION = 0.35f

        /**
         * How fast an acceleration reading fades when the sensor goes quiet (1/s). The sensor only
         * reports on change, so without this a single jolt would push the dice forever.
         */
        const val MOTION_DECAY = 7f

        /**
         * Air drag on a die in flight (1/s), linear and angular.
         *
         * Barely there. Drag is what a simulation reaches for when its dice will not settle, and it
         * pays for that by making every throw look like it is happening in syrup: the tumble slows
         * before it lands, the bounce goes nowhere, and the die arrives already half asleep. Friction
         * against the felt is what stops these dice, and it only gets to do that if they arrive moving.
         */
        const val LINEAR_DAMPING = 0.12f
        const val ANGULAR_DAMPING = 0.22f

        /**
         * Bounce and grip against the felt, and against the walls.
         *
         * The felt gives a little and holds; the walls are the sides of a box, so a die comes off one
         * livelier than it went in and with far less to catch on. Between them they are most of what a
         * throw looks like: a die that lands, kicks, catches the far wall and rattles back is doing
         * nothing more than these four numbers.
         *
         * Real coefficients, and they behave like them: the solver holds a step's friction to what this
         * much grip could have applied rather than reapplying it every sweep, so a die that arrives
         * moving skids before the felt has it, the way a die thrown across a table does.
         */
        const val FLOOR_RESTITUTION = 0.3f
        const val FLOOR_FRICTION = 0.72f
        const val WALL_RESTITUTION = 0.42f
        const val WALL_FRICTION = 0.3f

        /** Bounce and grip between two dice. */
        const val DIE_RESTITUTION = 0.28f
        const val DIE_FRICTION = 0.5f

        /** Under this closing speed a contact is a rest rather than a bounce. */
        const val RESTITUTION_THRESHOLD = 1f

        /**
         * Overlap the solver is content to leave (tray units), and how much of what is left over it
         * takes out per step.
         *
         * Correcting overlap all the way to zero every step is what makes a resting pile buzz: the
         * push-out overshoots, gravity puts it back, and the two argue at the step rate. Leaving a
         * hair of it alone costs nothing anyone can see and is the difference between a pile that sits
         * and a pile that hums.
         */
        const val CONTACT_SLOP = 0.006f
        const val CONTACT_BIAS = 0.14f

        /** Cap on how fast overlap is pushed out, so a deep one cannot fire a die across the tray. */
        const val MAX_CORRECTION_SPEED = 2.4f

        /** Quiet positional repair left after the impulse solve; unlike velocity bias it adds no bounce. */
        const val POSITION_CORRECTION_FRACTION = 0.08f
        const val MAX_POSITION_CORRECTION = 0.03f

        /** Contacts one pair of dice may produce; a face flat against a face needs four. */
        const val MAX_CONTACTS_PER_PAIR = 4

        /** Speed ceilings, which is also what keeps one step from moving a die past a wall. */
        const val MAX_LINEAR_SPEED = 26f
        const val MAX_ANGULAR_SPEED = 42f

        /** Smallest collision worth reporting, and the publication limit for one contact manifold. */
        const val IMPACT_EVENT_MIN_SPEED = 1f
        const val IMPACT_EVENT_INTERVAL = 0.035f

        /**
         * Tap throw timing: a deliberate slow dip, then a much shorter return stroke.
         *
         * The return speed is also used as a real angular velocity at release. A die twice as far
         * from the near hinge therefore receives twice the tangential lift instead of every die being
         * handed the same canned velocity.
         */
        const val THROW_WINDUP_DOWN_SECONDS = 0.56f
        const val THROW_WINDUP_RETURN_SECONDS = 0.105f
        const val THROW_WINDUP_SECONDS = THROW_WINDUP_DOWN_SECONDS + THROW_WINDUP_RETURN_SECONDS
        const val THROW_WINDUP_DIP_RADIANS = -0.28f
        const val THROW_BASE_LIFT_MIN = 3.4f
        const val THROW_BASE_LIFT_MAX = 4.3f
        const val THROW_RETURN_ANGULAR_SPEED = 0.92f
        const val THROW_FORWARD_MIN = 0.35f
        const val THROW_FORWARD_MAX = 1.15f
        const val THROW_OUTWARD_SPEED = 0.8f
        const val THROW_SCATTER = 0.55f
        const val THROW_SPIN_MIN = 18f
        const val THROW_SPIN_MAX = 32f

        /** Dice across the tray at entry and at rest; beyond this the grid grows down its long axis. */
        const val GRID_MAX_COLUMNS = 4

        /** Hard stop for a wedged or numerically noisy body. Orientation is never changed here. */
        const val ROLL_TIMEOUT_SECONDS = 6.5f

        /**
         * Speeds under which a supported die is done, how long it has to hold them, and the closing
         * speed at which a neighbour's arrival wakes it again.
         *
         * A sleeping die is immovable as far as the solver is concerned, which is what lets a pile of
         * them hold still instead of slowly sinking into each other. Only dice with something under
         * them may sleep — otherwise one would fall asleep at the top of its bounce and hang there.
         */
        const val SLEEP_LINEAR_SPEED = 0.13f
        const val SLEEP_ANGULAR_SPEED = 0.38f
        const val SLEEP_DELAY_SECONDS = 0.32f
        const val WAKE_SPEED = 1.05f

        /** Speed-dependent impulses applied by the two measured phone-toss gestures. */
        const val GESTURE_LIFT_MIN = 4.8f
        const val GESTURE_LIFT_MAX = 12.8f
        const val GESTURE_SPIN_MIN = 13f
        const val GESTURE_SPIN_MAX = 34f
        const val GESTURE_SCATTER_MIN = 0.25f
        const val GESTURE_SCATTER_MAX = 1.15f

        fun forTier(tier: SimulationQualityTier): DiceEngineTuning = when (tier) {
            SimulationQualityTier.HIGH -> DiceEngineTuning(
                tier = tier,
                solverIterations = 12,
                maxSubstepsPerFrame = 5,
                softShadows = true
            )
            SimulationQualityTier.BALANCED -> DiceEngineTuning(
                tier = tier,
                solverIterations = 9,
                maxSubstepsPerFrame = 4,
                softShadows = true
            )
            SimulationQualityTier.BATTERY -> DiceEngineTuning(
                tier = tier,
                solverIterations = 6,
                maxSubstepsPerFrame = 3,
                softShadows = false
            )
        }
    }
}
