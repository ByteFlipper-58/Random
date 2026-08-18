package com.byteflipper.random.domain.ball.physics

import com.byteflipper.random.domain.physics.SimulationQualityTier
import com.byteflipper.random.domain.physics.Vec3

/**
 * Every number the ball's simulation leans on, in one place.
 *
 * Lengths are in shell units: the shell's outer surface sits at 1.0, so the cavity is a hair
 * smaller. The motion is art-directed rather than physically scaled — a real 8-ball's fluid is
 * thick, and gravity in these units is chosen to match how slowly the die actually drifts.
 */
data class BallEngineTuning(
    /** Which tier these numbers came from, so the frame meter knows what it is measuring. */
    val tier: SimulationQualityTier,
    val particleCount: Int,
    val bubbleCount: Int,
    val maxSubstepsPerFrame: Int,
    /** Ray-march steps the interior shader may spend looking for the liquid surface. */
    val marchSteps: Int,
    /**
     * Probes across the light that the die's shadow in the liquid is sampled with, 1..5.
     *
     * Each one is a full test against the die's twenty planes, so this is the shader's second most
     * expensive knob after the march. One probe is a hard-edged shadow, five a soft one.
     */
    val shadowProbes: Int
) {

    companion object {
        /** Physics step; the engine accumulates real time and consumes it in chunks this size. */
        const val FIXED_STEP_SECONDS = 1f / 120f

        /**
         * Direction the answer window faces in the shell's *own* frame — tilted slightly up so it
         * sits where a hand would hold the ball. At rest the shell is unrotated, so this is also
         * where the window points in view space, and it is the axis the die docks against.
         *
         * The "8" badge is printed on the exact opposite side, so spinning the shell carries the two
         * around together through a full turn.
         */
        val WINDOW_AXIS = Vec3(0f, 0.12f, 1f).normalized()

        /** Half-angle of the window opening, in degrees. */
        const val WINDOW_HALF_ANGLE_DEGREES = 25f

        /** Where the bevel around the window starts. */
        const val WINDOW_BEVEL_HALF_ANGLE_DEGREES = 29.5f

        /**
         * Gravity leans away from the camera as well as down, so buoyancy carries the die up *and*
         * towards the window. Real 8-balls are held facing up; a phone is not, and this is what
         * makes the answer surface where the player is looking.
         */
        val GRAVITY_DEPTH_BIAS = -0.38f

        /** Gravity direction with no sensor reading: straight down the screen, leaning back. */
        fun defaultGravity(): Vec3 = Vec3(0f, -1f, GRAVITY_DEPTH_BIAS).normalized()

        /** Inner radius of the glass cavity. */
        const val CAVITY_RADIUS = 0.86f

        /**
         * Distance from the die's centre to a vertex. Big enough that a face fills most of the
         * window: the answer is the point of the whole feature, and a smaller die printed it too
         * small to read comfortably.
         */
        const val DIE_CIRCUMRADIUS = 0.50f

        /**
         * Fraction of the cavity filled with liquid. Nearly full, the way a real 8-ball is — just
         * enough air left for the bubbles to break the surface into.
         */
        const val FLUID_FILL = 0.96f

        /** Downward pull in shell units per second squared. */
        const val GRAVITY = 9.0f

        /** Die density relative to the liquid; below 1.0 so it drifts up to the window. */
        const val DIE_RELATIVE_DENSITY = 0.86f

        /** Linear drag inside the liquid (1/s) — the syrupy part of the look. */
        const val FLUID_LINEAR_DRAG = 3.4f

        /** Quadratic drag inside the liquid, which keeps a hard shake from launching the die. */
        const val FLUID_QUADRATIC_DRAG = 1.9f

        /** Linear drag in the air pocket above the liquid. */
        const val AIR_LINEAR_DRAG = 0.35f

        const val FLUID_ANGULAR_DRAG = 4.2f
        const val AIR_ANGULAR_DRAG = 0.5f

        /** Bounce and slide against the cavity wall. */
        const val WALL_RESTITUTION = 0.22f
        const val WALL_FRICTION = 0.34f

        /** Above this impact speed a wall hit is worth a haptic tick. */
        const val IMPACT_HAPTIC_SPEED = 1.1f

        /** Shell spin damping (1/s) once the finger lets go. */
        const val SHELL_ANGULAR_DRAG = 2.2f

        /** Cap on shell spin so a frantic shake stays readable. */
        const val SHELL_MAX_ANGULAR_SPEED = 14f

        /**
         * Cap on the spin a flick can leave behind, well under [SHELL_MAX_ANGULAR_SPEED].
         *
         * A finger can report several thousand pixels a second, which on a small screen is many turns
         * a second. This is where that lands instead. The shake and the window's own alignment still
         * get the full range.
         */
        const val SHELL_MAX_FLING_ANGULAR_SPEED = 9f

        /**
         * How much of a flick's own speed becomes shell spin.
         *
         * One means the surface leaves the finger at exactly the speed it was moving, which is the
         * only figure that reads as *letting go* of something rather than throwing it.
         */
        const val FLING_SPIN_SCALE = 1f

        /** Under this a release is just a release: the finger had stopped, so the ball stops with it. */
        const val FLING_MIN_ANGULAR_SPEED = 0.7f

        /** How long one flick stays "recent" (s); flicks only stack while they keep coming. */
        const val FLING_STREAK_WINDOW = 0.9f

        /** Flicks worth of stacking, past which more flicking changes nothing. */
        const val FLING_STREAK_MAX = 3f

        /** Extra spin per flick already in the streak, as a fraction. */
        const val FLING_STREAK_SPEED_GAIN = 0.22f

        /** How much longer each flick in the streak makes the ball coast, as a fraction of the drag. */
        const val FLING_STREAK_COAST_GAIN = 0.4f

        /**
         * How much of the spin survives a finger landing on a moving ball.
         *
         * Not all of it, because a hand on the shell really is a brake — and not none, because that is
         * what lets a second flick build on the first rather than starting over.
         */
        const val GRAB_SPIN_RETENTION = 0.55f

        /**
         * How fast the spin the *liquid* feels catches up with the finger (1/s).
         *
         * The shell itself follows the finger exactly; this only smooths the velocity the fluid and
         * the agitation are driven by, so a stuttering touch stream does not read as a stuttering
         * slosh.
         */
        const val DRAG_VELOCITY_RESPONSE = 24f

        /** How much of the shell's spin the liquid carries the die around with, as a fraction. */
        const val SHELL_DIE_COUPLING = 0.4f

        /** How quickly that coupling takes hold (1/s) — viscous, so it is a lag rather than a snap. */
        const val SHELL_DIE_COUPLING_RATE = 1.6f

        /**
         * How much of the device's own acceleration the contents feel, as a fraction. A phone shaken
         * in anger passes 3 g, and letting all of that through would fire the die into the glass
         * every time; this keeps the liquid lively and the die inside the ball.
         */
        const val MOTION_GRAVITY_SCALE = 0.6f

        /** Ceiling on the reported device acceleration, in g. */
        const val MOTION_MAX_G = 3f

        /**
         * How fast an acceleration reading fades when the sensor goes quiet (1/s). The sensor only
         * reports on change, so without this a single jolt would push the liquid forever.
         */
        const val MOTION_DECAY = 7f

        /**
         * How quickly the liquid's surface plane follows a change in effective gravity (1/s). The
         * lag is the slosh: shake the phone and the surface swings after the motion, not with it.
         */
        const val SLOSH_RESPONSE = 7.5f

        /** How far the whole ball lags behind the phone, in shell units per g. */
        const val SHELL_OFFSET_PER_G = 0.12f

        /** Spring pulling the ball back to the middle of the screen, and its damping (1/s²,1/s). */
        const val SHELL_OFFSET_STIFFNESS = 95f
        const val SHELL_OFFSET_DAMPING = 9.5f

        /** Hard cap on that lag, so the ball can never wander off the screen. */
        const val SHELL_MAX_OFFSET = 0.17f

        /**
         * PD gains that turn the shell until the window faces the camera again. Whatever the player
         * spun the ball to, an answer always ends up readable.
         */
        const val WINDOW_ALIGN_PROPORTIONAL_GAIN = 17f
        const val WINDOW_ALIGN_DERIVATIVE_GAIN = 6f

        /** The hold once answered is gentler, so a swipe can still turn the ball a little. */
        const val WINDOW_ALIGN_HOLD_SCALE = 0.45f

        /**
         * PD gains that steer the chosen face to the window during the reveal.
         *
         * Deliberately under-damped — critical for this proportional gain would be about ten. The die
         * has mass, so it should arrive with a slight overshoot and rock into place rather than glide
         * to a mathematical stop.
         */
        const val REVEAL_PROPORTIONAL_GAIN = 26f
        const val REVEAL_DERIVATIVE_GAIN = 5f

        /**
         * The nudge into the glass and the spin the die is given the moment the answer lands, so the
         * hold's own spring rocks it a couple of times instead of simply switching off.
         */
        const val SETTLE_PRESS = 0.34f
        const val SETTLE_ROCK = 0.95f

        /** The reveal is done once the face is this close to the window axis. */
        const val REVEAL_TOLERANCE_RADIANS = 0.06f

        /** Hard stop so a wedged die can never hang the phase machine. */
        const val REVEAL_TIMEOUT_SECONDS = 6f

        /** Below this the scene counts as settled and the renderer may throttle. */
        const val SETTLED_SPEED = 0.05f

        /** Largest particle count any tier asks for; the fluid's arrays are sized for it once. */
        const val MAX_PARTICLES = 384

        /** Largest bubble count any tier asks for, and the size of the shader's uniform array. */
        const val MAX_BUBBLES = 12

        /** Side of the cubic density grid handed to the interior shader. */
        const val DENSITY_RESOLUTION = 32

        /**
         * Kernel support as a multiple of the rest spacing between particles. Just under two, which
         * leaves each particle with roughly thirty neighbours whatever the tier's count is.
         */
        const val SPH_SUPPORT_SCALE = 1.93f

        /** Radius a particle keeps from the glass and from the die. */
        const val PARTICLE_RADIUS = 0.035f

        /**
         * Pressure stiffness. Low for SPH — the liquid is meant to be soft and slow, and the bulk
         * level comes from [BallCavity]'s analytic plane, so the particles only add the splashing.
         */
        const val SPH_STIFFNESS = 45f

        /** XSPH velocity smoothing, which stands in for viscosity and cannot destabilise the step. */
        const val SPH_VISCOSITY = 0.18f

        /** Particle drag (1/s). High: this is what makes the liquid look like syrup. */
        const val SPH_LINEAR_DRAG = 2.4f

        /** Hard cap on particle speed, so no single step can move one further than the kernel. */
        const val SPH_MAX_SPEED = 3.2f

        const val SPH_WALL_RESTITUTION = 0.08f
        const val SPH_WALL_FRICTION = 0.3f

        /** How much of the particles' push against the die actually reaches it. */
        const val SPH_DIE_COUPLING = 0.4f

        /** How strongly the spinning shell drags the liquid next to the glass along with it. */
        const val SPH_SHELL_COUPLING = 2.4f

        /** Splat radius as a multiple of the rest spacing, i.e. how lumpy the density field looks. */
        const val SPLAT_RADIUS_SCALE = 1.15f

        /** Volume fraction that counts as the liquid surface; the field is a fraction, so a half. */
        const val FLUID_ISO_LEVEL = 0.5f

        /** How fast a bubble climbs through the liquid, in shell units per second. */
        const val BUBBLE_RISE_SPEED = 0.26f

        /** Cap on the die's own spin. High enough that a hard shake really tumbles it. */
        const val DIE_MAX_ANGULAR_SPEED = 34f

        /** Cap on the die's speed through the liquid. */
        const val DIE_MAX_SPEED = 6f

        /** Impulses a shake hands to the ball, the die's motion, and the die's spin. */
        const val SHAKE_SHELL_IMPULSE = 2.4f
        const val SHAKE_LINEAR_IMPULSE = 2.8f
        const val SHAKE_ANGULAR_IMPULSE = 16f

        /**
         * How agitated the contents are, 0..1, tracked from the device's motion and the shell's spin.
         * It rises almost immediately and falls over about a second, so the die keeps thrashing for a
         * moment after the shake rather than stopping dead with the phone.
         */
        const val AGITATION_ATTACK = 12f
        const val AGITATION_DECAY = 1.6f

        /** Device acceleration, in g, that counts as fully agitated. */
        const val AGITATION_FULL_G = 1.4f

        /** Random torque and buffeting a fully agitated liquid applies to the die, per second. */
        const val AGITATION_TORQUE = 46f
        const val AGITATION_JITTER = 7f

        /**
         * The liquid clouds up when the die drives for the window and clears again over about two
         * seconds, so the answer develops out of the blue instead of sliding into view. Not slower
         * than that: the renderer cannot throttle while it is still clearing.
         */
        const val TURBIDITY_ON_REVEAL = 1f
        const val TURBIDITY_DECAY = 1.1f

        /** How much of the agitation reads as fizz — a cloud of fine bubbles under the surface. */
        const val FIZZ_FROM_AGITATION = 1.15f

        /**
         * How fast the film left on the glass above the waterline drains away (1/s).
         *
         * Slower than the churn that put it there — that lag is the whole point, since a shake is
         * over in a moment and wet glass is not. Not much slower, though: the drips are animating, so
         * the renderer cannot throttle until they have gone.
         */
        const val WETNESS_DECAY = 1f

        fun forTier(tier: SimulationQualityTier): BallEngineTuning = when (tier) {
            SimulationQualityTier.HIGH -> BallEngineTuning(
                tier = tier,
                particleCount = 384,
                bubbleCount = 12,
                maxSubstepsPerFrame = 4,
                marchSteps = 64,
                shadowProbes = 5
            )
            SimulationQualityTier.BALANCED -> BallEngineTuning(
                tier = tier,
                particleCount = 256,
                bubbleCount = 8,
                maxSubstepsPerFrame = 3,
                marchSteps = 44,
                shadowProbes = 3
            )
            SimulationQualityTier.BATTERY -> BallEngineTuning(
                tier = tier,
                particleCount = 128,
                bubbleCount = 5,
                maxSubstepsPerFrame = 2,
                marchSteps = 28,
                shadowProbes = 1
            )
        }
    }
}
