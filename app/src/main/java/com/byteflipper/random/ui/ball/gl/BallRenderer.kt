package com.byteflipper.random.ui.ball.gl

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.byteflipper.random.R
import com.byteflipper.random.domain.ball.physics.BallEngine
import com.byteflipper.random.domain.ball.physics.BallEngineTuning
import com.byteflipper.random.domain.ball.physics.BallSnapshot
import com.byteflipper.random.domain.ball.physics.DieGeometry
import com.byteflipper.random.domain.ball.physics.FluidFrame
import com.byteflipper.random.domain.ball.physics.Vec3
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * How much of the shorter viewport side the ball takes up, in NDC half-units — so its radius on screen
 * is half of this, whatever the aspect ratio.
 */
internal const val BALL_SCREEN_FRACTION = 0.78f

/**
 * The ball's on-screen radius as a fraction of the shorter side, in pixels.
 *
 * [BallGlSurface] works from the same figure: the placeholder has to sit exactly where the real ball
 * will appear, and a drag has to turn the shell by the arc the finger actually travelled across it.
 */
internal const val BALL_RADIUS_PER_SHORT_SIDE = BALL_SCREEN_FRACTION / 2f

/**
 * Draws the ball in three passes: the themed backdrop, the black shell with a hole where the answer
 * window is, and the interior seen through that hole.
 *
 * Everything here runs on the GL thread. Each frame steps [engine] by the measured frame time and
 * renders the snapshot it publishes; input goes the other way as engine commands, so the two threads
 * never share mutable state.
 */
class BallRenderer(
    private val context: Context,
    private val engine: BallEngine
) : GLSurfaceView.Renderer {

    private var backgroundProgram: GlProgram? = null
    private var shellProgram: GlProgram? = null
    private var interiorProgram: GlProgram? = null

    /** Set on a new surface, cleared once the interior program has been built. See [onDrawFrame]. */
    private var interiorPending = false

    /**
     * True once a frame has actually reached the display.
     *
     * Read from the UI thread, which uses it to drop the placeholder ball it draws in the meantime.
     */
    @Volatile
    private var presented = false
    val hasDrawnFrame: Boolean get() = presented
    private var quad: Mesh? = null
    private var sphere: Mesh? = null
    private var densityTexture: GlTexture3d? = null
    private var answerAtlas: TextAtlas? = null

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val model = FloatArray(16)
    private val viewModel = FloatArray(16)
    private val mvp = FloatArray(16)
    private val dieRotation = FloatArray(16)
    private val dieRotation3 = FloatArray(9)
    private val scratch = FloatArray(4)
    private val projectPoint = FloatArray(4)
    private val projectClip = FloatArray(4)
    private val ballCenter = floatArrayOf(0.5f, 0.5f)

    private var aspect = 1f
    private var tanHalfFov = tan(Math.toRadians(FOV_DEGREES / 2.0)).toFloat()
    private var cameraDistance = 4f

    /** Viewport size in pixels; the window's scissor and the text's mip level are picked against it. */
    private var viewportWidth = 0
    private var viewportHeight = 0

    /** Ball radius as a fraction of the viewport height, shared with the backdrop shader. */
    private var ballRadiusFraction = 0.4f

    /** Screen rectangle the window can possibly cover, used to scissor the interior pass. */
    private var windowScissor = intArrayOf(0, 0, 0, 0)
    @Volatile
    private var backdropTop = floatArrayOf(0.09f, 0.10f, 0.13f)

    @Volatile
    private var backdropBottom = floatArrayOf(0.02f, 0.02f, 0.03f)

    @Volatile
    private var backdropGlow = floatArrayOf(0.35f, 0.42f, 0.60f)

    /** Text for each die face, pushed from the UI and picked up by the GL thread. */
    @Volatile
    private var answerLabels: List<String> = emptyList()

    /** True while the tier is ours to choose; set from the UI, read on the GL thread. */
    @Volatile
    private var autoQuality = false

    /** GL-thread mirror of [autoQuality], so the meter is built and dropped on this thread only. */
    private var metering = false
    private var qualityMeter: AutoQualityMeter? = null

    private var lastFrameNanos = 0L

    /** Seconds the surface has been alive; the interior's fizz animates off it. */
    private var elapsedSeconds = 0f

    /** Backdrop colours, straight from `MaterialTheme.colorScheme`. */
    fun setBackdropColors(top: FloatArray, bottom: FloatArray, glow: FloatArray) {
        backdropTop = top
        backdropBottom = bottom
        backdropGlow = glow
    }

    /**
     * Hands the tier choice to the frame meter, or takes it back.
     *
     * On means the renderer measures a few windows of frames and retunes the engine to what the
     * device can hold. Off means the player picked a tier and the view model owns it; switching back
     * on starts the measurement over, since by then the answer may have changed.
     */
    fun setAutoQuality(enabled: Boolean) {
        autoQuality = enabled
    }

    /**
     * Sets what each of the die's twenty faces says, in face order.
     *
     * The sheet is redrawn on the GL thread the next time it is needed, so the bitmap is never shared
     * between threads and a locale change costs one frame.
     */
    fun setAnswerLabels(labels: List<String>) {
        answerLabels = labels
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glFrontFace(GLES30.GL_CCW)
        GLES30.glClearColor(0f, 0f, 0f, 1f)

        backgroundProgram = GlProgram.fromRaw(
            context = context,
            vertexRes = R.raw.ball_background_vert,
            fragmentRes = R.raw.ball_background_frag
        )
        shellProgram = GlProgram.fromRaw(
            context = context,
            vertexRes = R.raw.ball_shell_vert,
            fragmentRes = R.raw.ball_shell_frag
        )
        // The interior's ray-marcher is by far the most expensive shader here — seconds of compiling
        // on some drivers — and none of it is needed to draw the ball itself. So it waits until a
        // frame with the shell in it has been presented, and the window is filled flat until then.
        interiorProgram = null
        interiorPending = true
        quad = MeshFactory.fullscreenQuad()
        sphere = MeshFactory.uvSphere()
        densityTexture = GlTexture3d(BallEngineTuning.DENSITY_RESOLUTION)
        answerAtlas = TextAtlas(context, ANSWER_TEXTURE_UNIT)
        lastFrameNanos = 0L
        presented = false
        // A new surface may well be a new display mode, so whatever the meter concluded last time
        // does not carry over.
        metering = false
        qualityMeter = null
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspect = if (height == 0) 1f else width.toFloat() / height.toFloat()
        viewportWidth = width
        viewportHeight = height

        // Pull the camera back just far enough for the ball to take up BALL_SCREEN_FRACTION of the
        // shorter side, whatever the aspect ratio turns out to be.
        val radiusNdc = BALL_SCREEN_FRACTION * min(1f, aspect)
        tanHalfFov = tan(Math.toRadians(FOV_DEGREES / 2.0)).toFloat()
        cameraDistance = 1f / (radiusNdc * tanHalfFov)
        ballRadiusFraction = radiusNdc / 2f

        Matrix.perspectiveM(projection, 0, FOV_DEGREES, aspect, 0.1f, 20f)
        Matrix.setLookAtM(
            view, 0,
            0f, 0f, cameraDistance,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
        windowScissor = computeWindowScissor(
            axis = BallEngineTuning.WINDOW_AXIS,
            offset = Vec3.ZERO,
            width = width,
            height = height
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        // The first frame is on screen by now, so the expensive program can be built without the ball
        // being late for it.
        if (interiorPending && presented) buildInteriorProgram()

        val frameSeconds = advanceClock()
        elapsedSeconds += frameSeconds
        engine.advance(frameSeconds)
        val snapshot = engine.snapshot()

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Where the window points after the shell's spin. The shell lights its rim with it and the
        // interior fills exactly the hole it cut, so both passes work from the one vector.
        val windowAxis = snapshot.shellOrientation.rotate(BallEngineTuning.WINDOW_AXIS)

        drawBackground(snapshot.shellOffset)
        drawShell(snapshot, windowAxis)
        drawInterior(snapshot, windowAxis)

        trackQuality(frameSeconds)
        presented = true
        throttleWhenSettled(snapshot.settled)
    }

    /**
     * Compiles the interior's ray-marcher, off the critical path.
     *
     * The clock is restarted afterwards: however long the driver took over it is not time the liquid
     * spent moving, and charging it to the simulation would jolt everything inside the ball.
     */
    private fun buildInteriorProgram() {
        interiorPending = false
        interiorProgram = GlProgram.fromRaw(
            context = context,
            vertexRes = R.raw.ball_interior_vert,
            fragmentRes = R.raw.ball_interior_frag
        )
        lastFrameNanos = 0L
    }

    fun release() {
        quad?.release()
        sphere?.release()
        densityTexture?.release()
        answerAtlas?.release()
        backgroundProgram?.release()
        shellProgram?.release()
        interiorProgram?.release()
        quad = null
        sphere = null
        densityTexture = null
        answerAtlas = null
        backgroundProgram = null
        shellProgram = null
        interiorProgram = null
    }

    private fun advanceClock(): Float {
        val now = System.nanoTime()
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now
            return 0f
        }
        val delta = (now - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = now
        // A frame that took ages (surface resumed, GC pause) must not fling the die across the ball.
        return delta.coerceIn(0f, 0.05f)
    }

    /**
     * Feeds the frame meter and retunes the engine when it changes its mind.
     *
     * The retune is queued rather than applied here — the engine picks it up at the top of its next
     * step, where changing the particle count is safe.
     */
    private fun trackQuality(frameSeconds: Float) {
        val auto = autoQuality
        if (auto != metering) {
            metering = auto
            qualityMeter = if (auto) AutoQualityMeter(engine.tuning.tier) else null
        }
        val next = qualityMeter?.observe(frameSeconds) ?: return
        engine.setTuning(BallEngineTuning.forTier(next))
    }

    /**
     * Holds the GL thread back to [IDLE_FRAME_NANOS] once nothing in the ball is moving.
     *
     * The liquid still creeps and the die still bobs a little at rest, so the frame is worth drawing —
     * just not sixty times a second. Physics keeps its fixed step either way; it simply consumes more
     * of them per frame.
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

    private fun drawBackground(shellOffset: Vec3) {
        val program = backgroundProgram ?: return
        val mesh = quad ?: return

        program.use()
        // The backdrop is behind everything by construction; it neither tests nor writes depth.
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)

        // The glow and the contact shadow follow the ball, so the whole picture drifts as one.
        projectToScreen(shellOffset, ballCenter)

        GLES30.glUniform3fv(program.uniform("uTopColor"), 1, backdropTop, 0)
        GLES30.glUniform3fv(program.uniform("uBottomColor"), 1, backdropBottom, 0)
        GLES30.glUniform3fv(program.uniform("uGlowColor"), 1, backdropGlow, 0)
        GLES30.glUniform2f(program.uniform("uBallCenter"), ballCenter[0], ballCenter[1])
        GLES30.glUniform1f(program.uniform("uBallRadius"), ballRadiusFraction)
        GLES30.glUniform1f(program.uniform("uAspect"), aspect)

        mesh.draw(program)

        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun drawShell(snapshot: BallSnapshot, windowAxisWorld: Vec3) {
        val program = shellProgram ?: return
        val mesh = sphere ?: return

        snapshot.shellOrientation.writeMatrix(model)
        // The rotation is left alone and the drift goes in as a plain translation, so mat3 of this
        // stays a pure rotation and the shell's normals need no fixing up.
        model[12] = snapshot.shellOffset.x
        model[13] = snapshot.shellOffset.y
        model[14] = snapshot.shellOffset.z
        Matrix.multiplyMM(viewModel, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, viewModel, 0)

        program.use()
        GLES30.glUniformMatrix4fv(program.uniform("uMvp"), 1, false, mvp, 0)
        GLES30.glUniformMatrix4fv(program.uniform("uModel"), 1, false, model, 0)

        GLES30.glUniform3f(program.uniform("uCameraPos"), 0f, 0f, cameraDistance)
        GLES30.glUniform3fv(program.uniform("uLightDir"), 1, LIGHT_DIR, 0)
        GLES30.glUniform3fv(program.uniform("uShellColor"), 1, SHELL_COLOR, 0)
        GLES30.glUniform3fv(program.uniform("uRimColor"), 1, RIM_COLOR, 0)

        // Object space: the hole and the badge are cut into the shell itself and turn with it.
        GLES30.glUniform3fv(program.uniform("uWindowAxis"), 1, WINDOW_AXIS, 0)
        // The bevel's raised lip has to be lit, and light lives in world space.
        windowAxisWorld.writeTo(scratch)
        GLES30.glUniform3fv(program.uniform("uWindowAxisWorld"), 1, scratch, 0)
        GLES30.glUniform1f(program.uniform("uWindowCos"), WINDOW_COS)
        GLES30.glUniform1f(program.uniform("uBevelCos"), BEVEL_COS)
        // The interior pass fills the hole, so the shell really does cut one — until its program is
        // built, when a flat fill is all the window has to show.
        GLES30.glUniform1f(
            program.uniform("uInteriorEnabled"),
            if (interiorProgram != null) 1f else 0f
        )
        GLES30.glUniform3fv(program.uniform("uWindowFillColor"), 1, WINDOW_FILL_COLOR, 0)

        GLES30.glUniform3fv(program.uniform("uBadgeAxis"), 1, BADGE_AXIS, 0)
        GLES30.glUniform3fv(program.uniform("uBadgeTangent"), 1, BADGE_TANGENT, 0)
        GLES30.glUniform3fv(program.uniform("uBadgeBitangent"), 1, BADGE_BITANGENT, 0)
        GLES30.glUniform1f(program.uniform("uBadgeCos"), BADGE_COS)

        mesh.draw(program)
    }

    private fun drawInterior(snapshot: BallSnapshot, windowAxis: Vec3) {
        val program = interiorProgram ?: return
        val mesh = quad ?: return

        // The window turns with the shell, so where it lands on screen is a per-frame question — and
        // when it has turned away there is no hole to fill and the whole march can be skipped.
        windowScissor = computeWindowScissor(
            axis = windowAxis,
            offset = snapshot.shellOffset,
            width = viewportWidth,
            height = viewportHeight
        )
        if (windowScissor[2] <= 0 || windowScissor[3] <= 0) return

        snapshot.dieOrientation.writeMatrix(dieRotation)
        // Column-major 3x3 corner of the rotation, which is all the shader needs.
        for (column in 0..2) {
            for (row in 0..2) {
                dieRotation3[column * 3 + row] = dieRotation[column * 4 + row]
            }
        }

        program.use()
        // Only the window's hole still has far depth, so a near-far quad lands exactly there.
        GLES30.glDepthMask(false)
        GLES30.glEnable(GLES30.GL_SCISSOR_TEST)
        GLES30.glScissor(windowScissor[0], windowScissor[1], windowScissor[2], windowScissor[3])

        // The contents are simulated around the origin, so moving the ball moves the camera the
        // other way. A translation leaves the ray directions alone, which is why this is enough.
        GLES30.glUniform3f(
            program.uniform("uCameraPos"),
            -snapshot.shellOffset.x,
            -snapshot.shellOffset.y,
            cameraDistance - snapshot.shellOffset.z
        )
        GLES30.glUniform1f(program.uniform("uTanHalfFov"), tanHalfFov)
        GLES30.glUniform1f(program.uniform("uAspect"), aspect)

        windowAxis.writeTo(scratch)
        GLES30.glUniform3fv(program.uniform("uWindowAxis"), 1, scratch, 0)
        GLES30.glUniform1f(program.uniform("uWindowCos"), WINDOW_COS)
        GLES30.glUniform1f(program.uniform("uCavityRadius"), BallEngineTuning.CAVITY_RADIUS)

        snapshot.diePosition.writeTo(scratch)
        GLES30.glUniform3fv(program.uniform("uDieCenter"), 1, scratch, 0)
        GLES30.glUniformMatrix3fv(program.uniform("uDieRotation"), 1, false, dieRotation3, 0)
        GLES30.glUniform1f(program.uniform("uDiePlaneDistance"), DieGeometry.PLANE_DISTANCE)
        GLES30.glUniform3fv(program.uniform("uFaceNormals"), DieGeometry.FACE_COUNT, FACE_NORMALS, 0)
        // The count goes in as a uniform so the shader's plane loops stay loops; see the shader.
        GLES30.glUniform1i(program.uniform("uFaceCount"), DieGeometry.FACE_COUNT)
        GLES30.glUniform1i(program.uniform("uShadowProbes"), engine.tuning.shadowProbes)

        GLES30.glUniform3fv(program.uniform("uLightDir"), 1, LIGHT_DIR, 0)
        snapshot.up.writeTo(scratch)
        GLES30.glUniform3fv(program.uniform("uUp"), 1, scratch, 0)
        GLES30.glUniform1f(program.uniform("uFluidSurfaceOffset"), snapshot.fluidSurfaceOffset)
        GLES30.glUniform3fv(program.uniform("uLiquidColor"), 1, LIQUID_COLOR, 0)
        GLES30.glUniform3fv(program.uniform("uDieColor"), 1, DIE_COLOR, 0)

        // How stirred up the liquid is, straight from the simulation: one clouds the water while an
        // answer surfaces, the other decides how much fine bubbling shows under it.
        GLES30.glUniform1f(program.uniform("uTurbidity"), snapshot.turbidity.coerceIn(0f, 1f))
        GLES30.glUniform1f(
            program.uniform("uFizz"),
            (snapshot.agitation * BallEngineTuning.FIZZ_FROM_AGITATION).coerceIn(0f, 1f)
        )
        // How much of a film the churn has left on the glass above the waterline. Zero switches the
        // whole drip path off in the shader, which is what an untouched ball looks like.
        GLES30.glUniform1f(program.uniform("uWetness"), snapshot.wetness.coerceIn(0f, 1f))
        GLES30.glUniform1f(program.uniform("uTime"), elapsedSeconds)

        bindFluid(snapshot.fluid)
        bindAnswers(snapshot.diePosition)

        mesh.draw(program)

        GLES30.glDisable(GLES30.GL_SCISSOR_TEST)
        GLES30.glDepthMask(true)
    }

    /**
     * Hands the liquid to the interior program: the density grid on texture unit 0 and the bubbles as
     * plain uniforms.
     *
     * The grid is re-uploaded every frame because the engine refills the very same buffer during the
     * next step — see [com.byteflipper.random.domain.ball.physics.FluidFrame].
     */
    private fun bindFluid(fluid: FluidFrame?) {
        val program = interiorProgram ?: return
        val texture = densityTexture

        GLES30.glUniform1f(program.uniform("uIsoLevel"), BallEngineTuning.FLUID_ISO_LEVEL)
        GLES30.glUniform1f(program.uniform("uVoxel"), VOXEL_SIZE)
        GLES30.glUniform1i(program.uniform("uMaxSteps"), engine.tuning.marchSteps)
        GLES30.glUniform3fv(program.uniform("uAbsorption"), 1, ABSORPTION, 0)

        if (fluid == null || texture == null) {
            // Before the first step there is no liquid; the shader then sees an empty cavity.
            GLES30.glUniform1f(program.uniform("uHasDensity"), 0f)
            GLES30.glUniform1f(program.uniform("uFieldScale"), 1f)
            GLES30.glUniform1i(program.uniform("uBubbleCount"), 0)
            return
        }

        // Bind first, upload second: the atlas leaves its own unit active, and upload works on
        // whichever one is.
        texture.bind(DENSITY_TEXTURE_UNIT)
        texture.upload(fluid.density)
        GLES30.glUniform1i(program.uniform("uDensity"), DENSITY_TEXTURE_UNIT)
        GLES30.glUniform1f(program.uniform("uHasDensity"), 1f)
        GLES30.glUniform1f(program.uniform("uFieldScale"), fluid.fieldScale)

        val bubbleCount = fluid.bubbleCount.coerceIn(0, BallEngineTuning.MAX_BUBBLES)
        GLES30.glUniform1i(program.uniform("uBubbleCount"), bubbleCount)
        if (bubbleCount > 0) {
            GLES30.glUniform4fv(program.uniform("uBubbles"), bubbleCount, fluid.bubbles, 0)
        }
    }

    /**
     * Hands the answers to the interior program: the atlas on its own texture unit, the per-face basis
     * that places a cell on a face, and the mip level to read it at.
     *
     * The sheet is drawn here, on the GL thread, the first frame after the labels change. That costs
     * one frame — twenty text layouts and an upload — and in exchange the bitmap belongs to exactly
     * one thread.
     */
    private fun bindAnswers(diePosition: Vec3) {
        val program = interiorProgram ?: return
        val atlas = answerAtlas

        atlas?.setLabels(answerLabels)

        // Set even when there is nothing to sample: a sampler left at its default would point at the
        // unit the density grid lives on.
        GLES30.glUniform1i(program.uniform("uAnswerAtlas"), ANSWER_TEXTURE_UNIT)
        if (atlas == null || !atlas.isReady) {
            GLES30.glUniform1f(program.uniform("uHasAnswers"), 0f)
            return
        }

        atlas.bind()
        GLES30.glUniform1f(program.uniform("uHasAnswers"), 1f)
        GLES30.glUniform3fv(program.uniform("uFaceTangents"), DieGeometry.FACE_COUNT, FACE_TANGENTS, 0)
        GLES30.glUniform3fv(
            program.uniform("uFaceBitangents"),
            DieGeometry.FACE_COUNT,
            FACE_BITANGENTS,
            0
        )
        GLES30.glUniform2i(program.uniform("uAtlasCells"), atlas.cellsAcross, atlas.cellsDown)
        GLES30.glUniform1f(program.uniform("uTextHalfExtent"), TEXT_HALF_EXTENT)
        GLES30.glUniform3fv(program.uniform("uTextColor"), 1, TEXT_COLOR, 0)
        GLES30.glUniform1f(program.uniform("uTextLod"), atlas.lodFor(textScreenPixels(diePosition)))
    }

    /**
     * How many pixels wide the text square is at the die's current distance. Feeds the atlas's mip
     * choice, which the shader cannot make for itself inside the march.
     */
    private fun textScreenPixels(diePosition: Vec3): Float {
        if (viewportHeight <= 0) return 1f

        val dx = diePosition.x
        val dy = diePosition.y
        val dz = diePosition.z - cameraDistance
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        if (distance < 1e-3f) return viewportHeight.toFloat()

        val worldPerPixel = 2f * tanHalfFov * distance / viewportHeight
        return 2f * TEXT_HALF_EXTENT / worldPerPixel
    }

    /**
     * Pixel rectangle the window covers, given where [axis] points in view space and how far the ball
     * has drifted. The interior pass is a screen-filling quad, so without this it would run the
     * ray-march for every pixel and throw almost all of them away — the depth test only rejects
     * fragments where the shell actually drew.
     *
     * An empty rectangle means the window has turned away from the camera, and the caller skips the
     * pass entirely. Back-face culling already hides the hole from behind; this saves the work.
     */
    private fun computeWindowScissor(axis: Vec3, offset: Vec3, width: Int, height: Int): IntArray {
        if (width <= 0 || height <= 0) return EMPTY_SCISSOR

        val tangent = (Vec3.UP cross axis).normalized(Vec3(1f, 0f, 0f))
        val bitangent = (axis cross tangent).normalized(Vec3.UP)

        // The bevel rim is wider than the hole, so its projection safely contains it.
        val halfAngle = Math.toRadians(BallEngineTuning.WINDOW_BEVEL_HALF_ANGLE_DEGREES.toDouble()).toFloat()
        val ring = sin(halfAngle)
        val depth = cos(halfAngle)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var visible = false

        for (index in 0..SCISSOR_SAMPLES) {
            val vertex = if (index == SCISSOR_SAMPLES) {
                // The cap centre, in case the rim alone leaves it out on an extreme aspect ratio.
                axis
            } else {
                val angle = 2f * PI.toFloat() * index / SCISSOR_SAMPLES
                axis * depth + tangent * (cos(angle) * ring) + bitangent * (sin(angle) * ring)
            }

            // On a unit sphere the sample *is* its own normal, so this is the exact front-face test.
            // The margin keeps the rim in the rectangle while the window is crossing the silhouette.
            val toCamera = Vec3(-offset.x, -offset.y, cameraDistance - offset.z) - vertex
            val facing = (vertex dot toCamera.normalized(Vec3.FORWARD))
            if (facing > 0f) visible = true
            if (facing < SCISSOR_FACING_MARGIN) continue

            projectToScreen(vertex + offset, null) ?: continue
            val px = projectClip[0] * width
            val py = projectClip[1] * height
            minX = min(minX, px)
            minY = min(minY, py)
            maxX = max(maxX, px)
            maxY = max(maxY, py)
        }

        if (!visible || minX > maxX || minY > maxY) return EMPTY_SCISSOR

        val left = (minX - SCISSOR_MARGIN_PIXELS).toInt().coerceIn(0, width)
        val bottom = (minY - SCISSOR_MARGIN_PIXELS).toInt().coerceIn(0, height)
        val right = (maxX + SCISSOR_MARGIN_PIXELS).toInt().coerceIn(0, width)
        val top = (maxY + SCISSOR_MARGIN_PIXELS).toInt().coerceIn(0, height)
        return intArrayOf(left, bottom, max(right - left, 0), max(top - bottom, 0))
    }

    /**
     * Projects a world point to 0..1 screen coordinates, into [target] when one is given, and leaves
     * the same pair in [projectClip] for callers that want it without an array. Null when the point
     * is behind the camera.
     */
    private fun projectToScreen(point: Vec3, target: FloatArray?): FloatArray? {
        projectPoint[0] = point.x
        projectPoint[1] = point.y
        projectPoint[2] = point.z
        projectPoint[3] = 1f
        Matrix.multiplyMV(projectClip, 0, viewProjection, 0, projectPoint, 0)
        if (projectClip[3] <= 1e-4f) return null

        val x = projectClip[0] / projectClip[3] * 0.5f + 0.5f
        val y = projectClip[1] / projectClip[3] * 0.5f + 0.5f
        projectClip[0] = x
        projectClip[1] = y
        target?.let {
            it[0] = x
            it[1] = y
        }
        return projectClip
    }

    private companion object {
        const val FOV_DEGREES = 32f

        /** Rim samples used to bound the window on screen; the curve is smooth, so this is plenty. */
        const val SCISSOR_SAMPLES = 24
        const val SCISSOR_MARGIN_PIXELS = 3f

        /**
         * How far behind the silhouette a rim sample still counts. A little slack, so a window in the
         * middle of turning away keeps its whole visible sliver inside the rectangle.
         */
        const val SCISSOR_FACING_MARGIN = -0.2f

        /** What the scissor comes to when the window is facing away: nothing to draw. */
        val EMPTY_SCISSOR = intArrayOf(0, 0, 0, 0)

        /** Frame budget once the ball has settled: 30 fps is plenty for liquid this slow. */
        const val IDLE_FRAME_NANOS = 1_000_000_000L / 30L

        /** Below a millisecond a sleep costs more than the frame it would save. */
        const val MIN_SLEEP_NANOS = 1_500_000L

        val LIGHT_DIR = normalized(-0.42f, 0.78f, 0.66f)

        val SHELL_COLOR = floatArrayOf(0.045f, 0.045f, 0.052f)
        val RIM_COLOR = floatArrayOf(0.42f, 0.50f, 0.68f)
        val WINDOW_FILL_COLOR = floatArrayOf(0.03f, 0.04f, 0.06f)

        /** Deep blue, the way the classic ball's fluid reads through the glass. */
        val LIQUID_COLOR = floatArrayOf(0.06f, 0.16f, 0.42f)

        /** Moulded white plastic. */
        val DIE_COLOR = floatArrayOf(0.82f, 0.85f, 0.90f)

        /** The answers are printed in near-black ink, which survives the liquid's blue tint. */
        val TEXT_COLOR = floatArrayOf(0.05f, 0.07f, 0.13f)

        /**
         * Half the side of the text square on a face. A square of `inradius / sqrt(2)` is the largest
         * that fits inside a face's incircle, and the incircle is what fits inside the triangle. This
         * sits a little under that: the answer wants every pixel of the face it can get, and the last
         * sliver of the corners is where a triangular face crops a line of text anyway.
         */
        val TEXT_HALF_EXTENT = 0.74f * DieGeometry.FACE_INRADIUS

        /**
         * Beer-Lambert coefficients per metre of liquid. Red is swallowed roughly four times faster
         * than blue, which is what makes a deep die look drowned and a docked one merely tinted.
         */
        val ABSORPTION = floatArrayOf(3.4f, 2.2f, 0.9f)

        const val DENSITY_TEXTURE_UNIT = 0
        const val ANSWER_TEXTURE_UNIT = 1

        /** World size of one density voxel; the grid spans the cavity's full diameter. */
        val VOXEL_SIZE = 2f * BallEngineTuning.CAVITY_RADIUS /
            BallEngineTuning.DENSITY_RESOLUTION.toFloat()

        val WINDOW_AXIS = FloatArray(3).also { BallEngineTuning.WINDOW_AXIS.writeTo(it) }
        val WINDOW_COS = cosDegrees(BallEngineTuning.WINDOW_HALF_ANGLE_DEGREES)
        val BEVEL_COS = cosDegrees(BallEngineTuning.WINDOW_BEVEL_HALF_ANGLE_DEGREES)

        /**
         * The "8" is printed exactly opposite the window, both in the shell's own frame — so the two
         * are always back to back and one turn of the ball carries them round together.
         */
        val BADGE_AXIS_VECTOR = -BallEngineTuning.WINDOW_AXIS
        val BADGE_AXIS = FloatArray(3).also { BADGE_AXIS_VECTOR.writeTo(it) }

        /** In-plane axes of the badge, built off the same vector so the "8" stands upright on it. */
        val BADGE_TANGENT_VECTOR = (BADGE_AXIS_VECTOR cross Vec3.UP).normalized(Vec3(1f, 0f, 0f))
        val BADGE_TANGENT = FloatArray(3).also { BADGE_TANGENT_VECTOR.writeTo(it) }
        val BADGE_BITANGENT = FloatArray(3).also {
            (BADGE_TANGENT_VECTOR cross BADGE_AXIS_VECTOR).normalized(Vec3.UP).writeTo(it)
        }
        val BADGE_COS = cosDegrees(30f)

        val FACE_NORMALS = DieGeometry.flatten(DieGeometry.faceNormals)
        val FACE_TANGENTS = DieGeometry.flatten(DieGeometry.faceTangents)
        val FACE_BITANGENTS = DieGeometry.flatten(DieGeometry.faceBitangents)

        fun normalized(x: Float, y: Float, z: Float): FloatArray {
            val length = sqrt(x * x + y * y + z * z)
            if (length < 1e-6f) return floatArrayOf(0f, 1f, 0f)
            return floatArrayOf(x / length, y / length, z / length)
        }

        fun cosDegrees(degrees: Float): Float = cos(Math.toRadians(degrees.toDouble())).toFloat()
    }
}
