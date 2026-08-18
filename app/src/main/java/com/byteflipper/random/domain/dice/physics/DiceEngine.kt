package com.byteflipper.random.domain.dice.physics

import com.byteflipper.random.domain.physics.SimulationQualityTier
import com.byteflipper.random.domain.physics.Vec3
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The dice tray: up to ten cubes, six walls, and a fixed-step rigid-body simulation.
 *
 * [advance] runs on the GL thread in fixed steps; commands arrive from the UI thread through a queue
 * and state goes back out as an immutable snapshot, so neither side ever waits on the other — the same
 * shape as the ball's engine, for the same reasons.
 *
 * A 3D throw is deliberately different from the flat dice: its result is the face that actually ends
 * up pointing upward. There is no late steering or orientation snap, so a die that appears to have
 * stopped can never rotate in place just to satisfy a value chosen before the throw.
 */
class DiceEngine(
    tuning: DiceEngineTuning = DiceEngineTuning.forTier(SimulationQualityTier.BALANCED),
    private val random: Random = Random.Default
) {

    @Volatile
    var tuning: DiceEngineTuning = tuning
        private set

    /** Cut for the most dice the tray will ever hold; only the first [count] are in play. */
    private val bodies = List(DiceEngineTuning.MAX_DICE) { DiceBody() }
    private var count = 0
    private var halfExtent = DiceEngineTuning.dieHalfExtent(1)

    private val collision = BoxCollision()
    private val solver = DiceSolver()
    private val contacts = ContactBuffer()

    private val commands = ConcurrentLinkedQueue<DiceCommand>()
    private val eventChannel = Channel<DiceEngineEvent>(Channel.BUFFERED)
    private val published = AtomicReference(DiceSnapshot.Initial)

    /** Impacts and the end of a roll, for haptics and the caption. */
    val events: Flow<DiceEngineEvent> = eventChannel.receiveAsFlow()

    private var phase = DicePhase.IDLE
    private var phaseSeconds = 0f
    private var accumulator = 0f

    /** Static down: holding the phone at an angle never drags the dice around the tray. */
    private val gravityDirection = Vec3.DOWN

    /** Device movement in g, world axes, gravity already taken out. Decays when the sensor goes quiet. */
    private var deviceAcceleration = Vec3.ZERO

    /** One finger-controlled body and its spring target. All mutations still happen on the GL thread. */
    private var draggedIndex = NO_DIE
    private var dragTarget = Vec3.ZERO
    private var dragVelocity = Vec3.ZERO

    /** Short visual recoil of the physical tray after a recognised phone toss. */
    private var trayReactionSeconds = 0f
    private var trayReactionAmplitude = 0f

    /** Time since the last published impact, so one contact manifold cannot flood observers. */
    private var sinceImpactEvent = 0f

    /**
     * The snapshot's arrays, filled in place and handed out by reference.
     *
     * Reused rather than rebuilt, as the ball does with its density grid: a fresh set every frame is
     * garbage for nothing, and the only reader is the renderer, on the thread that just stepped.
     */
    private val positions = FloatArray(DiceEngineTuning.MAX_DICE * 3)
    private val orientations = FloatArray(DiceEngineTuning.MAX_DICE * 4)
    private val values = IntArray(DiceEngineTuning.MAX_DICE)

    init {
        publish()
    }

    /** Latest scalar state plus GL-thread-owned arrays; consume synchronously on the render thread. */
    fun snapshot(): DiceSnapshot = published.get()

    fun submit(command: DiceCommand) {
        commands.offer(command)
    }

    /** Swaps the quality tier. Queued, so the thread that steps the bodies is the one that retunes. */
    fun setTuning(tuning: DiceEngineTuning) {
        commands.offer(DiceCommand.Retune(tuning))
    }

    /** Consumes [deltaSeconds] of real time in fixed steps, then publishes a snapshot. */
    fun advance(deltaSeconds: Float) {
        drainCommands()

        accumulator += deltaSeconds.coerceIn(0f, 0.25f)
        var steps = 0
        while (accumulator >= DiceEngineTuning.FIXED_STEP_SECONDS &&
            steps < tuning.maxSubstepsPerFrame
        ) {
            step(DiceEngineTuning.FIXED_STEP_SECONDS)
            accumulator -= DiceEngineTuning.FIXED_STEP_SECONDS
            steps++
        }
        // Too far behind to catch up: drop the backlog rather than stepping forever.
        if (accumulator > DiceEngineTuning.FIXED_STEP_SECONDS) accumulator = 0f

        publish()
    }

    private fun drainCommands() {
        while (true) {
            when (val command = commands.poll() ?: return) {
                is DiceCommand.Roll -> roll(command.count)
                is DiceCommand.Arrange -> arrange(command.values)
                is DiceCommand.Motion -> applyMotion(command.acceleration)
                is DiceCommand.Toss -> toss(command.kind, command.strength)
                is DiceCommand.BeginDrag -> beginDrag(command.index, command.target)
                is DiceCommand.Drag -> drag(command.index, command.target, command.velocity)
                is DiceCommand.EndDrag -> endDrag(command.index, command.velocity)
                is DiceCommand.TapDie -> tapDie(command.index)
                is DiceCommand.Retune -> tuning = command.tuning
            }
        }
    }

    /** Starts a tap throw by dipping the far edge while the dice remain attached to the tray. */
    private fun roll(requestedCount: Int) {
        cancelDrag()
        clearTrayReaction()
        val nextCount = requestedCount.coerceIn(0, DiceEngineTuning.MAX_DICE)
        if (nextCount == 0) {
            count = 0
            phase = DicePhase.IDLE
            phaseSeconds = 0f
            return
        }
        if (nextCount != count) {
            arrange(List(nextCount) { random.nextInt(DieFaces.MIN_VALUE, DieFaces.MAX_VALUE + 1) })
        }
        count = nextCount
        halfExtent = DiceEngineTuning.dieHalfExtent(count)
        for (index in 0 until count) {
            bodies[index].forceSleep(
                trayNearHalfX = DiceEngineTuning.TRAY_NEAR_HALF_X,
                trayFarHalfX = DiceEngineTuning.TRAY_FAR_HALF_X,
                trayHalfZ = DiceEngineTuning.TRAY_HALF_Z
            )
        }
        phase = DicePhase.WINDUP
        phaseSeconds = 0f
        accumulator = 0f
        deviceAcceleration = Vec3.ZERO
        sinceImpactEvent = DiceEngineTuning.IMPACT_EVENT_INTERVAL
    }

    /**
     * Releases every die with the tangential velocity of the tray's fast return stroke.
     *
     * The near edge is the hinge. The farther a die lies from it, the longer its lever arm and the
     * faster the returning tray is moving underneath it. Its height also contributes the forward
     * component, while its lateral position gives it a small outward spread. That is why a handful
     * fans out naturally instead of ten cubes receiving the same launch vector.
     */
    private fun launchFromTray() {
        for (index in 0 until count) {
            val body = bodies[index]
            val hingeDistance = (DiceEngineTuning.TRAY_HALF_Z - body.position.z)
                .coerceIn(0f, 2f * DiceEngineTuning.TRAY_HALF_Z)
            val trayWidth = DiceEngineTuning.trayHalfWidth(body.position.z).coerceAtLeast(halfExtent)
            val lateralPosition = (body.position.x / trayWidth).coerceIn(-1f, 1f)
            val returnSpeed = DiceEngineTuning.THROW_RETURN_ANGULAR_SPEED
            val lift = lerp(
                DiceEngineTuning.THROW_BASE_LIFT_MIN,
                DiceEngineTuning.THROW_BASE_LIFT_MAX,
                random.nextFloat()
            ) + returnSpeed * hingeDistance
            val forward = lerp(
                DiceEngineTuning.THROW_FORWARD_MIN,
                DiceEngineTuning.THROW_FORWARD_MAX,
                random.nextFloat()
            ) + returnSpeed * body.position.y
            val spin = lerp(
                DiceEngineTuning.THROW_SPIN_MIN,
                DiceEngineTuning.THROW_SPIN_MAX,
                random.nextFloat()
            )
            body.launch(
                halfExtent = halfExtent,
                position = body.position,
                velocity = Vec3(
                    lateralPosition * DiceEngineTuning.THROW_OUTWARD_SPEED +
                        (random.nextFloat() * 2f - 1f) * DiceEngineTuning.THROW_SCATTER,
                    lift,
                    forward
                ),
                orientation = body.orientation,
                angularVelocity = randomDirection() * spin
            )
        }
        phase = DicePhase.ROLLING
        phaseSeconds = 0f
    }

    /**
     * Lays the dice out at rest showing [shown], for when the player changes how many are in play.
     *
     * A tray nobody has thrown into yet still has to look like dice sitting in a tray, and the count
     * decides how big they are as well as how many.
     */
    private fun arrange(shown: List<Int>) {
        cancelDrag()
        clearTrayReaction()
        count = shown.size.coerceIn(0, DiceEngineTuning.MAX_DICE)
        halfExtent = DiceEngineTuning.dieHalfExtent(count)
        phase = DicePhase.IDLE
        phaseSeconds = 0f
        accumulator = 0f

        val columns = gridColumns(count)
        val rows = ceil(count / columns.toFloat()).toInt().coerceAtLeast(1)
        val spacing = halfExtent * REST_SPACING
        for (index in 0 until count) {
            bodies[index].rest(
                halfExtent = halfExtent,
                targetValue = shown[index],
                x = (index % columns - (columns - 1) * 0.5f) * spacing,
                z = (index / columns - (rows - 1) * 0.5f) * spacing,
                yawRadians = random.nextFloat() * TWO_PI
            )
        }
    }

    /**
     * Dice across the tray for a handful of [count], which is what decides the shape of the grid they
     * enter and rest on. Square would be the obvious choice, but the tray is not: past three abreast a
     * row would be wider than the walls it has to fit between.
     */
    private fun gridColumns(count: Int): Int {
        val ideal = ceil(sqrt(count.toFloat())).toInt()
        val spacing = halfExtent * REST_SPACING
        val usableWidth = 2f * (DiceEngineTuning.TRAY_NEAR_HALF_X - halfExtent)
            .coerceAtLeast(0f)
        val fitting = (floor(usableWidth / spacing).toInt() + 1).coerceAtLeast(1)
        return ideal
            .coerceAtMost(fitting)
            .coerceIn(1, DiceEngineTuning.GRID_MAX_COLUMNS)
            .coerceAtMost(count.coerceAtLeast(1))
    }

    /**
     * Takes the newest reading of how hard the phone itself is being moved, mapped into the tray.
     *
     * The camera looks down on the tray, so a shove up the screen pushes the dice away from the viewer
     * and one out of the screen lifts them. Clamped rather than trusted — a hard shake reports several
     * g, and all of it would fire the dice through the lid — and stored rather than integrated, because
     * the sensor only speaks when something changes: [step] fades it so one jolt does not push forever.
     */
    private fun applyMotion(acceleration: Vec3) {
        if (!acceleration.isFinite) return
        deviceAcceleration = Vec3(acceleration.x, acceleration.z, -acceleration.y)
            .clampLength(DiceEngineTuning.MOTION_MAX_G)
        if (deviceAcceleration.lengthSquared > MOTION_WAKE_G * MOTION_WAKE_G) wakeAll()
    }

    /**
     * Converts measured phone speed into a real impulse on the dice already lying in the tray.
     *
     * A table-pitch uses the near edge as a hinge, so dice near the moving far edge receive more lift.
     * An upright lift raises the whole tray and therefore gives every die the same base impulse. Both
     * keep a little position-dependent spread and tumble so the handful does not move as one object.
     */
    private fun toss(kind: DiceTossKind, measuredStrength: Float) {
        if (count == 0) return
        cancelDrag()
        val strength = smoothstep(measuredStrength.coerceIn(0f, 1f))
        val lift = lerp(
            DiceEngineTuning.GESTURE_LIFT_MIN,
            DiceEngineTuning.GESTURE_LIFT_MAX,
            strength
        )
        val spin = lerp(
            DiceEngineTuning.GESTURE_SPIN_MIN,
            DiceEngineTuning.GESTURE_SPIN_MAX,
            strength
        )
        val scatterSpeed = lerp(
            DiceEngineTuning.GESTURE_SCATTER_MIN,
            DiceEngineTuning.GESTURE_SCATTER_MAX,
            strength
        )

        phase = DicePhase.ROLLING
        phaseSeconds = 0f
        deviceAcceleration = Vec3.ZERO
        sinceImpactEvent = DiceEngineTuning.IMPACT_EVENT_INTERVAL
        trayReactionSeconds = TRAY_REACTION_SECONDS
        trayReactionAmplitude = lerp(
            TRAY_REACTION_MIN_RADIANS,
            TRAY_REACTION_MAX_RADIANS,
            strength
        ) * if (kind == DiceTossKind.TABLE_PITCH) 1f else VERTICAL_REACTION_SCALE
        for (index in 0 until count) {
            val body = bodies[index]
            body.wake()
            val randomVector = randomDirection()
            val lateralPosition = (body.position.x /
                DiceEngineTuning.trayHalfWidth(body.position.z)).coerceIn(-1f, 1f)
            val lever = ((DiceEngineTuning.TRAY_HALF_Z - body.position.z) /
                (2f * DiceEngineTuning.TRAY_HALF_Z)).coerceIn(0f, 1f)
            val liftScale = when (kind) {
                DiceTossKind.TABLE_PITCH -> lerp(PITCH_NEAR_LIFT, PITCH_FAR_LIFT, lever)
                DiceTossKind.VERTICAL_LIFT -> 1f
            }
            val forward = when (kind) {
                DiceTossKind.TABLE_PITCH -> strength * PITCH_FORWARD_SPEED * (0.35f + lever)
                DiceTossKind.VERTICAL_LIFT -> randomVector.z * scatterSpeed
            }
            val impulse = Vec3(
                randomVector.x * scatterSpeed + lateralPosition * POSITION_OUTWARD_SPEED * strength,
                lift * liftScale,
                forward
            )
            body.velocity = (body.velocity + impulse)
                .clampLength(DiceEngineTuning.MAX_LINEAR_SPEED)
            body.angularVelocity =
                (body.angularVelocity + randomDirection() * spin)
                    .clampLength(DiceEngineTuning.MAX_ANGULAR_SPEED)
        }
    }

    private fun beginDrag(index: Int, target: Vec3) {
        if (index !in 0 until count || !target.isFinite || phase == DicePhase.WINDUP) return
        draggedIndex = index
        dragTarget = clampDragTarget(target)
        dragVelocity = Vec3.ZERO
        bodies[index].wake()
        phase = DicePhase.ROLLING
        phaseSeconds = 0f
    }

    private fun drag(index: Int, target: Vec3, velocity: Vec3) {
        if (index != draggedIndex || !target.isFinite || !velocity.isFinite) return
        dragTarget = clampDragTarget(target)
        dragVelocity = velocity.clampLength(DRAG_MAX_RELEASE_SPEED)
    }

    private fun endDrag(index: Int, velocity: Vec3) {
        if (index != draggedIndex) return
        val body = bodies[index]
        val release = if (velocity.isFinite) velocity else dragVelocity
        val planar = Vec3(release.x, 0f, release.z).clampLength(DRAG_MAX_RELEASE_SPEED)
        val speed = planar.length
        val lift = if (speed < DRAG_FLING_START_SPEED) {
            DRAG_DROP_LIFT
        } else {
            lerp(
                DRAG_FLING_LIFT_MIN,
                DRAG_FLING_LIFT_MAX,
                ((speed - DRAG_FLING_START_SPEED) /
                    (DRAG_MAX_RELEASE_SPEED - DRAG_FLING_START_SPEED)).coerceIn(0f, 1f)
            )
        }
        body.velocity = (planar + Vec3(0f, lift, 0f))
            .clampLength(DiceEngineTuning.MAX_LINEAR_SPEED)
        body.angularVelocity = (
            body.angularVelocity * DRAG_RETAINED_SPIN +
                Vec3(-planar.z, 0f, planar.x) * DRAG_FLING_SPIN
            ).clampLength(DiceEngineTuning.MAX_ANGULAR_SPEED)
        draggedIndex = NO_DIE
        phase = DicePhase.ROLLING
        phaseSeconds = 0f
    }

    private fun tapDie(index: Int) {
        if (index !in 0 until count) return
        if (draggedIndex != NO_DIE && draggedIndex != index) return
        draggedIndex = NO_DIE
        val body = bodies[index]
        body.wake()
        body.velocity = (body.velocity * TAP_RETAINED_VELOCITY + Vec3(
            (random.nextFloat() * 2f - 1f) * TAP_SCATTER,
            TAP_LIFT,
            (random.nextFloat() * 2f - 1f) * TAP_SCATTER
        )).clampLength(DiceEngineTuning.MAX_LINEAR_SPEED)
        body.angularVelocity = (body.angularVelocity + randomDirection() * TAP_SPIN)
            .clampLength(DiceEngineTuning.MAX_ANGULAR_SPEED)
        phase = DicePhase.ROLLING
        phaseSeconds = 0f
        sinceImpactEvent = DiceEngineTuning.IMPACT_EVENT_INTERVAL
    }

    private fun cancelDrag() {
        draggedIndex = NO_DIE
        dragTarget = Vec3.ZERO
        dragVelocity = Vec3.ZERO
    }

    private fun clampDragTarget(target: Vec3): Vec3 {
        val y = target.y.coerceIn(halfExtent * DRAG_MIN_HEIGHT_MULTIPLIER, DRAG_MAX_HEIGHT)
        val limitZ = (DiceEngineTuning.TRAY_HALF_Z - halfExtent).coerceAtLeast(0f)
        val z = target.z.coerceIn(-limitZ, limitZ)
        val limitX = (DiceEngineTuning.trayHalfWidth(z) - halfExtent).coerceAtLeast(0f)
        return Vec3(target.x.coerceIn(-limitX, limitX), y, z)
    }

    /**
     * One fixed step, in the order the pieces depend on each other.
     *
     * Forces first, so the velocities the solver sees are the ones the step is about to integrate;
     * detection before waking, because a die woken after the effective masses were worked out would
     * spend the step immovable and hand its neighbour a bounce off a wall; and integration only once
     * every contact has had its say.
     */
    private fun step(deltaSeconds: Float) {
        phaseSeconds += deltaSeconds

        if (phase == DicePhase.WINDUP) {
            if (phaseSeconds >= DiceEngineTuning.THROW_WINDUP_SECONDS) launchFromTray()
        } else if (count > 0) {
            applyForces(deltaSeconds)
            buildContacts()
            solver.wakeOnImpact(bodies, contacts, DiceEngineTuning.WAKE_SPEED)
            solver.prepare(bodies, contacts)
            solver.solve(bodies, contacts, tuning.solverIterations, deltaSeconds)
            solver.correctPositions(bodies, contacts)
            integrate(deltaSeconds)
            reportImpact(deltaSeconds)
            updateSleep(deltaSeconds)
            advancePhase()
        }

        // The sensor only reports on change, so a jolt has to be let go of deliberately.
        deviceAcceleration *= exp(-DiceEngineTuning.MOTION_DECAY * deltaSeconds)
        if (deviceAcceleration.length < 1e-3f) deviceAcceleration = Vec3.ZERO
        trayReactionSeconds = (trayReactionSeconds - deltaSeconds).coerceAtLeast(0f)
    }

    /** Tap wind-up, or a short damped recoil after a measured phone gesture. */
    private fun trayPitchRadians(): Float {
        if (phase == DicePhase.WINDUP) {
            return if (phaseSeconds <= DiceEngineTuning.THROW_WINDUP_DOWN_SECONDS) {
                val down = phaseSeconds / DiceEngineTuning.THROW_WINDUP_DOWN_SECONDS
                DiceEngineTuning.THROW_WINDUP_DIP_RADIANS * smootherstep(down)
            } else {
                val returning = ((phaseSeconds - DiceEngineTuning.THROW_WINDUP_DOWN_SECONDS) /
                    DiceEngineTuning.THROW_WINDUP_RETURN_SECONDS).coerceIn(0f, 1f)
                // Cubic ease-out: maximum angular speed immediately after the direction changes.
                val remaining = 1f - returning
                DiceEngineTuning.THROW_WINDUP_DIP_RADIANS * remaining * remaining * remaining
            }
        }
        if (trayReactionSeconds <= 0f) return 0f
        val progress = 1f - trayReactionSeconds / TRAY_REACTION_SECONDS
        val envelope = (1f - progress) * (1f - progress)
        return -trayReactionAmplitude * sin(progress * TWO_PI) * envelope
    }

    private fun clearTrayReaction() {
        trayReactionSeconds = 0f
        trayReactionAmplitude = 0f
    }

    private fun applyForces(deltaSeconds: Float) {
        val gravity = effectiveGravity()
        for (index in 0 until count) {
            val body = bodies[index]
            if (body.asleep) continue
            if (index == draggedIndex) {
                // A damped spring keeps the die attached to the finger while still letting it collide
                // with and push the other dice instead of teleporting through them.
                val spring = (dragTarget - body.position) * DRAG_SPRING -
                    (body.velocity - dragVelocity) * DRAG_DAMPING
                body.velocity += spring.clampLength(DRAG_MAX_ACCELERATION) * deltaSeconds
            } else {
                body.velocity += gravity * deltaSeconds
            }
            body.damp(
                linearPerSecond = DiceEngineTuning.LINEAR_DAMPING,
                angularPerSecond = DiceEngineTuning.ANGULAR_DAMPING,
                deltaSeconds = deltaSeconds
            )
        }
    }

    private fun buildContacts() {
        contacts.clear()
        for (index in 0 until count) {
            val body = bodies[index]
            body.supported = false
            collision.tray(
                index = index,
                body = body,
                trayNearHalfX = DiceEngineTuning.TRAY_NEAR_HALF_X,
                trayFarHalfX = DiceEngineTuning.TRAY_FAR_HALF_X,
                trayHalfZ = DiceEngineTuning.TRAY_HALF_Z,
                ceiling = DiceEngineTuning.TRAY_CEILING,
                floorRestitution = DiceEngineTuning.FLOOR_RESTITUTION,
                floorFriction = DiceEngineTuning.FLOOR_FRICTION,
                wallRestitution = DiceEngineTuning.WALL_RESTITUTION,
                wallFriction = DiceEngineTuning.WALL_FRICTION,
                out = contacts
            )
        }
        // Ten dice come to forty-five pairs, each an early-out away from being free. A broad phase
        // would cost more to keep sorted than the tests it saved.
        for (a in 0 until count) {
            for (b in a + 1 until count) {
                collision.dice(
                    indexA = a,
                    a = bodies[a],
                    indexB = b,
                    b = bodies[b],
                    restitution = DiceEngineTuning.DIE_RESTITUTION,
                    friction = DiceEngineTuning.DIE_FRICTION,
                    out = contacts
                )
            }
        }
        markSupported()
    }

    /**
     * Notes which dice have something under them.
     *
     * Only a die with a floor — the tray's, or another die's back — may fall asleep, or one would nod
     * off at the top of its bounce and hang there. "Under" is measured against gravity rather than
     * against the vertical, so it still means something on a leaning tray.
     */
    private fun markSupported() {
        val up = -gravityDirection
        for (index in 0 until contacts.size) {
            val contact = contacts[index]
            // The normal is the way the first die has to travel to come free, so the second one is
            // held up by exactly the contacts the first is held down by.
            val alignment = contact.normal dot up
            if (alignment > SUPPORT_ALIGNMENT) bodies[contact.a].supported = true
            if (contact.b >= 0 && alignment < -SUPPORT_ALIGNMENT) bodies[contact.b].supported = true
        }
    }

    private fun integrate(deltaSeconds: Float) {
        for (index in 0 until count) {
            val body = bodies[index]
            if (body.asleep) continue
            body.clampSpeeds(DiceEngineTuning.MAX_LINEAR_SPEED, DiceEngineTuning.MAX_ANGULAR_SPEED)
            body.integrate(deltaSeconds)
            body.sanitize()
            contain(body)
        }
    }

    /**
     * The last word on where a die may be.
     *
     * The walls belong to the solver and it keeps them; this only catches what a violent step could
     * get past one. A die whose centre has crossed a wall is on the wrong side of every contact it
     * would generate, so it would be pushed further out rather than back in — and one die escaping
     * the tray is the kind of thing a player never stops seeing.
     *
     * The bounds are the centre's, so they hold a die's own half-extent back from each surface: however
     * a cube is turned, its centre is never nearer a wall than that, and never below it on the floor.
     */
    private fun contain(body: DiceBody) {
        val half = body.halfExtent
        val limitZ = (DiceEngineTuning.TRAY_HALF_Z - half).coerceAtLeast(0f)
        val position = body.position
        val z = position.z.coerceIn(-limitZ, limitZ)
        val limitX = (DiceEngineTuning.trayHalfWidth(z) - half).coerceAtLeast(0f)
        val x = position.x.coerceIn(-limitX, limitX)
        val y = position.y.coerceIn(half, DiceEngineTuning.TRAY_CEILING)
        if (x == position.x && y == position.y && z == position.z) return

        body.position = Vec3(x, y, z)
        val velocity = body.velocity
        body.velocity = Vec3(
            if (x == position.x) velocity.x else 0f,
            if (y == position.y) velocity.y else 0f,
            if (z == position.z) velocity.z else 0f
        )
    }

    /** Publishes the step's hardest meaningful landing at a bounded rate. */
    private fun reportImpact(deltaSeconds: Float) {
        sinceImpactEvent += deltaSeconds
        val speed = solver.strongestImpact
        if (speed < DiceEngineTuning.IMPACT_EVENT_MIN_SPEED) return
        if (sinceImpactEvent < DiceEngineTuning.IMPACT_EVENT_INTERVAL) return
        sinceImpactEvent = 0f
        val strength = (speed / IMPACT_FULL_SPEED).coerceIn(MIN_REPORTED_IMPACT, 1f)
        eventChannel.trySend(
            DiceEngineEvent.Impact(strength, solver.strongestImpactMaterial)
        )
    }

    private fun updateSleep(deltaSeconds: Float) {
        for (index in 0 until count) {
            if (index == draggedIndex) {
                bodies[index].wake()
                continue
            }
            bodies[index].updateSleep(
                deltaSeconds = deltaSeconds,
                linearLimit = DiceEngineTuning.SLEEP_LINEAR_SPEED,
                angularLimit = DiceEngineTuning.SLEEP_ANGULAR_SPEED,
                delaySeconds = DiceEngineTuning.SLEEP_DELAY_SECONDS
            )
        }
    }

    /**
     * Ends the roll once every die is physically asleep, with a safety net for a pathological body.
     *
     * The timeout only freezes translation and rotation; it never changes orientation. This keeps a
     * wedged or numerically noisy die from holding the UI open without ever changing a face the user
     * has already seen.
     */
    private fun advancePhase() {
        if (phase != DicePhase.ROLLING) return
        if (draggedIndex != NO_DIE) return

        if (phaseSeconds >= DiceEngineTuning.ROLL_TIMEOUT_SECONDS) {
            for (index in 0 until count) {
                bodies[index].forceSleep(
                    trayNearHalfX = DiceEngineTuning.TRAY_NEAR_HALF_X,
                    trayFarHalfX = DiceEngineTuning.TRAY_FAR_HALF_X,
                    trayHalfZ = DiceEngineTuning.TRAY_HALF_Z
                )
            }
        }

        for (index in 0 until count) {
            if (!bodies[index].asleep) return
        }
        phase = DicePhase.SETTLED
        phaseSeconds = 0f
        eventChannel.trySend(DiceEngineEvent.Settled(List(count) { bodies[it].upValue() }))
    }

    private fun publish() {
        // A roll in progress is never still, and neither is a tray being moved about.
        var resting = phase != DicePhase.ROLLING && phase != DicePhase.WINDUP &&
            draggedIndex == NO_DIE &&
            deviceAcceleration.lengthSquared < RESTING_MOTION_G * RESTING_MOTION_G

        for (index in 0 until count) {
            val body = bodies[index]
            if (!body.asleep) resting = false
            body.position.writeTo(positions, index * 3)
            val orientation = body.orientation
            val slot = index * 4
            orientations[slot] = orientation.w
            orientations[slot + 1] = orientation.x
            orientations[slot + 2] = orientation.y
            orientations[slot + 3] = orientation.z
            values[index] = body.upValue()
        }

        published.set(
            DiceSnapshot(
                count = count,
                halfExtent = halfExtent,
                positions = positions,
                orientations = orientations,
                values = values,
                phase = phase,
                trayPitchRadians = trayPitchRadians(),
                selectedIndex = draggedIndex,
                settled = resting
            )
        )
    }

    /**
     * Gravity as the dice feel it: a fixed downward pull plus the pseudo-force of the phone actually
     * being accelerated. Merely holding the phone at another angle does not move the dice.
     *
     * What survives all of that is a floor: enough of the pull is always left pointing down that the
     * dice cannot be lifted off the felt, however hard the phone is shaken. A tick of haptic feedback
     * reads to the accelerometer as several g, and every landing fires one — without this, a die that
     * lands hard enough to be felt is a die thrown upwards by the feeling of it.
     */
    private fun effectiveGravity(): Vec3 {
        val combined = gravityDirection * DiceEngineTuning.GRAVITY -
            deviceAcceleration * (DiceEngineTuning.GRAVITY * DiceEngineTuning.MOTION_GRAVITY_SCALE)
        val least = DiceEngineTuning.GRAVITY * DiceEngineTuning.MIN_GRAVITY_FRACTION
        val downward = combined dot gravityDirection
        if (downward >= least) return combined
        return combined + gravityDirection * (least - downward)
    }

    private fun wakeAll() {
        var woke = false
        for (index in 0 until count) {
            val body = bodies[index]
            if (!body.asleep) continue
            woke = true
            body.wake()
        }
        if (woke && phase == DicePhase.SETTLED) {
            phase = DicePhase.ROLLING
            phaseSeconds = 0f
        }
    }

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

    private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

    private fun smoothstep(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return clamped * clamped * (3f - 2f * clamped)
    }

    private fun smootherstep(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return clamped * clamped * clamped * (clamped * (clamped * 6f - 15f) + 10f)
    }

    private companion object {
        val TWO_PI = (2.0 * Math.PI).toFloat()

        /** Spacing between dice laid out at rest, as a multiple of a die's own size. */
        const val REST_SPACING = 2.25f

        /** How square-on a contact has to be to count as holding a die up. */
        const val SUPPORT_ALIGNMENT = 0.5f

        /** Closing speed at which a published impact reaches full physical strength. */
        const val IMPACT_FULL_SPEED = 10f
        const val MIN_REPORTED_IMPACT = 0.1f

        /** Device movement, in g, worth waking a resting tray for. */
        const val MOTION_WAKE_G = 0.24f

        /** Device movement, in g, under which the tray may be called still. */
        const val RESTING_MOTION_G = 0.02f

        /** Shape of the phone-gesture impulse across the tray. */
        const val PITCH_NEAR_LIFT = 0.50f
        const val PITCH_FAR_LIFT = 1.18f
        const val PITCH_FORWARD_SPEED = 1.4f
        const val POSITION_OUTWARD_SPEED = 0.55f

        const val TRAY_REACTION_SECONDS = 0.30f
        const val TRAY_REACTION_MIN_RADIANS = 0.055f
        const val TRAY_REACTION_MAX_RADIANS = 0.13f
        const val VERTICAL_REACTION_SCALE = 0.42f

        const val NO_DIE = -1
        const val DRAG_MIN_HEIGHT_MULTIPLIER = 1.8f
        const val DRAG_MAX_HEIGHT = 3.2f
        const val DRAG_SPRING = 72f
        const val DRAG_DAMPING = 15f
        const val DRAG_MAX_ACCELERATION = 90f
        const val DRAG_MAX_RELEASE_SPEED = 12f
        const val DRAG_FLING_START_SPEED = 0.8f
        const val DRAG_DROP_LIFT = 0.35f
        const val DRAG_FLING_LIFT_MIN = 1.4f
        const val DRAG_FLING_LIFT_MAX = 5.2f
        const val DRAG_FLING_SPIN = 3.2f
        const val DRAG_RETAINED_SPIN = 0.25f

        const val TAP_LIFT = 6.2f
        const val TAP_SCATTER = 0.65f
        const val TAP_SPIN = 19f
        const val TAP_RETAINED_VELOCITY = 0.2f

    }
}
