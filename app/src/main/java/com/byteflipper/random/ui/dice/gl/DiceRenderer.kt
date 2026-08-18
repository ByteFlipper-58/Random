package com.byteflipper.random.ui.dice.gl

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.byteflipper.random.R
import com.byteflipper.random.domain.physics.Quat
import com.byteflipper.random.domain.physics.Vec3
import com.byteflipper.random.domain.dice.physics.DiceCommand
import com.byteflipper.random.domain.dice.physics.DiceEngine
import com.byteflipper.random.domain.dice.physics.DiceEngineTuning
import com.byteflipper.random.domain.dice.physics.DiceSnapshot
import com.byteflipper.random.ui.gl.AutoQualityMeter
import com.byteflipper.random.ui.gl.GlProgram
import com.byteflipper.random.ui.gl.Mesh
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Draws the tray in three passes: the themed backdrop, the floor with the felt and the shadows painted
 * onto it, and then one rounded cube per die.
 *
 * Everything here runs on the GL thread. Each frame steps [engine] by the time the last one took and
 * draws the snapshot it publishes; input goes the other way as engine commands, so neither thread ever
 * touches the other's state.
 *
 * The camera is fixed, and where it sits is not a free choice. It looks down on the tray steeply enough
 * that the horizon stays out of shot, from the near side, with screen-up pointing along the tray's far
 * axis — which is the arrangement the engine's sensor mapping already assumes when it turns a shove of
 * the phone into a shove of the dice.
 */
class DiceRenderer(
    private val context: Context,
    private val engine: DiceEngine
) : GLSurfaceView.Renderer {

    private var backdropProgram: GlProgram? = null
    private var floorProgram: GlProgram? = null
    private var dieProgram: GlProgram? = null

    private var backdrop: Mesh? = null
    private var floor: Mesh? = null
    private var die: Mesh? = null

    /**
     * True once a frame has actually reached the display.
     *
     * Read from the UI thread, which uses it to drop the still tray it draws in the meantime.
     */
    @Volatile
    private var presented = false
    val hasDrawnFrame: Boolean get() = presented

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val inverseViewProjection = FloatArray(16)
    private val trayModel = FloatArray(16)
    private val model = FloatArray(16)
    private val presentedModel = FloatArray(16)

    /** How far back the camera sits, fitted to the tray for the aspect ratio it is drawing at. */
    private var cameraDistance = 16f

    /** Viewport in pixels; the floor shader recomputes the backdrop gradient against it. */
    private var viewportWidth = 0
    private var viewportHeight = 0

    /** GL-thread touch state. Screen input is queued here in order by [GLSurfaceView]. */
    private var touchedDie = NO_DIE
    private var touchBlocked = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartMillis = 0L
    private var touchSlopSquared = 0f
    private var touchMoved = false
    private var dragPlaneHeight = 0f
    private var lastDragTarget = Vec3.ZERO
    private var lastDragMillis = 0L
    private var filteredDragVelocity = Vec3.ZERO

    @Volatile
    private var onEmptyTap: (() -> Unit)? = null

    @Volatile
    private var backdropTop = floatArrayOf(0.09f, 0.10f, 0.13f)

    @Volatile
    private var backdropBottom = floatArrayOf(0.02f, 0.02f, 0.03f)

    @Volatile
    private var backdropGlow = floatArrayOf(0.35f, 0.42f, 0.60f)

    @Volatile
    private var feltColor = floatArrayOf(0.10f, 0.22f, 0.16f)

    @Volatile
    private var rimColor = floatArrayOf(0.26f, 0.30f, 0.36f)

    /** Three floats per die, in the order the engine simulates them. Cut for the whole tray. */
    @Volatile
    private var dieColors = FloatArray(DiceEngineTuning.MAX_DICE * 3) {
        DEFAULT_DIE_COLOR[it % 3]
    }

    /** True while the tier is ours to choose; set from the UI, read on the GL thread. */
    @Volatile
    private var autoQuality = false

    /** GL-thread mirror of [autoQuality], so the meter is built and dropped on this thread only. */
    private var metering = false
    private var qualityMeter: AutoQualityMeter? = null

    private var lastFrameNanos = 0L

    /** Backdrop colours, straight from `MaterialTheme.colorScheme`. */
    fun setBackdropColors(top: FloatArray, bottom: FloatArray, glow: FloatArray) {
        backdropTop = top
        backdropBottom = bottom
        backdropGlow = glow
    }

    /** The tray's own two colours: the felt the dice land on and the raised edge around it. */
    fun setTrayColors(felt: FloatArray, rim: FloatArray) {
        feltColor = felt
        rimColor = rim
    }

    /**
     * Sets what colour each die is, three floats each.
     *
     * Copied into an array cut for the whole tray, cycling what it is given, so the draw loop can index
     * it by die without a bounds check and no die ever comes out black for the want of a colour.
     */
    fun setDieColors(colors: FloatArray) {
        val usable = colors.size - colors.size % 3
        if (usable == 0) return
        dieColors = FloatArray(DiceEngineTuning.MAX_DICE * 3) { colors[it % usable] }
    }

    /**
     * Hands the tier choice to the frame meter, or takes it back.
     *
     * On means the renderer measures a few windows of frames and retunes the engine to what the device
     * can hold. Off means the player picked a tier and the view model owns it; switching back on starts
     * the measurement over, since by then the answer may have changed.
     */
    fun setAutoQuality(enabled: Boolean) {
        autoQuality = enabled
    }

    fun setOnEmptyTap(callback: () -> Unit) {
        onEmptyTap = callback
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glFrontFace(GLES30.GL_CCW)
        GLES30.glClearColor(0f, 0f, 0f, 1f)

        backdropProgram = GlProgram.fromRaw(
            context = context,
            vertexRes = R.raw.dice_backdrop_vert,
            fragmentRes = R.raw.dice_backdrop_frag
        )
        floorProgram = GlProgram.fromRaw(
            context = context,
            vertexRes = R.raw.dice_floor_vert,
            fragmentRes = R.raw.dice_floor_frag
        )
        dieProgram = GlProgram.fromRaw(
            context = context,
            vertexRes = R.raw.dice_cube_vert,
            fragmentRes = R.raw.dice_cube_frag
        )
        backdrop = DiceMeshFactory.fullscreenQuad()
        floor = DiceMeshFactory.floorQuad(FLOOR_HALF_EXTENT)
        die = DiceMeshFactory.roundedDie()
        lastFrameNanos = 0L
        presented = false
        // A new surface may well be a new display mode, so whatever the meter concluded last time does
        // not carry over.
        metering = false
        qualityMeter = null
    }

    /**
     * Backs the camera off until the whole tray is in shot.
     *
     * The camera looks at a point [AIM_HEIGHT] above the middle of the felt from `distance * (0, sin E,
     * cos E)` beyond it, which puts a point `(x, y, z)` at depth `distance + (AIM_HEIGHT - y) sin E -
     * z cos E`, `x` to the side and `(y - AIM_HEIGHT) cos E - z sin E` up. Aiming over the tray rather
     * than at it is what drops the felt into the lower part of the screen: everything in shot slides
     * down by `AIM_HEIGHT cos E`, and the room that opens up above it is where a thrown die is.
     *
     * Three constraints come out of that, one per edge that can run out of frame first — the side, at
     * the near corner where the tray is closest; the far edge, which the lifted aim pushes up; and the
     * near edge, which it pushes down. Whichever needs the most room decides, and on a portrait phone
     * that is the width, so the lift and the tilt cost no scale at all.
     *
     * The tray is fitted a little larger than it is, so its rim and the shadow it drops are in shot
     * rather than resting against the edge of the screen.
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        viewportWidth = width
        viewportHeight = height

        val aspect = if (height == 0) 1f else width.toFloat() / height.toFloat()
        val tanVertical = tan(Math.toRadians(FOV_DEGREES / 2.0)).toFloat()
        val tanHorizontal = tanVertical * aspect
        val nearHalfX = DiceEngineTuning.TRAY_NEAR_HALF_X + TRAY_FRAME
        val farHalfX = DiceEngineTuning.TRAY_FAR_HALF_X + TRAY_FRAME
        val halfZ = DiceEngineTuning.TRAY_HALF_Z + TRAY_FRAME
        val lift = AIM_HEIGHT * SIN_ELEVATION
        val drop = AIM_HEIGHT * COS_ELEVATION
        val sideNear = nearHalfX / tanHorizontal - lift + halfZ * COS_ELEVATION
        val sideFar = farHalfX / tanHorizontal - lift - halfZ * COS_ELEVATION
        val side = max(sideNear, sideFar)
        val far = (halfZ * SIN_ELEVATION - drop) / tanVertical - lift - halfZ * COS_ELEVATION
        val near = (halfZ * SIN_ELEVATION + drop) / tanVertical - lift + halfZ * COS_ELEVATION
        cameraDistance = max(side, max(far, near))

        Matrix.perspectiveM(projection, 0, FOV_DEGREES, aspect, NEAR_PLANE, FAR_PLANE)
        Matrix.setLookAtM(
            view, 0,
            0f, cameraDistance * SIN_ELEVATION + AIM_HEIGHT, cameraDistance * COS_ELEVATION,
            0f, AIM_HEIGHT, 0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
        Matrix.invertM(inverseViewProjection, 0, viewProjection, 0)
    }

    /** Starts either a one-die interaction or a possible tap on the empty tray. Runs on the GL thread. */
    fun touchDown(x: Float, y: Float, eventMillis: Long, touchSlop: Float) {
        val snapshot = engine.snapshot()
        touchBlocked = snapshot.phase == com.byteflipper.random.domain.dice.physics.DicePhase.WINDUP
        touchStartX = x
        touchStartY = y
        touchStartMillis = eventMillis
        touchSlopSquared = touchSlop * touchSlop
        touchMoved = false
        filteredDragVelocity = Vec3.ZERO
        lastDragMillis = eventMillis
        touchedDie = if (touchBlocked) NO_DIE else pickDie(x, y, snapshot)
        if (touchedDie == NO_DIE) return

        val positionSlot = touchedDie * 3
        dragPlaneHeight = max(
            snapshot.positions[positionSlot + 1],
            snapshot.halfExtent * DRAG_HEIGHT_MULTIPLIER
        )
        val fallback = Vec3(
            snapshot.positions[positionSlot],
            dragPlaneHeight,
            snapshot.positions[positionSlot + 2]
        )
        val target = screenPointOnPlane(x, y, dragPlaneHeight) ?: fallback
        lastDragTarget = target
        engine.submit(DiceCommand.BeginDrag(touchedDie, target))
    }

    fun touchMove(x: Float, y: Float, eventMillis: Long) {
        if (touchBlocked) return
        val dx = x - touchStartX
        val dy = y - touchStartY
        if (dx * dx + dy * dy > touchSlopSquared) touchMoved = true
        if (touchedDie == NO_DIE) return

        val target = screenPointOnPlane(x, y, dragPlaneHeight) ?: return
        val deltaMillis = eventMillis - lastDragMillis
        if (deltaMillis > 0L) {
            val seconds = deltaMillis / 1_000f
            val instant = (target - lastDragTarget) / seconds
            filteredDragVelocity = filteredDragVelocity.lerp(
                instant.clampLength(MAX_TRACKED_DRAG_SPEED),
                DRAG_VELOCITY_FOLLOW
            )
        }
        lastDragTarget = target
        lastDragMillis = eventMillis
        engine.submit(DiceCommand.Drag(touchedDie, target, filteredDragVelocity))
    }

    fun touchUp(x: Float, y: Float, eventMillis: Long) {
        if (touchBlocked) {
            clearTouch()
            return
        }
        touchMove(x, y, eventMillis)
        val elapsed = eventMillis - touchStartMillis
        if (touchedDie != NO_DIE) {
            if (!touchMoved && elapsed <= DIE_TAP_MILLIS) {
                engine.submit(DiceCommand.TapDie(touchedDie))
            } else {
                engine.submit(DiceCommand.EndDrag(touchedDie, filteredDragVelocity))
            }
        } else if (!touchMoved && elapsed <= EMPTY_TAP_MILLIS) {
            onEmptyTap?.invoke()
        }
        clearTouch()
    }

    fun touchCancel() {
        if (touchedDie != NO_DIE) {
            engine.submit(DiceCommand.EndDrag(touchedDie, Vec3.ZERO))
        }
        clearTouch()
    }

    private fun clearTouch() {
        touchedDie = NO_DIE
        touchBlocked = false
        touchMoved = false
        filteredDragVelocity = Vec3.ZERO
    }

    private fun pickDie(x: Float, y: Float, snapshot: DiceSnapshot): Int {
        val ray = screenRay(x, y) ?: return NO_DIE
        var picked = NO_DIE
        var nearest = Float.POSITIVE_INFINITY
        for (index in 0 until snapshot.count) {
            val positionSlot = index * 3
            val center = Vec3(
                snapshot.positions[positionSlot],
                snapshot.positions[positionSlot + 1],
                snapshot.positions[positionSlot + 2]
            )
            val turn = index * 4
            val orientation = Quat(
                snapshot.orientations[turn],
                snapshot.orientations[turn + 1],
                snapshot.orientations[turn + 2],
                snapshot.orientations[turn + 3]
            )
            val distance = rayBoxDistance(ray, center, orientation, snapshot.halfExtent) ?: continue
            if (distance < nearest) {
                nearest = distance
                picked = index
            }
        }
        return picked
    }

    private fun rayBoxDistance(ray: Ray, center: Vec3, orientation: Quat, half: Float): Float? {
        val origin = orientation.inverseRotate(ray.origin - center)
        val direction = orientation.inverseRotate(ray.direction)
        var near = 0f
        var far = Float.POSITIVE_INFINITY
        for (axis in 0..2) {
            val originAxis = component(origin, axis)
            val directionAxis = component(direction, axis)
            if (abs(directionAxis) < RAY_EPSILON) {
                if (originAxis !in -half..half) return null
                continue
            }
            var first = (-half - originAxis) / directionAxis
            var second = (half - originAxis) / directionAxis
            if (first > second) {
                val swap = first
                first = second
                second = swap
            }
            near = max(near, first)
            far = min(far, second)
            if (near > far) return null
        }
        return near.takeIf { it >= 0f }
    }

    private fun screenPointOnPlane(x: Float, y: Float, planeY: Float): Vec3? {
        val ray = screenRay(x, y) ?: return null
        if (abs(ray.direction.y) < RAY_EPSILON) return null
        val distance = (planeY - ray.origin.y) / ray.direction.y
        if (distance <= 0f) return null
        return ray.origin + ray.direction * distance
    }

    private fun screenRay(x: Float, y: Float): Ray? {
        if (viewportWidth <= 0 || viewportHeight <= 0) return null
        val ndcX = x / viewportWidth * 2f - 1f
        val ndcY = 1f - y / viewportHeight * 2f
        val near = unproject(ndcX, ndcY, -1f) ?: return null
        val far = unproject(ndcX, ndcY, 1f) ?: return null
        return Ray(near, (far - near).normalized(Vec3.DOWN))
    }

    private fun unproject(x: Float, y: Float, z: Float): Vec3? {
        val input = floatArrayOf(x, y, z, 1f)
        val output = FloatArray(4)
        Matrix.multiplyMV(output, 0, inverseViewProjection, 0, input, 0)
        val w = output[3]
        if (!w.isFinite() || abs(w) < RAY_EPSILON) return null
        return Vec3(output[0] / w, output[1] / w, output[2] / w)
            .takeIf { it.isFinite }
    }

    private fun component(vector: Vec3, axis: Int): Float = when (axis) {
        0 -> vector.x
        1 -> vector.y
        else -> vector.z
    }

    override fun onDrawFrame(gl: GL10?) {
        val frameSeconds = advanceClock()
        engine.advance(frameSeconds)
        val snapshot = engine.snapshot()
        writeTrayModel(snapshot.trayPitchRadians)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        drawBackdrop()
        drawFloor(snapshot)
        drawDice(snapshot)

        trackQuality(frameSeconds)
        presented = true
        throttleWhenSettled(snapshot.settled)
    }

    fun release() {
        backdrop?.release()
        floor?.release()
        die?.release()
        backdropProgram?.release()
        floorProgram?.release()
        dieProgram?.release()
        backdrop = null
        floor = null
        die = null
        backdropProgram = null
        floorProgram = null
        dieProgram = null
    }

    private fun advanceClock(): Float {
        val now = System.nanoTime()
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now
            return 0f
        }
        val delta = (now - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = now
        // A frame that took ages (surface resumed, GC pause) must not fling a die through a wall.
        return delta.coerceIn(0f, 0.05f)
    }

    /**
     * Feeds the frame meter and retunes the engine when it changes its mind.
     *
     * The retune is queued rather than applied here — the engine picks it up at the top of its next
     * step, where changing the solver's workload is safe.
     */
    private fun trackQuality(frameSeconds: Float) {
        val auto = autoQuality
        if (auto != metering) {
            metering = auto
            qualityMeter = if (auto) AutoQualityMeter(engine.tuning.tier) else null
        }
        val next = qualityMeter?.observe(frameSeconds) ?: return
        engine.setTuning(DiceEngineTuning.forTier(next))
    }

    /**
     * Holds the GL thread back to [IDLE_FRAME_NANOS] once every die has stopped.
     *
     * A settled tray is a still picture, and redrawing it sixty times a second is a warm phone for
     * nothing. It is still redrawn, though, rather than left alone: the phone can be tilted or shaken at
     * any moment, and a surface that had stopped drawing would answer that a frame late.
     *
     * Never while a measurement window is open: a throttled frame would read as a device that cannot
     * keep up.
     */
    private fun throttleWhenSettled(settled: Boolean) {
        if (!settled || qualityMeter?.isMeasuring == true) return

        val spent = System.nanoTime() - lastFrameNanos
        val remaining = IDLE_FRAME_NANOS - spent
        if (remaining < MIN_SLEEP_NANOS) return
        runCatching { Thread.sleep(remaining / 1_000_000L) }
    }

    private fun drawBackdrop() {
        val program = backdropProgram ?: return
        val mesh = backdrop ?: return

        program.use()
        // Behind everything by construction; it neither tests nor writes depth.
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)

        GLES30.glUniform3fv(program.uniform("uTopColor"), 1, backdropTop, 0)
        GLES30.glUniform3fv(program.uniform("uBottomColor"), 1, backdropBottom, 0)
        GLES30.glUniform3fv(program.uniform("uGlowColor"), 1, backdropGlow, 0)

        mesh.draw(program)

        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    /**
     * The floor, which is where the tray itself is drawn.
     *
     * The dice go in as uniforms rather than being drawn again into a shadow map: ten centres and a
     * radius is all a contact shadow needs, and at this angle nothing casts onto anything but the felt.
     */
    private fun drawFloor(snapshot: DiceSnapshot) {
        val program = floorProgram ?: return
        val mesh = floor ?: return

        program.use()
        GLES30.glUniformMatrix4fv(program.uniform("uViewProjection"), 1, false, viewProjection, 0)
        GLES30.glUniformMatrix4fv(program.uniform("uTrayModel"), 1, false, trayModel, 0)
        GLES30.glUniform3f(
            program.uniform("uTrayHalf"),
            DiceEngineTuning.TRAY_NEAR_HALF_X,
            DiceEngineTuning.TRAY_FAR_HALF_X,
            DiceEngineTuning.TRAY_HALF_Z
        )
        GLES30.glUniform1f(program.uniform("uTrayCorner"), TRAY_CORNER_RADIUS)
        GLES30.glUniform3fv(program.uniform("uFeltColor"), 1, feltColor, 0)
        GLES30.glUniform3fv(program.uniform("uRimColor"), 1, rimColor, 0)
        GLES30.glUniform3fv(program.uniform("uTopColor"), 1, backdropTop, 0)
        GLES30.glUniform3fv(program.uniform("uBottomColor"), 1, backdropBottom, 0)
        GLES30.glUniform3fv(program.uniform("uGlowColor"), 1, backdropGlow, 0)
        GLES30.glUniform2f(
            program.uniform("uViewport"),
            viewportWidth.toFloat().coerceAtLeast(1f),
            viewportHeight.toFloat().coerceAtLeast(1f)
        )
        GLES30.glUniform1f(program.uniform("uDieHalf"), snapshot.halfExtent)
        GLES30.glUniform1f(
            program.uniform("uSoftShadows"),
            if (engine.tuning.softShadows) 1f else 0f
        )

        val count = snapshot.count
        GLES30.glUniform1i(program.uniform("uDiceCount"), count)
        GLES30.glUniform1i(program.uniform("uSelectedDie"), snapshot.selectedIndex)
        if (count > 0) {
            GLES30.glUniform3fv(program.uniform("uDicePos"), count, snapshot.positions, 0)
        }

        mesh.draw(program)
    }

    /**
     * The dice, one draw call each.
     *
     * Ten calls is nothing next to the state changes batching them would cost: they share a mesh and a
     * program, and all that changes between them is a matrix and a colour. What they cannot share is the
     * matrix, and instancing ten cubes would mean a buffer upload per frame to save nine calls.
     */
    private fun drawDice(snapshot: DiceSnapshot) {
        val program = dieProgram ?: return
        val mesh = die ?: return
        if (snapshot.count == 0) return

        program.use()
        GLES30.glUniformMatrix4fv(program.uniform("uViewProjection"), 1, false, viewProjection, 0)
        GLES30.glUniform3f(
            program.uniform("uCameraPos"),
            0f,
            cameraDistance * SIN_ELEVATION + AIM_HEIGHT,
            cameraDistance * COS_ELEVATION
        )
        GLES30.glUniform3fv(program.uniform("uLightDir"), 1, LIGHT_DIR, 0)
        GLES30.glUniform3fv(program.uniform("uPipColor"), 1, PIP_COLOR, 0)
        // The die is lit by the room it is in: the backdrop above it, the felt underneath.
        GLES30.glUniform3fv(program.uniform("uSkyColor"), 1, backdropTop, 0)
        GLES30.glUniform3fv(program.uniform("uBounceColor"), 1, feltColor, 0)

        val modelUniform = program.uniform("uModel")
        val colorUniform = program.uniform("uColor")
        val selectedUniform = program.uniform("uSelected")
        val colors = dieColors
        for (index in 0 until snapshot.count) {
            writeModelMatrix(snapshot, index)
            if (snapshot.phase == com.byteflipper.random.domain.dice.physics.DicePhase.WINDUP) {
                Matrix.multiplyMM(presentedModel, 0, trayModel, 0, model, 0)
            } else {
                System.arraycopy(model, 0, presentedModel, 0, model.size)
            }
            GLES30.glUniformMatrix4fv(modelUniform, 1, false, presentedModel, 0)
            val slot = index * 3
            GLES30.glUniform3f(colorUniform, colors[slot], colors[slot + 1], colors[slot + 2])
            GLES30.glUniform1f(selectedUniform, if (index == snapshot.selectedIndex) 1f else 0f)
            mesh.draw(program)
        }
    }

    /**
     * Builds one die's model matrix: turn, then size, then place.
     *
     * The size is the same on all three axes and the translation goes in as a translation, so the upper
     * 3x3 stays a rotation times a scalar — which is why the shader can turn a normal with it and simply
     * normalise the result.
     */
    private fun writeModelMatrix(snapshot: DiceSnapshot, index: Int) {
        val turn = index * 4
        Quat(
            snapshot.orientations[turn],
            snapshot.orientations[turn + 1],
            snapshot.orientations[turn + 2],
            snapshot.orientations[turn + 3]
        ).writeMatrix(model)
        Matrix.scaleM(model, 0, snapshot.halfExtent, snapshot.halfExtent, snapshot.halfExtent)

        val at = index * 3
        model[12] = snapshot.positions[at]
        model[13] = snapshot.positions[at + 1]
        model[14] = snapshot.positions[at + 2]
    }

    /** Rotates the tray and attached dice around the near edge during the tap-throw wind-up. */
    private fun writeTrayModel(pitchRadians: Float) {
        Matrix.setIdentityM(trayModel, 0)
        if (pitchRadians == 0f) return
        Matrix.translateM(trayModel, 0, 0f, 0f, DiceEngineTuning.TRAY_HALF_Z)
        Matrix.rotateM(trayModel, 0, Math.toDegrees(pitchRadians.toDouble()).toFloat(), 1f, 0f, 0f)
        Matrix.translateM(trayModel, 0, 0f, 0f, -DiceEngineTuning.TRAY_HALF_Z)
    }

    private companion object {
        data class Ray(val origin: Vec3, val direction: Vec3)

        const val NO_DIE = -1
        const val RAY_EPSILON = 1e-5f
        const val DRAG_HEIGHT_MULTIPLIER = 2.2f
        const val DRAG_VELOCITY_FOLLOW = 0.42f
        const val MAX_TRACKED_DRAG_SPEED = 12f
        const val DIE_TAP_MILLIS = 260L
        const val EMPTY_TAP_MILLIS = 450L

        /**
         * Vertical field of view, and how far above the felt the camera looks down from.
         *
         * The angle is the whole composition. Flatter and the dice hide behind each other and the far
         * wall would have to be drawn; steeper and a die stops being a cube and becomes a square with a
         * number on it. It is also what the engine assumes: it maps a shove of the phone onto the tray
         * as though screen-up ran along the tray's far axis, which is only true from up here.
         *
         * Low enough to be a view across the tray rather than down into it — at this angle a tray as
         * deep as this one is nearly twice as wide as it is tall on screen, and that foreshortening is
         * the only depth cue a flat screen gets for free. The dice have to read as cubes at rest, which
         * is what stops it going lower.
         */
        const val FOV_DEGREES = 42f
        const val ELEVATION_DEGREES = 42f
        val SIN_ELEVATION = sin(Math.toRadians(ELEVATION_DEGREES.toDouble())).toFloat()
        val COS_ELEVATION = cos(Math.toRadians(ELEVATION_DEGREES.toDouble())).toFloat()

        /**
         * How far above the felt the camera aims, which is how far down the screen the tray sits.
         *
         * Aiming level with the felt centres it, and a tray dead centre has the same amount of nothing
         * above it as below. Aiming over it instead drops the whole tray into the lower two thirds and
         * leaves the top of the screen to the dice while they are in the air — which is where a throw
         * happens, and the reason to be watching at all.
         */
        const val AIM_HEIGHT = 0.85f

        /** How much room past the tray the camera leaves, for its rim and the shadow it drops. */
        const val TRAY_FRAME = 0.42f

        /** Corner radius of the felt; the straight side segments match the physical trapezoid. */
        const val TRAY_CORNER_RADIUS = 0.42f

        /**
         * Reach of the floor quad. Far larger than the tray, because from this angle the horizon never
         * comes into shot and the floor is therefore the whole background.
         */
        const val FLOOR_HALF_EXTENT = 30f

        /** Nothing is nearer than about ten units or further than about thirty; this is room to spare. */
        const val NEAR_PLANE = 1f
        const val FAR_PLANE = 60f

        /** Frame budget once the dice have stopped: a still picture at 30 fps. */
        const val IDLE_FRAME_NANOS = 1_000_000_000L / 30L

        /** Below a millisecond a sleep costs more than the frame it would save. */
        const val MIN_SLEEP_NANOS = 1_500_000L

        /** Above and a little to the near left, so the faces the camera can see are the lit ones. */
        val LIGHT_DIR = normalized(-0.40f, 0.85f, 0.34f)

        /** The pips, in the same near-white the 2D dice print them in. */
        val PIP_COLOR = floatArrayOf(0.96f, 0.96f, 0.96f)

        /** Moulded white plastic, for the frame or two before the UI has said what colour it wants. */
        val DEFAULT_DIE_COLOR = floatArrayOf(0.90f, 0.91f, 0.93f)

        fun normalized(x: Float, y: Float, z: Float): FloatArray {
            val length = sqrt(x * x + y * y + z * z)
            if (length < 1e-6f) return floatArrayOf(0f, 1f, 0f)
            return floatArrayOf(x / length, y / length, z / length)
        }
    }
}
