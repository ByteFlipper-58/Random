package com.byteflipper.random.domain.ball.physics

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The ball's simulation: the shell's spin, the die floating in the liquid, and the phase machine
 * that turns a chosen answer into a face settling in the window.
 *
 * [advance] runs on the GL thread with a fixed step; commands arrive from the UI thread through a
 * queue and state goes back out through an immutable snapshot, so neither side ever blocks.
 *
 * Everything lives in *view* space. The cavity is a sphere, so it does not care how the shell is
 * turned; the window is a fixed direction in the shell's own frame, and the renderer rotates it by
 * [BallSnapshot.shellOrientation] to find out where it points on screen.
 *
 * The device's own movement is part of the physics: it is folded into gravity so the liquid sloshes
 * and the die is thrown about, and it drags the whole ball off centre so the shell visibly lags
 * behind the phone.
 */
class BallEngine(
    tuning: BallEngineTuning = BallEngineTuning.forTier(BallQualityTier.BALANCED),
    private val random: Random = Random.Default
) {

    @Volatile
    var tuning: BallEngineTuning = tuning
        private set

    private val cavity = BallCavity()
    private val die = RigidBody(radius = BallEngineTuning.DIE_CIRCUMRADIUS, mass = 1f)
    private val fluid = SphFluid(random = random)
    private val bubbles = BubbleSwarm(random = random)

    private val commands = ConcurrentLinkedQueue<BallCommand>()
    private val eventChannel = Channel<BallEngineEvent>(Channel.BUFFERED)
    private val published = AtomicReference(BallSnapshot.Initial)

    /** Impacts and reveals, for haptics and the caption. */
    val events: Flow<BallEngineEvent> = eventChannel.receiveAsFlow()

    private var phase = BallEnginePhase.IDLE
    private var phaseSeconds = 0f
    private var accumulator = 0f
    private var elapsedSeconds = 0f

    private var gravityDirection = BallEngineTuning.defaultGravity()
    private var shellOrientation = Quat.IDENTITY
    private var shellAngularVelocity = Vec3.ZERO

    /** True while a finger is on the ball. Nothing but the finger may turn the shell until it lifts. */
    private var dragging = false

    /** Rotation the finger has asked for and [stepShell] has not applied yet, in radians. */
    private var pendingDragRotation = Vec3.ZERO

    /** The same rotation, but reset once a frame — it is what the finger's own speed is measured from. */
    private var dragRotationSinceTick = Vec3.ZERO

    /** Smoothed spin the finger implies, so the liquid is stirred while the shell is being turned. */
    private var dragAngularVelocity = Vec3.ZERO

    /** Spin held over from before the finger landed, handed back when it lets go. */
    private var carriedSpin = Vec3.ZERO

    /** Flicks still counted as recent; each one makes the next spin faster and coast longer. */
    private var flingStreak = 0f

    /** Latest device acceleration in g, gravity already removed. Decays when the sensor goes quiet. */
    private var deviceAcceleration = Vec3.ZERO

    /** Where the liquid thinks "up" is: the effective one, lagging behind so the surface swings. */
    private var sloshUp = -BallEngineTuning.defaultGravity()

    /** How far the ball has been left behind by the phone, and how fast it is catching up. */
    private var shellOffset = Vec3.ZERO
    private var shellOffsetVelocity = Vec3.ZERO

    /**
     * How stirred up the contents are, 0..1. Driven by the phone's motion and the shell's spin, and
     * fed straight back into the die as random torque, so a shake really does make the thing inside
     * thrash rather than just drift faster.
     */
    private var agitation = 0f

    /** How cloudy the liquid is, 0..1. Kicked up by the reveal and left to settle. */
    private var turbidity = 0f

    /** How wet the glass above the waterline is, 0..1. Soaked by the splashing, then drains. */
    private var wetness = 0f

    private var targetFace = 0
    private var revealTarget = Quat.IDENTITY

    /** Volume fraction per voxel, splatted from the particles once a frame. */
    private val densityField = FloatArray(DENSITY_VOXELS)
    private val densityBytes = ByteArray(DENSITY_VOXELS)
    private val packedBubbles = FloatArray(BallEngineTuning.MAX_BUBBLES * BUBBLE_STRIDE)
    private var fluidFrame = FluidFrame(
        resolution = BallEngineTuning.DENSITY_RESOLUTION,
        density = densityBytes,
        bubbles = packedBubbles,
        bubbleCount = 0,
        fieldScale = 1f
    )

    init {
        die.reset(orientation = randomOrientation())
        applyTuning(tuning)
        publish()
    }

    /** Latest consistent state; safe to read from any thread. */
    fun snapshot(): BallSnapshot = published.get()

    fun submit(command: BallCommand) {
        commands.offer(command)
    }

    /**
     * Swaps the quality tier.
     *
     * Queued rather than applied on the spot: it changes the particle count, and the fluid's arrays
     * belong to whichever thread is stepping.
     */
    fun setTuning(tuning: BallEngineTuning) {
        if (tuning == this.tuning) return
        commands.offer(BallCommand.Retune(tuning))
    }

    /** Consumes [deltaSeconds] of real time in fixed steps, then publishes a snapshot. */
    fun advance(deltaSeconds: Float) {
        drainCommands()

        val frameSeconds = deltaSeconds.coerceIn(0f, 0.25f)
        trackFinger(frameSeconds)

        accumulator += frameSeconds
        var steps = 0
        while (accumulator >= BallEngineTuning.FIXED_STEP_SECONDS && steps < tuning.maxSubstepsPerFrame) {
            step(BallEngineTuning.FIXED_STEP_SECONDS)
            accumulator -= BallEngineTuning.FIXED_STEP_SECONDS
            steps++
        }
        // Too far behind to catch up: drop the backlog instead of stepping forever.
        if (accumulator > BallEngineTuning.FIXED_STEP_SECONDS) accumulator = 0f

        // The density grid is only for drawing, so it is rebuilt per frame rather than per step.
        if (steps > 0) refreshFluidFrame()
        publish()
    }

    private fun drainCommands() {
        while (true) {
            when (val command = commands.poll() ?: return) {
                BallCommand.Grab -> beginGrab()
                is BallCommand.Drag -> applyDrag(command)
                is BallCommand.Fling -> endDrag(command)
                is BallCommand.Tilt -> applyTilt(command.gravity)
                is BallCommand.Motion -> applyMotion(command.acceleration)
                is BallCommand.Shake -> applyShake(command.magnitude)
                is BallCommand.Ask -> startAsk(command.faceIndex)
                is BallCommand.Retune -> applyTuning(command.tuning)
                BallCommand.Reset -> reset()
            }
        }
    }

    /** Resizes the fluid to fit [tuning] and refills it, which is why this cannot run per frame. */
    private fun applyTuning(tuning: BallEngineTuning) {
        this.tuning = tuning
        fluid.seed(tuning.particleCount, cavity, sloshUp, die.position)
        bubbles.seed(tuning.bubbleCount, cavity, sloshUp)
        fluidFrame = FluidFrame(
            resolution = BallEngineTuning.DENSITY_RESOLUTION,
            density = densityBytes,
            bubbles = packedBubbles,
            bubbleCount = bubbles.count,
            // Twice the splat radius: over about that distance a blob's field falls from full to
            // nothing, so it is the length the shader should read a fraction difference as.
            fieldScale = 2f * BallEngineTuning.SPLAT_RADIUS_SCALE * fluid.spacing
        )
        refreshFluidFrame()
    }

    /** Splats the particles into the grid the shader samples and packs the bubbles beside it. */
    private fun refreshFluidFrame() {
        fluid.splat(densityField, BallEngineTuning.DENSITY_RESOLUTION)
        for (index in densityField.indices) {
            densityBytes[index] = ((densityField[index] * 255f).toInt().coerceIn(0, 255)).toByte()
        }
        bubbles.writeTo(packedBubbles)
    }

    /**
     * A finger has taken hold of the shell.
     *
     * A hand on a spinning ball is a brake rather than a clutch, so only part of the spin is kept —
     * and it is kept rather than dropped because that is what lets one flick build on the last.
     */
    private fun beginGrab() {
        dragging = true
        pendingDragRotation = Vec3.ZERO
        dragRotationSinceTick = Vec3.ZERO
        carriedSpin = shellAngularVelocity * BallEngineTuning.GRAB_SPIN_RETENTION
        dragAngularVelocity = shellAngularVelocity
    }

    /**
     * Banks the rotation the finger has just asked for; [stepShell] applies it verbatim.
     *
     * Dragging right turns the ball about the vertical axis, dragging down about the horizontal one —
     * so the surface under the finger goes exactly where the finger goes.
     */
    private fun applyDrag(command: BallCommand.Drag) {
        if (!dragging) beginGrab()
        val rotation = Vec3(command.dyRadians, command.dxRadians, 0f)
        pendingDragRotation += rotation
        dragRotationSinceTick += rotation
    }

    /**
     * The finger has left. Whatever speed it left at is the spin the ball coasts on.
     *
     * A release that was not a flick leaves only what the ball was already doing, which is what makes
     * putting a finger down and lifting it again feel like stopping the thing.
     */
    private fun endDrag(command: BallCommand.Fling) {
        if (!dragging) return

        val flick = Vec3(command.dyRadiansPerSecond, command.dxRadiansPerSecond, 0f)
        val spin = if (!flick.isFinite || flick.length < BallEngineTuning.FLING_MIN_ANGULAR_SPEED) {
            carriedSpin
        } else {
            // Each flick still counted as recent adds a little speed, and — through the weakened drag
            // in [stepShell] — a little more time before the ball gives up.
            val gain = 1f + flingStreak * BallEngineTuning.FLING_STREAK_SPEED_GAIN
            flingStreak = (flingStreak + 1f).coerceAtMost(BallEngineTuning.FLING_STREAK_MAX)
            carriedSpin + flick * (BallEngineTuning.FLING_SPIN_SCALE * gain)
        }

        dragging = false
        pendingDragRotation = Vec3.ZERO
        dragRotationSinceTick = Vec3.ZERO
        dragAngularVelocity = Vec3.ZERO
        carriedSpin = Vec3.ZERO
        shellAngularVelocity = spin.clampLength(BallEngineTuning.SHELL_MAX_FLING_ANGULAR_SPEED)
    }

    /**
     * Keeps the spin the *contents* feel in step with the finger, and lets a streak of flicks fade.
     *
     * The shell's own rotation comes straight from the touch stream, but the liquid, the bubbles and
     * the agitation are all driven by an angular velocity — so one has to be inferred: how far the
     * finger moved over how long, smoothed enough that a missed touch sample does not read as the ball
     * stopping dead.
     */
    private fun trackFinger(frameSeconds: Float) {
        if (flingStreak > 0f) {
            flingStreak = (flingStreak - frameSeconds / BallEngineTuning.FLING_STREAK_WINDOW)
                .coerceAtLeast(0f)
        }
        if (!dragging) return

        val implied = if (frameSeconds > 1e-4f) dragRotationSinceTick / frameSeconds else Vec3.ZERO
        dragRotationSinceTick = Vec3.ZERO
        val blend = 1f - exp(-BallEngineTuning.DRAG_VELOCITY_RESPONSE * frameSeconds)
        dragAngularVelocity = (dragAngularVelocity + (implied - dragAngularVelocity) * blend)
            .clampLength(BallEngineTuning.SHELL_MAX_ANGULAR_SPEED)
        shellAngularVelocity = dragAngularVelocity
    }

    private fun applyTilt(gravity: Vec3) {
        val flat = gravity.normalized(BallEngineTuning.defaultGravity())
        // Keep the lean away from the camera whatever the device reports, so buoyancy still walks
        // the die towards the window.
        gravityDirection = Vec3(flat.x, flat.y, BallEngineTuning.GRAVITY_DEPTH_BIAS).normalized()
    }

    /**
     * Takes the newest reading of how hard the phone itself is being moved.
     *
     * Clamped rather than trusted: a hard shake reports several g, and letting all of that through
     * would fire the die into the glass. It is stored rather than integrated because the sensor only
     * speaks when something changes — [step] fades it out so a single jolt does not push forever.
     */
    private fun applyMotion(acceleration: Vec3) {
        if (!acceleration.isFinite) return
        deviceAcceleration = acceleration.clampLength(BallEngineTuning.MOTION_MAX_G)
    }

    /**
     * A shake, straight into the contents.
     *
     * The die takes far more spin than travel: quadratic drag eats the travel almost at once, so a
     * shake that reads as violent has to arrive mostly as tumble. The kick also tops up [agitation],
     * which keeps the die thrashing for about a second after the phone has stopped.
     */
    private fun applyShake(magnitude: Float) {
        val strength = magnitude.coerceIn(0f, 4f)
        shellAngularVelocity =
            (shellAngularVelocity + randomDirection() * (strength * BallEngineTuning.SHAKE_SHELL_IMPULSE))
                .clampLength(BallEngineTuning.SHELL_MAX_ANGULAR_SPEED)
        die.velocity = (die.velocity + randomDirection() * (strength * BallEngineTuning.SHAKE_LINEAR_IMPULSE))
            .clampLength(BallEngineTuning.DIE_MAX_SPEED)
        die.angularVelocity =
            (die.angularVelocity + randomDirection() * (strength * BallEngineTuning.SHAKE_ANGULAR_IMPULSE))
                .clampLength(BallEngineTuning.DIE_MAX_ANGULAR_SPEED)
        agitation = (agitation + strength * 0.5f).coerceAtMost(1f)
    }

    private fun startAsk(faceIndex: Int) {
        targetFace = ((faceIndex % DieGeometry.FACE_COUNT) + DieGeometry.FACE_COUNT) % DieGeometry.FACE_COUNT
        phase = BallEnginePhase.ASKING
        phaseSeconds = 0f
        // Shove the die away from the window so the answer has to swim back into view.
        die.velocity = (die.velocity - BallEngineTuning.WINDOW_AXIS * 2.7f + randomDirection() * 0.7f)
            .clampLength(BallEngineTuning.DIE_MAX_SPEED)
        die.angularVelocity = (die.angularVelocity + randomDirection() * 11f)
            .clampLength(BallEngineTuning.DIE_MAX_ANGULAR_SPEED)
        shellAngularVelocity = (shellAngularVelocity + randomDirection() * 1.4f)
            .clampLength(BallEngineTuning.SHELL_MAX_ANGULAR_SPEED)
        agitation = (agitation + 0.55f).coerceAtMost(1f)
    }

    private fun reset() {
        phase = BallEnginePhase.IDLE
        phaseSeconds = 0f
        accumulator = 0f
        die.reset(orientation = randomOrientation())
        shellAngularVelocity = Vec3.ZERO
        dragging = false
        pendingDragRotation = Vec3.ZERO
        dragRotationSinceTick = Vec3.ZERO
        dragAngularVelocity = Vec3.ZERO
        carriedSpin = Vec3.ZERO
        flingStreak = 0f
        deviceAcceleration = Vec3.ZERO
        shellOffset = Vec3.ZERO
        shellOffsetVelocity = Vec3.ZERO
        agitation = 0f
        turbidity = 0f
        wetness = 0f
        sloshUp = -gravityDirection
        fluid.seed(tuning.particleCount, cavity, sloshUp, die.position)
        bubbles.seed(tuning.bubbleCount, cavity, sloshUp)
        refreshFluidFrame()
    }

    private fun step(deltaSeconds: Float) {
        phaseSeconds += deltaSeconds
        elapsedSeconds += deltaSeconds

        val acceleration = effectiveGravity()
        trackAgitation(deltaSeconds)
        trackSlosh(deltaSeconds, acceleration)
        stepShell(deltaSeconds)
        stepOffset(deltaSeconds)
        stepDie(deltaSeconds, acceleration, sloshUp)
        fluid.step(deltaSeconds, acceleration, die, shellAngularVelocity)
        bubbles.step(deltaSeconds, elapsedSeconds, cavity, sloshUp, shellAngularVelocity)
        advancePhase()

        // The sensor only reports on change, so a jolt has to be let go of deliberately.
        deviceAcceleration *= exp(-BallEngineTuning.MOTION_DECAY * deltaSeconds)
        if (deviceAcceleration.length < 1e-3f) deviceAcceleration = Vec3.ZERO
    }

    /**
     * Chases whatever the motion is asking for and falls back slowly.
     *
     * Fast attack, slow release: the contents have to react on the first jolt, but they should keep
     * churning after the phone is still — that lag is most of what makes a shake read as liquid
     * rather than as an animation that stops with the input.
     */
    private fun trackAgitation(deltaSeconds: Float) {
        val fromMotion = deviceAcceleration.length / BallEngineTuning.AGITATION_FULL_G
        val fromSpin = shellAngularVelocity.length / BallEngineTuning.SHELL_MAX_ANGULAR_SPEED
        val target = (fromMotion + fromSpin * 0.7f).coerceIn(0f, 1f)
        val rate = if (target > agitation) BallEngineTuning.AGITATION_ATTACK else BallEngineTuning.AGITATION_DECAY
        agitation += (target - agitation) * (1f - exp(-rate * deltaSeconds))
        if (agitation < 1e-3f) agitation = 0f

        turbidity *= exp(-BallEngineTuning.TURBIDITY_DECAY * deltaSeconds)
        if (turbidity < 1e-3f) turbidity = 0f

        // Whatever is splashing about wets the glass at once; the film then drains on its own clock,
        // which is much slower than the churn that put it there.
        if (agitation > wetness) wetness = agitation
        wetness *= exp(-BallEngineTuning.WETNESS_DECAY * deltaSeconds)
        if (wetness < 1e-3f) wetness = 0f
    }

    /**
     * Gravity as the contents actually feel it: the real thing plus the pseudo-force of the phone
     * being moved. Whip the device up and everything inside is pressed down, which is the whole
     * reason shaking looks like shaking.
     */
    private fun effectiveGravity(): Vec3 = gravityDirection * BallEngineTuning.GRAVITY -
        deviceAcceleration * (BallEngineTuning.GRAVITY * BallEngineTuning.MOTION_GRAVITY_SCALE)

    /**
     * Follows [acceleration] with a lag. That lag *is* the slosh: the surface, the bubbles and the
     * buoyancy all use this rather than the instantaneous direction, so the liquid swings after the
     * motion instead of snapping to it.
     */
    private fun trackSlosh(deltaSeconds: Float, acceleration: Vec3) {
        val target = (-acceleration).normalized(-gravityDirection)
        val blend = 1f - exp(-BallEngineTuning.SLOSH_RESPONSE * deltaSeconds)
        sloshUp = (sloshUp + (target - sloshUp) * blend).normalized(target)
    }

    /**
     * A damped spring between the ball and the middle of the screen, driven by the phone's own
     * acceleration. Flick the device down and the ball, being heavy, is left behind and rides up.
     */
    private fun stepOffset(deltaSeconds: Float) {
        val target = (-deviceAcceleration * BallEngineTuning.SHELL_OFFSET_PER_G)
            .clampLength(BallEngineTuning.SHELL_MAX_OFFSET)
        val force = (target - shellOffset) * BallEngineTuning.SHELL_OFFSET_STIFFNESS -
            shellOffsetVelocity * BallEngineTuning.SHELL_OFFSET_DAMPING
        shellOffsetVelocity += force * deltaSeconds
        shellOffset = (shellOffset + shellOffsetVelocity * deltaSeconds)
            .clampLength(BallEngineTuning.SHELL_MAX_OFFSET)
        if (shellOffset.length < 1e-4f && shellOffsetVelocity.length < 1e-3f) {
            shellOffset = Vec3.ZERO
            shellOffsetVelocity = Vec3.ZERO
        }
    }

    private fun stepShell(deltaSeconds: Float) {
        if (dragging) {
            // The finger owns the shell: it turns by exactly the rotation the finger asked for, with
            // neither the drag nor the window's own alignment allowed a say until the touch ends.
            val rotation = pendingDragRotation
            pendingDragRotation = Vec3.ZERO
            val angle = rotation.length
            if (angle > 1e-6f) {
                shellOrientation = Quat.fromAxisAngle(rotation / angle, angle) * shellOrientation
            }
            return
        }

        if (phase != BallEnginePhase.IDLE) alignWindowToCamera(deltaSeconds)
        shellOrientation = shellOrientation.integrated(shellAngularVelocity, deltaSeconds)
        // Flicks in quick succession coast longer as well as faster: the same drag, weakened.
        val angularDrag = BallEngineTuning.SHELL_ANGULAR_DRAG /
            (1f + flingStreak * BallEngineTuning.FLING_STREAK_COAST_GAIN)
        shellAngularVelocity *= exp(-angularDrag * deltaSeconds)
        if (shellAngularVelocity.length < 0.01f) shellAngularVelocity = Vec3.ZERO
    }

    /**
     * Turns the shell until the window faces the camera again.
     *
     * However far the player has spun the ball, an answer has to end up readable — so from the
     * moment one is asked for, the window is steered back to the front. Only the swing is damped:
     * the twist about the window axis is left alone, so a flick still rolls the ball in place.
     */
    private fun alignWindowToCamera(deltaSeconds: Float) {
        val axis = BallEngineTuning.WINDOW_AXIS
        val facing = shellOrientation.rotate(axis)

        val cross = facing cross axis
        val sine = cross.length
        val cosine = (facing dot axis).coerceIn(-1f, 1f)
        val errorAxis = when {
            sine > 1e-4f -> cross / sine
            // Already there, or turned exactly away — in which case any axis gets it moving.
            cosine > 0f -> Vec3.ZERO
            else -> perpendicularTo(axis)
        }

        val hold = if (phase == BallEnginePhase.ANSWERED) BallEngineTuning.WINDOW_ALIGN_HOLD_SCALE else 1f
        val twist = facing * (shellAngularVelocity dot facing)
        val correction = errorAxis * (atan2(sine, cosine) * BallEngineTuning.WINDOW_ALIGN_PROPORTIONAL_GAIN * hold) -
            (shellAngularVelocity - twist) * (BallEngineTuning.WINDOW_ALIGN_DERIVATIVE_GAIN * hold)
        shellAngularVelocity = (shellAngularVelocity + correction * deltaSeconds)
            .clampLength(BallEngineTuning.SHELL_MAX_ANGULAR_SPEED)
    }

    private fun stepDie(deltaSeconds: Float, acceleration: Vec3, up: Vec3) {
        val submersion = cavity.submergedFraction(die.position, die.radius, up)
        val steering = phase == BallEnginePhase.REVEALING || phase == BallEnginePhase.ANSWERED

        // Weight and the buoyancy of however much of the die is under the surface. While the reveal
        // controller holds the die at the window, most of that pair is cancelled: left alone the die
        // settles where the two balance, which is a good deal lower than the window.
        val displaced = acceleration.length * die.mass * submersion /
            BallEngineTuning.DIE_RELATIVE_DENSITY
        val bodyForce = acceleration * die.mass + up * displaced
        die.applyForce(bodyForce * (if (steering) BUOYANCY_HOLD_SCALE else 1f), deltaSeconds)

        // Quadratic drag is what stops a hard shake from flinging the die across the cavity.
        val speed = die.velocity.length
        if (speed > Vec3.EPSILON) {
            val quadratic = BallEngineTuning.FLUID_QUADRATIC_DRAG * submersion * speed * speed
            die.applyForce(die.velocity / speed * -quadratic, deltaSeconds)
        }

        // Stirred liquid does not push evenly. A new random torque every step is what turns a shake
        // into the die visibly kicking and spinning rather than merely being carried faster, and it
        // is scaled by how much of the die the liquid actually has hold of.
        if (agitation > 1e-3f) {
            val churn = agitation * submersion * (if (steering) 0.35f else 1f)
            die.angularVelocity =
                (die.angularVelocity + randomDirection() * (churn * BallEngineTuning.AGITATION_TORQUE * deltaSeconds))
                    .clampLength(BallEngineTuning.DIE_MAX_ANGULAR_SPEED)
            die.velocity =
                (die.velocity + randomDirection() * (churn * BallEngineTuning.AGITATION_JITTER * deltaSeconds))
                    .clampLength(BallEngineTuning.DIE_MAX_SPEED)
        }

        // The liquid carries the die around with the shell. Viscous rather than a kick, so a finger
        // that keeps turning keeps feeding it and one that lets go leaves it to wind down on its own.
        if (!steering && submersion > 1e-3f) {
            val target = shellAngularVelocity * BallEngineTuning.SHELL_DIE_COUPLING
            val blend = 1f - exp(-BallEngineTuning.SHELL_DIE_COUPLING_RATE * submersion * deltaSeconds)
            die.angularVelocity = (die.angularVelocity + (target - die.angularVelocity) * blend)
                .clampLength(BallEngineTuning.DIE_MAX_ANGULAR_SPEED)
        }

        if (steering) {
            steerToWindow(deltaSeconds)
        }

        die.damp(
            linearPerSecond = lerp(BallEngineTuning.AIR_LINEAR_DRAG, BallEngineTuning.FLUID_LINEAR_DRAG, submersion),
            angularPerSecond = lerp(BallEngineTuning.AIR_ANGULAR_DRAG, BallEngineTuning.FLUID_ANGULAR_DRAG, submersion),
            deltaSeconds = deltaSeconds
        )
        die.integrate(deltaSeconds)

        val impactSpeed = cavity.constrain(die)
        if (impactSpeed > BallEngineTuning.IMPACT_HAPTIC_SPEED) {
            val strength = ((impactSpeed - BallEngineTuning.IMPACT_HAPTIC_SPEED) / 2.5f)
                .coerceIn(0.15f, 1f)
            eventChannel.trySend(BallEngineEvent.Impact(strength))
        }
        die.sanitize()
    }

    /**
     * PD control that walks the die into the window cup and twists the chosen face onto the window
     * axis. The answer was already drawn before any of this ran — this only makes the picture agree
     * with it.
     */
    private fun steerToWindow(deltaSeconds: Float) {
        val hold = if (phase == BallEnginePhase.ANSWERED) HOLD_GAIN_SCALE else 1f

        val error = die.orientation.errorTo(revealTarget)
        val angularCorrection = error * (BallEngineTuning.REVEAL_PROPORTIONAL_GAIN * hold) -
            die.angularVelocity * (BallEngineTuning.REVEAL_DERIVATIVE_GAIN * hold)
        die.angularVelocity = (die.angularVelocity + angularCorrection * deltaSeconds)
            .clampLength(BallEngineTuning.DIE_MAX_ANGULAR_SPEED)

        val toDock = dockPosition() - die.position
        val linearCorrection = toDock * (DOCK_PROPORTIONAL_GAIN * hold) -
            die.velocity * (DOCK_DERIVATIVE_GAIN * hold)
        die.velocity = (die.velocity + linearCorrection * deltaSeconds)
            .clampLength(BallEngineTuning.DIE_MAX_SPEED)
    }

    private fun advancePhase() {
        when (phase) {
            BallEnginePhase.IDLE, BallEnginePhase.ANSWERED -> Unit

            BallEnginePhase.ASKING -> if (phaseSeconds >= ASK_TUMBLE_SECONDS) {
                revealTarget = buildRevealTarget(targetFace)
                phase = BallEnginePhase.REVEALING
                phaseSeconds = 0f
                // The die sets off for the window through liquid it has just churned up; the answer
                // should develop out of that murk rather than slide into view through clear blue.
                turbidity = BallEngineTuning.TURBIDITY_ON_REVEAL
            }

            BallEnginePhase.REVEALING -> {
                val aligned = die.orientation.angleTo(revealTarget) <=
                    BallEngineTuning.REVEAL_TOLERANCE_RADIANS
                val docked = (die.position - dockPosition()).length <= DOCK_TOLERANCE
                val calm = die.angularVelocity.length <= 0.6f
                val timedOut = phaseSeconds >= BallEngineTuning.REVEAL_TIMEOUT_SECONDS
                if (timedOut) {
                    // A wedged die must never leave the caption waiting; snap and report.
                    die.orientation = revealTarget
                    die.position = dockPosition()
                    die.velocity = Vec3.ZERO
                    die.angularVelocity = Vec3.ZERO
                }
                if ((aligned && docked && calm) || timedOut) {
                    phase = BallEnginePhase.ANSWERED
                    phaseSeconds = 0f
                    if (!timedOut) settleAgainstGlass()
                    eventChannel.trySend(BallEngineEvent.Revealed)
                }
            }
        }
    }
    /**
     * The knock and rock as the answer beds into the window.
     *
     * The controller is perfectly able to just hold the die still, and that is exactly what it looked
     * like: the reveal ended by being switched off. A nudge into the glass and a little spin let the
     * hold's own spring rock the die a couple of times instead, which is how something with mass
     * arrives somewhere. Skipped when the reveal timed out — that path has already snapped everything
     * into place and has a caption waiting on it.
     */
    private fun settleAgainstGlass() {
        die.velocity = (die.velocity + BallEngineTuning.WINDOW_AXIS * BallEngineTuning.SETTLE_PRESS)
            .clampLength(BallEngineTuning.DIE_MAX_SPEED)
        die.angularVelocity = (die.angularVelocity + randomDirection() * BallEngineTuning.SETTLE_ROCK)
            .clampLength(BallEngineTuning.DIE_MAX_ANGULAR_SPEED)
    }

    private fun publish() {
        val up = sloshUp
        val submersion = cavity.submergedFraction(die.position, die.radius, up)
        val settled = (phase == BallEnginePhase.IDLE || phase == BallEnginePhase.ANSWERED) &&
            // A finger on the ball has to be followed at full rate, whatever the ball is doing.
            !dragging &&
            die.velocity.length < BallEngineTuning.SETTLED_SPEED &&
            die.angularVelocity.length < 0.2f &&
            shellAngularVelocity.length < 0.02f &&
            deviceAcceleration.length < 0.02f &&
            shellOffsetVelocity.length < 0.02f &&
            // Both of these are still animating something the player can see, so a throttled frame
            // rate would show the fizz dying and the murk clearing in steps.
            agitation < 0.02f &&
            turbidity < 0.02f &&
            // So are the drips on the glass, which outlast both.
            wetness < 0.04f

        published.set(
            BallSnapshot(
                phase = phase,
                shellOrientation = shellOrientation,
                shellOffset = shellOffset,
                diePosition = die.position,
                dieOrientation = die.orientation,
                up = up,
                fluidSurfaceOffset = cavity.surfaceOffset,
                dieSubmersion = submersion,
                agitation = agitation,
                turbidity = turbidity,
                wetness = wetness,
                settled = settled,
                fluid = fluidFrame
            )
        )
    }

    /**
     * Orientation that puts face [faceIndex] in the window, upright: the face normal goes onto the
     * window axis and the face's own tangent onto screen-right, so the answer reads level.
     */
    private fun buildRevealTarget(faceIndex: Int): Quat {
        val normal = DieGeometry.faceNormals[faceIndex]
        val tangent = DieGeometry.faceTangents[faceIndex]

        val axis = BallEngineTuning.WINDOW_AXIS
        val screenRight = (Vec3(1f, 0f, 0f) - axis * axis.x).normalized()

        val toAxis = Quat.rotationBetween(normal, axis)
        val twisted = toAxis.rotate(tangent)

        // Both vectors are perpendicular to the axis, so what is left is a pure twist about it. It
        // has to be built from the signed angle rather than from a generic rotation-between: four
        // faces come out of the first turn pointing exactly *away* from screen-right, and a
        // rotation-between would then be free to pick any axis and swing the face off the window.
        val cosTwist = (twisted dot screenRight).coerceIn(-1f, 1f)
        val sinTwist = (twisted cross screenRight) dot axis
        return Quat.fromAxisAngle(axis, atan2(sinTwist, cosTwist)) * toAxis
    }

    /** Where the die rests when it is showing an answer: just inside the glass under the window. */
    private fun dockPosition(): Vec3 = BallEngineTuning.WINDOW_AXIS *
        ((BallEngineTuning.CAVITY_RADIUS - BallEngineTuning.DIE_CIRCUMRADIUS) * 0.94f)

    private fun randomDirection(): Vec3 {
        // Rejection sampling inside the unit sphere; a handful of tries at worst.
        while (true) {
            val candidate = Vec3(
                random.nextFloat() * 2f - 1f,
                random.nextFloat() * 2f - 1f,
                random.nextFloat() * 2f - 1f
            )
            val lengthSquared = candidate.lengthSquared
            if (lengthSquared in 1e-4f..1f) return candidate / sqrt(lengthSquared)
        }
    }

    private fun randomOrientation(): Quat =
        Quat.fromAxisAngle(randomDirection(), random.nextFloat() * 2f * Math.PI.toFloat())

    /** Any unit vector perpendicular to [axis]; used when a rotation error is exactly half a turn. */
    private fun perpendicularTo(axis: Vec3): Vec3 {
        val reference = if (kotlin.math.abs(axis.y) < 0.9f) Vec3.UP else Vec3.FORWARD
        return (reference cross axis).normalized(Vec3(1f, 0f, 0f))
    }

    private fun lerp(from: Float, to: Float, t: Float): Float =
        from + (to - from) * t.coerceIn(0f, 1f)

    private companion object {
        /** How long the die tumbles before the controller takes over. */
        const val ASK_TUMBLE_SECONDS = 0.85f

        /** Voxels in the density grid handed to the shader. */
        const val DENSITY_VOXELS = BallEngineTuning.DENSITY_RESOLUTION *
            BallEngineTuning.DENSITY_RESOLUTION *
            BallEngineTuning.DENSITY_RESOLUTION

        /** Floats per bubble in the packed array: centre plus radius. */
        const val BUBBLE_STRIDE = 4

        const val DOCK_PROPORTIONAL_GAIN = 15f
        const val DOCK_DERIVATIVE_GAIN = 5.5f
        const val DOCK_TOLERANCE = 0.06f

        /**
         * How much of gravity and buoyancy is left in play while the die is docked. Not zero, so a
         * tilt still makes the answer shift a little instead of looking pinned.
         */
        const val BUOYANCY_HOLD_SCALE = 0.15f

        /** The hold once answered is gentler, so the die breathes instead of locking rigid. */
        const val HOLD_GAIN_SCALE = 0.45f
    }
}
