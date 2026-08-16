package com.byteflipper.random.domain.ball.physics

/** What the UI can ask the simulation to do. Submitted from any thread, applied on the GL thread. */
sealed interface BallCommand {

    /**
     * A finger has landed on the ball and taken hold of the shell.
     *
     * Until it lets go the shell turns by exactly what [Drag] reports and nothing else may steer it.
     */
    data object Grab : BallCommand

    /**
     * How far the surface under the finger has just travelled, in radians of shell rotation.
     *
     * Pixels are converted by the view, because only the view knows how big the ball is on screen —
     * and that ratio is the whole reason the surface keeps up with the finger exactly.
     */
    data class Drag(val dxRadians: Float, val dyRadians: Float) : BallCommand

    /**
     * The finger has left, at [dxRadiansPerSecond] by [dyRadiansPerSecond]. That speed is the spin the
     * ball keeps coasting on; a release with no flick in it leaves nothing behind.
     */
    data class Fling(val dxRadiansPerSecond: Float, val dyRadiansPerSecond: Float) : BallCommand

    /** Device gravity in view space, already normalised by the sensor layer. */
    data class Tilt(val gravity: Vec3) : BallCommand

    /**
     * How hard the device itself is being moved right now, in g and in view space, gravity already
     * taken out. This is what makes the liquid slosh and the whole ball lag behind the phone.
     */
    data class Motion(val acceleration: Vec3) : BallCommand

    /** A shake, with [magnitude] in multiples of g above the shake threshold. */
    data class Shake(val magnitude: Float) : BallCommand

    /** Start a reveal that has to end with [faceIndex] facing the window. */
    data class Ask(val faceIndex: Int) : BallCommand

    /** Swap the quality tier. Goes through the queue so the fluid is only ever touched by one thread. */
    data class Retune(val tuning: BallEngineTuning) : BallCommand

    /** Back to a calm idle state. */
    data object Reset : BallCommand
}

/** Things the simulation wants the UI to react to. */
sealed interface BallEngineEvent {

    /** The die hit the glass; [strength] is 0..1 and drives the haptic tick. */
    data class Impact(val strength: Float) : BallEngineEvent

    /** The chosen face has settled in the window. */
    data object Revealed : BallEngineEvent
}

enum class BallEnginePhase {
    /** Nothing asked yet: the die drifts with gravity. */
    IDLE,

    /** The die has been shoved away from the window and is tumbling. */
    ASKING,

    /** The controller is steering the chosen face onto the window axis. */
    REVEALING,

    /** The answer is docked in the window. */
    ANSWERED
}

/**
 * The liquid as the interior shader needs it: a coarse volume-fraction grid and the bubbles.
 *
 * These are the engine's own buffers rather than copies — 32 KB a frame is not worth duplicating.
 * They are refilled by the next [BallEngine.advance], so they have to be uploaded during the same
 * frame that read the snapshot. That holds by construction: stepping and drawing both happen on the
 * GL thread inside `onDrawFrame`.
 */
class FluidFrame(
    /** Side of the cubic grid; it spans the whole cavity, from `-radius` to `+radius` on each axis. */
    val resolution: Int,
    /** Volume fraction per voxel, 0..255, ordered with x fastest and z slowest. */
    val density: ByteArray,
    /** Bubble centre and radius, four floats each. */
    val bubbles: FloatArray,
    val bubbleCount: Int,
    /**
     * How far the shader should read a volume-fraction difference as, in world units. It follows the
     * particle size, so the waves keep their shape when the tier changes the particle count.
     */
    val fieldScale: Float
)

/**
 * One consistent frame of simulation state, published for the GL thread.
 *
 * Immutable on purpose: the renderer grabs the latest instance once per frame and can read it at
 * leisure while the engine is already building the next one. [fluid] is the one exception, and it
 * says so.
 */
data class BallSnapshot(
    val phase: BallEnginePhase,
    val shellOrientation: Quat,
    /**
     * How far the whole ball has lagged behind the phone, in shell units and view space. Move the
     * device down and this goes up: the ball is heavy and the screen is not.
     */
    val shellOffset: Vec3,
    val diePosition: Vec3,
    val dieOrientation: Quat,
    /** Unit "up" in view space, i.e. the direction buoyancy pushes. */
    val up: Vec3,
    /** Signed distance from the cavity centre to the liquid surface along [up]. */
    val fluidSurfaceOffset: Float,
    /** How much of the die is under the liquid, 0..1. */
    val dieSubmersion: Float,
    /**
     * How stirred up the contents are, 0..1. Rises with the shake and falls over about a second, so
     * the liquid keeps fizzing for a moment after the phone has stopped.
     */
    val agitation: Float,
    /**
     * How cloudy the liquid is, 0..1. Spikes when the die sets off for the window so the answer
     * develops out of the blue rather than sliding into view, then clears.
     */
    val turbidity: Float,
    /**
     * How wet the glass above the waterline is, 0..1. A shake soaks it and the film then drains back
     * towards the surface over a couple of seconds.
     */
    val wetness: Float,
    /** True once nothing is moving enough to be worth a full-rate frame. */
    val settled: Boolean,
    /** The liquid, or null before the first step has filled it. */
    val fluid: FluidFrame? = null
) {
    companion object {
        val Initial = BallSnapshot(
            phase = BallEnginePhase.IDLE,
            shellOrientation = Quat.IDENTITY,
            shellOffset = Vec3.ZERO,
            diePosition = Vec3.ZERO,
            dieOrientation = Quat.IDENTITY,
            up = -BallEngineTuning.defaultGravity(),
            fluidSurfaceOffset = 0f,
            dieSubmersion = 1f,
            agitation = 0f,
            turbidity = 0f,
            wetness = 0f,
            settled = true,
            fluid = null
        )
    }
}
